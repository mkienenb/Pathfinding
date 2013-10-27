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

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.terasology.math.Vector3i;
import org.terasology.pathfinding.MazeChunkGenerator;
import org.terasology.world.WorldProvider;

/**
 * @author synopia
 */
public class MazeTest {
    private WorldProvider world;
    private Pathfinder pathfinder;
    private TestHelper helper;

    @Ignore
    @Test
    public void test() {
        WalkableBlock start = pathfinder.getBlock(new Vector3i(55, 6, 1));
        Assert.assertNotNull(start);
        WalkableBlock target = pathfinder.getBlock(new Vector3i(1, 3 * 3, 1));
        Assert.assertNotNull(target);

        StringBuilder sb = new StringBuilder();
        for (int l = 6; l < 7; l++) {
            for (int j = 0; j < 100; j++) {
                for (int i = 0; i <= 160; i++) {
                    WalkableBlock block = pathfinder.getBlock(new Vector3i(i, l, j));
                    if (block != null) {
                        sb.append(block.floor.isEntrance(block) ? 'C' : ' ');
                    } else {
                        sb.append(world.getBlock(i, l, j).isPenetrable() ? ' ' : 'X');
                    }
                }
                sb.append("\n");
            }
        }
        System.out.println(sb);
        Assert.assertTrue(pathfinder.findPath(target, start)[0].size() > 0);
    }


    @Before
    public void setup() {
        int width = 160;
        int height = 100;
        helper = new TestHelper();
        helper.init(new MazeChunkGenerator(width, height, 4, 0, 20));
        world = helper.world;
        pathfinder = new Pathfinder(world);
        for (int x = 0; x < width / 16 + 1; x++) {
            for (int z = 0; z < height / 16 + 1; z++) {
                pathfinder.init(new Vector3i(x, 0, z));
            }
        }
    }

}