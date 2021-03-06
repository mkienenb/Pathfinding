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
package org.terasology.jobSystem.jobs;

import com.google.common.collect.Lists;
import org.terasology.engine.CoreRegistry;
import org.terasology.engine.SimpleUri;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.In;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.jobSystem.Job;
import org.terasology.jobSystem.JobFactory;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Vector3i;
import org.terasology.pathfinding.componentSystem.PathfinderSystem;
import org.terasology.pathfinding.model.WalkableBlock;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import javax.vecmath.Vector3f;
import java.util.List;

/**
 * @author synopia
 */
@RegisterSystem
public class BuildBlock implements Job, ComponentSystem {
    private static final int[][] NEIGHBORS = new int[][]{
            {-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1},
//            {-1, 1, 0}, {1, 1, 0}, {0, 1, -1}, {0, 1, 1},
            {-1, -1, 0}, {1, -1, 0}, {0, -1, -1}, {0, -1, 1},
            {-1, -2, 0}, {1, -2, 0}, {0, -2, -1}, {0, -2, 1},
    };
    private static final int[][] DIRECT_NEIGHBORS = new int[][]{
            {-1, 0, 0}, {1, 0, 0}, {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}
    };

    @In
    private PathfinderSystem pathfinderSystem;
    @In
    private WorldProvider worldProvider;

    private Block blockType;
    private final SimpleUri uri;

    public BuildBlock() {
        uri = new SimpleUri("Pathfinding:buildBlock");
    }

    @Override
    public void initialise() {
        CoreRegistry.get(JobFactory.class).register(this);
        blockType = CoreRegistry.get(BlockManager.class).getBlock("core:Dirt");
    }

    @Override
    public void shutdown() {
    }

    @Override
    public SimpleUri getUri() {
        return uri;
    }

    @Override
    public List<WalkableBlock> getTargetPositions(EntityRef block) {
        List<WalkableBlock> result = Lists.newArrayList();

        Vector3i worldPos = block.getComponent(BlockComponent.class).getPosition();
        WalkableBlock walkableBlock;
        Vector3i pos = new Vector3i();
        for (int[] neighbor : NEIGHBORS) {
            pos.set(worldPos.x + neighbor[0], worldPos.y + neighbor[1], worldPos.z + neighbor[2]);
            walkableBlock = pathfinderSystem.getBlock(pos);
            if (walkableBlock != null) {
                result.add(walkableBlock);
            }
        }
        return result;
    }

    @Override
    public boolean canMinionWork(EntityRef block, EntityRef minion) {
        Vector3f pos = new Vector3f();
        pos.sub(block.getComponent(LocationComponent.class).getWorldPosition(), minion.getComponent(LocationComponent.class).getWorldPosition());
        pos.y /= 4;
        float length = pos.length();
        return length < 2;
    }

    @Override
    public void letMinionWork(EntityRef block, EntityRef minion) {
        worldProvider.setBlock(block.getComponent(BlockComponent.class).getPosition(), blockType);
    }

    @Override
    public boolean isAssignable(EntityRef block) {
        Vector3i position = new Vector3i(block.getComponent(BlockComponent.class).getPosition());
        Block type = worldProvider.getBlock(position);
        return type.isPenetrable();
    }

    @Override
    public boolean isRequestable(EntityRef block) {
        Vector3i position = new Vector3i(block.getComponent(BlockComponent.class).getPosition());
        Vector3i pos = new Vector3i();
        for (int[] neighbor : DIRECT_NEIGHBORS) {
            pos.set(position.x + neighbor[0], position.y + neighbor[1], position.z + neighbor[2]);
            Block solid = worldProvider.getBlock(pos);
            if (!solid.isPenetrable()) {
                return true;
            }
        }
        return false;

    }
}
