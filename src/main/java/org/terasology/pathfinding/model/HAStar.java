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
package org.terasology.pathfinding.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.math.Vector3i;

import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author synopia
 */
public class HAStar {
    private static final Logger logger = LoggerFactory.getLogger(HAStar.class);
    private static final int MAX_NODES = 64 * 1024;
    private Path localPath;
    private HAStar localAStar;

    private List<Node> nodes = Lists.newArrayList();

    private Map<WalkableBlock, Integer> nodeMap = Maps.newHashMap();
    private int start;
    private int end;
    private int cacheHits;
    private int localPathsUsed;
    private BinaryHeap openList;

    private BitSet closedList = new BitSet(16 * 1024);
    private boolean useContour;

    public HAStar() {
        this(true);
    }

    public HAStar(boolean useContour) {
        this.useContour = useContour;
        openList = new BinaryHeap(new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                float fA = nodes.get(a).f;
                float fB = nodes.get(b).f;
                return -(fA < fB ? -1 : (fA > fB ? 1 : 0));
            }
        }, MAX_NODES, MAX_NODES);
        if (useContour) {
            localAStar = new HAStar(false);
        }
    }

    public void reset() {
        nodes.clear();
        nodeMap.clear();
        closedList.clear();
        openList.clear();
        cacheHits = 0;
    }

    private int create(WalkableBlock block) {
        Integer id = nodeMap.get(block);
        if (id != null) {
            return id;
        }
        Node node = new Node();
        node.block = block;
        node.id = nodes.size();
        nodes.add(node);
        nodeMap.put(block, node.id);
        return node.id;
    }

    public boolean run(WalkableBlock startBlock, WalkableBlock endBlock) {
        this.start = create(startBlock);
        this.end = create(endBlock);

        openList.insert(this.start);

        int maxSize = 0;
        int current = -1;
        while (!openList.isEmpty()) {
            current = openList.removeMin();
            if (current == this.end) {
                break;
            }
            if (nodes.size() > MAX_NODES - MAX_NODES / 10) {
                logger.info("stop hpa*... nodes: " + nodes.size());
                break;
            }
            expand(current);
            closedList.set(current);

            if (openList.getSize() > maxSize) {
                maxSize = openList.getSize();
            }
        }
        return current == this.end;
    }

    public Path getPath() {
        localPathsUsed = 0;
        Path path = new Path();
        Node startNode = nodes.get(this.start);
        Node current = nodes.get(end);
        while (current != startNode && current != null) {
            if (current.path != null) {
                path.addAll(current.path);
                localPathsUsed++;
            } else {
                path.add(current.block);
            }
            current = current.p;
        }
        return path;
    }

    protected void expand(int current) {
        Node currentNode = nodes.get(current);
        Floor currentFloor = currentNode.block.floor;
        Set<WalkableBlock> neighbors = new HashSet<WalkableBlock>();
        boolean onEndHeightMap = nodes.get(end).block.floor.heightMap == currentFloor.heightMap;
        boolean onStartHeightMap = nodes.get(start).block.floor.heightMap == currentFloor.heightMap;
        if (!useContour || onEndHeightMap || onStartHeightMap) {
            // normal A* if on start or end height map
            for (WalkableBlock neighbor : currentNode.block.neighbors) {
                if (neighbor == null) {
                    continue;
                }
                neighbors.add(neighbor);
            }
        } else {
            // otherwise use entrances of other floor
            for (Floor neighborFloor : currentFloor.neighborRegions) {
                for (Entrance neighborEntrance : neighborFloor.entrances()) {
                    neighbors.add(neighborEntrance.getAbstractBlock());
                }
            }
        }

        for (WalkableBlock neighbor : neighbors) {
            expandNeighbor(current, currentNode, neighbor);
        }
    }

    private void expandNeighbor(int current, Node currentNode, WalkableBlock neighbor) {
        int successor = create(neighbor);
        if (closedList.get(successor)) {
            return;
        }
        float tentativeG = currentNode.g + c(current, successor);
        Node successorNode = nodes.get(successor);
        if (openList.contains(successor) && tentativeG >= successorNode.g) {
            return;
        }
        successorNode.path = localPath;
        successorNode.p = currentNode;
        successorNode.g = tentativeG;
        successorNode.f = tentativeG + h(successor);

        if (openList.contains(successor)) {
            openList.update(successor);
        } else {
            openList.insert(successor);
        }
    }

    protected float c(int from, int to) {
        localPath = null;
        Node fromNode = nodes.get(from);
        Node toNode = nodes.get(to);
        Vector3i fromPos = fromNode.block.getBlockPosition();
        Vector3i toPos = toNode.block.getBlockPosition();
        int diffX = Math.abs(fromPos.x - toPos.x);
        int diffZ = Math.abs(fromPos.z - toPos.z);
        if (toNode.block.hasNeighbor(fromNode.block)) {
            if (diffX + diffZ == 1) {
                return 1;
            } else {
                return BitMap.SQRT_2;
            }
        }
        if (fromNode.block.floor.heightMap.pathCache.hasPath(fromNode.block, toNode.block)) {
            cacheHits++;
        }
        localPath = fromNode.block.floor.heightMap.pathCache.findPath(
                fromNode.block, toNode.block, new PathCache.Callback() {
            @Override
            public Path run(WalkableBlock from, WalkableBlock to) {

                localAStar.reset();
                if (localAStar.run(from, to)) {
                    return localAStar.getPath();
                }
                return Path.INVALID;
            }
        });
        if (localPath == null || localPath == Path.INVALID) {
            throw new IllegalStateException(fromNode + ", " + toNode + " no costs found!");
        }

        return localPath.size();
    }

    protected float h(int current) {
        Node fromNode = nodes.get(current);
        Node toNode = nodes.get(end);
        Vector3i fromPos = fromNode.block.getBlockPosition();
        Vector3i toPos = toNode.block.getBlockPosition();
        return (float) Math.abs(fromPos.x - toPos.x) + Math.abs(fromPos.y - toPos.y) + Math.abs(fromPos.z - toPos.z);
    }

    @Override
    public String toString() {
        return "closed list size=" + closedList.cardinality() + ", cache hits=" + cacheHits + ", local paths used=" + localPathsUsed;
    }

    private class Node {
        int id;
        float g;
        float f;
        Node p;
        WalkableBlock block;
        Path path;
    }

}
