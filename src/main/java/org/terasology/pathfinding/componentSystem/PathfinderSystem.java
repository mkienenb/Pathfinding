/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.pathfinding.componentSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.CoreRegistry;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.In;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.pathfinding.model.HeightMap;
import org.terasology.pathfinding.model.Path;
import org.terasology.pathfinding.model.Pathfinder;
import org.terasology.pathfinding.model.WalkableBlock;
import org.terasology.world.WorldChangeListener;
import org.terasology.world.WorldComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.chunks.event.OnChunkLoaded;

import javax.vecmath.Vector3f;
import java.util.*;
import java.util.concurrent.*;

/**
 * This systems helps finding a path through the game world.
 * <p/>
 * Since path finding takes some time, it completely runs in a background thread. So, a requested path is not
 * available in the moment it is requested. Instead you need to listen for a PathReadyEvent.
 *
 * @author synopia
 */
@RegisterSystem
public class PathfinderSystem implements ComponentSystem, WorldChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(PathfinderSystem.class);

    /**
     * Task to update a chunk
     */
    private class UpdateChunkTask {
        public Vector3i chunkPos;

        private UpdateChunkTask(Vector3i chunkPos) {
            this.chunkPos = chunkPos;
        }

        public void process() {
            maps.remove(chunkPos);
            HeightMap map = pathfinder.update(chunkPos);
            maps.put(chunkPos, map);
            pathfinder.clearCache();
        }
    }

    /**
     * Task to find a path.
     */
    private class FindPathTask {
        public EntityRef entity;
        public Path path;
        public Vector3i start;
        public Vector3i[] target;
        public boolean processed;
        public int pathId;

        private FindPathTask(Vector3i start, Vector3i[] target, EntityRef entity) {
            this.start = start;
            this.target = target;
            this.entity = entity;
            this.pathId = nextId;
            nextId++;
        }

        /**
         * Does the actual path finding. When its done, the outputQueue is filled with the result.
         * This method should be called from a thread only, it may take long.
         */
        public void process() {
            WalkableBlock startBlock = pathfinder.getBlock(this.start);
            WalkableBlock targetBlock = pathfinder.getBlock(this.target[0]);
            path = null;
            if (start != null && target != null) {
                path = pathfinder.findPath(targetBlock, startBlock);
            }
            processed = true;
            entity.send(new PathReadyEvent(pathId, startBlock, targetBlock, path));
        }
    }

    @In
    private WorldProvider world;

    private BlockingQueue<UpdateChunkTask> updateChunkQueue = new ArrayBlockingQueue<>(100);
    private BlockingQueue<FindPathTask> findPathTasks = new ArrayBlockingQueue<>(100);
    private Set<Vector3i> invalidChunks = Collections.synchronizedSet(new HashSet<Vector3i>());

    private ExecutorService inputThreads;

    private Map<Vector3i, HeightMap> maps = new HashMap<>();
    private Pathfinder pathfinder;
    private int nextId;

    public PathfinderSystem() {
        CoreRegistry.put(PathfinderSystem.class, this);
    }

    public int requestPath(EntityRef requestor, Vector3f start, Vector3f... targets) {
        Vector3i[] _targets = new Vector3i[targets.length];
        WalkableBlock block;
        for (int i = 0; i < targets.length; i++) {
            block = getBlock(targets[i]);
            if( block!=null ) {
                _targets[i] = block.getBlockPosition();
            } else {
                throw new IllegalArgumentException(targets[i]+" is no valid walkable block");
            }
        }
        block = getBlock(start);
        if( block!=null ) {
            return requestPath(requestor, block.getBlockPosition(), _targets);
        } else {
            throw new IllegalArgumentException(start+" is no valid walkable block");
        }
    }
    public int requestPath(EntityRef requestor, Vector3i start, Vector3i... target) {
        FindPathTask task = new FindPathTask(start, target, requestor);
        findPathTasks.add(task);
        return task.pathId;
    }

    public HeightMap getHeightMap(Vector3i chunkPos) {
        return maps.get(chunkPos);
    }

    public WalkableBlock getBlock(Vector3i pos) {
        return pathfinder.getBlock(pos);
    }

    public WalkableBlock getBlock(Vector3f pos) {
        Vector3i blockPos = new Vector3i(pos);
        WalkableBlock block = pathfinder.getBlock(blockPos);
        if( block == null ) {
            blockPos.y+=2;
            while (blockPos.y>0 && (block=pathfinder.getBlock(blockPos))==null ) {
                blockPos.y--;
            }
        }
        return block;
    }

    @Override
    public void initialise() {
        world.registerListener(this);
        pathfinder = new Pathfinder(world);
        logger.info("Pathfinder started");

        inputThreads = Executors.newFixedThreadPool(1);
        inputThreads.execute(new Runnable() {
            @Override
            public void run() {

                boolean running = true;
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                while (running) {
                    try {
                        UpdateChunkTask task = updateChunkQueue.poll(1, TimeUnit.SECONDS);
                        if (task != null) {
                            task.process();
                        } else {
                            findPaths();
                        }
                    } catch (InterruptedException e) {
                        logger.error("Thread interrupted", e);
                    } catch (Exception e) {
                        logger.error("Error in thread", e);
                    }
                }
                logger.debug("Thread shutdown safely");
            }
        });
    }

    private void findPaths() {
        int count = 0;
        long time = System.nanoTime();
        int notFound = 0;
        int invalid = 0;
        int processed = 0;
        while (!findPathTasks.isEmpty()) {
            FindPathTask pathTask = findPathTasks.poll();
            processed++;
            if (pathTask.processed) {
                continue;
            }
            count++;
            pathTask.process();
            if (pathTask.path == null) {
                invalid++;
            }
            if (pathTask.path == Path.INVALID) {
                notFound++;
            }
        }
        float ms = (System.nanoTime() - time) / 1000 / 1000f;
        if (count > 0) {
            logger.info("Searching " + count + " pathes took " + ms + " ms ("
                + (1000f / ms * count) + " pps), processed=" + processed + ", invalid=" + invalid + ", not found=" + notFound);
        }
    }

    @Override
    public void shutdown() {
        inputThreads.shutdown();
    }

    @Override
    public void onBlockChanged(Vector3i pos, Block newBlock, Block originalBlock) {
        Vector3i chunkPos = TeraMath.calcChunkPos(pos);
        invalidChunks.add(chunkPos);
        updateChunkQueue.offer(new UpdateChunkTask(chunkPos));
    }

    @ReceiveEvent(components = WorldComponent.class)
    public void chunkReady(OnChunkLoaded event, EntityRef worldEntity) {
        invalidChunks.add(event.getChunkPos());
        updateChunkQueue.offer(new UpdateChunkTask(event.getChunkPos()));
    }
}
