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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author synopia
 */
public class WalkableBlockFinderTest {
    @Test
    public void testNeighbors4() {
        TestHelper helper = new TestHelper();
        helper.init();

        helper.setGround(
                "XXX|   |   |",
                "XXX|X  |X  |",
                "XXX|   |   |"
        );
        helper.map.update();
        WalkableBlock sut = helper.map.getBlock(1, 0, 1);
        WalkableBlock lu = helper.map.getBlock(0, 0, 0);
        WalkableBlock ld = helper.map.getBlock(0, 0, 2);

        Assert.assertFalse(sut.hasNeighbor(lu));
        Assert.assertFalse(sut.hasNeighbor(ld));
    }

    @Test
    public void testNeighbors3() {
        TestHelper helper = new TestHelper();
        helper.init();

        helper.setGround(
                "XXXXXX|      |      |      |XXXXXX|",
                "XX    |  X   |   X  |    X |  X  X|",
                "XX    |  X   |   X  |    X |  X  X|",
                "XXXXXX|      |      |      |XXXXXX|"
        );
        helper.map.update();
        WalkableBlock left = helper.map.getBlock(2, 1, 1);
        WalkableBlock right = helper.map.getBlock(3, 2, 1);

        Assert.assertFalse(left.hasNeighbor(right));
        Assert.assertFalse(right.hasNeighbor(left));
    }

    @Test
    public void testNeighbors2() {
        TestHelper helper = new TestHelper();
        helper.init();

        helper.setGround(
                " X ",
                "X X",
                " X "
//                "XXXXXX|      |      |XXXXXX|",
//                "XX    |  X   |   X  |    XX|",
//                "XX    |  X   |   X  |    XX|",
//                "XXXXXX|      |      |XXXXXX|"
        );
        helper.map.update();
        WalkableBlock left = helper.map.getBlock(0, 0, 1);
        WalkableBlock up = helper.map.getBlock(1, 0, 0);
        WalkableBlock right = helper.map.getBlock(2, 0, 1);
        WalkableBlock down = helper.map.getBlock(1, 0, 2);

        Assert.assertTrue(left.hasNeighbor(up));
        Assert.assertTrue(left.hasNeighbor(down));
        Assert.assertFalse(left.hasNeighbor(right));

        Assert.assertTrue(up.hasNeighbor(left));
        Assert.assertTrue(up.hasNeighbor(right));
        Assert.assertFalse(up.hasNeighbor(down));

        Assert.assertTrue(right.hasNeighbor(up));
        Assert.assertTrue(right.hasNeighbor(down));
        Assert.assertFalse(right.hasNeighbor(left));

        Assert.assertTrue(down.hasNeighbor(left));
        Assert.assertTrue(down.hasNeighbor(right));
        Assert.assertFalse(down.hasNeighbor(up));
    }

    @Test
    public void testFind() {
        assertWalkableBlocks(new String[]{
                "XXX",
                "XXX",
                "XXX"
        }, new String[]{
                "XXX",
                "XXX",
                "XXX"
        });
        assertWalkableBlocks(new String[]{
                "XXX|   |   |XXX",
                "XXX|   |   |XXX",
                "XXX|   |   |XXX"
        }, new String[]{
                "XXX|   |   |XXX",
                "XXX|   |   |XXX",
                "XXX|   |   |XXX"
        });
        assertWalkableBlocks(new String[]{
                "XXX|   |XXX",
                "XXX|   |XXX",
                "XXX|   |XXX"
        }, new String[]{
                "   |   |XXX",
                "   |   |XXX",
                "   |   |XXX"
        });
        assertWalkableBlocks(new String[]{
                "XXX|   |XXX",
                "XXX|   |X X",
                "XXX|   |XXX"
        }, new String[]{
                "   |   |XXX",
                " X |   |X X",
                "   |   |XXX"
        });
    }

    @Test
    public void testNeighbors() {
        assertNeighbors3x3(
                "XXX",
                "XXX",
                "XXX"
        );
        assertNeighbors3x3(
                "XXX|   ",
                "XXX| X ",
                "XXX|   "
        );
        assertNeighbors3x3(
                "XXX|   ",
                "X X| X ",
                "XXX|   "
        );
        assertNeighbors3x3(
                " X |X X",
                "X X| X ",
                " X |X X"
        );
    }

    private void assertNeighbors3x3(String... data) {
        final TestHelper helper = new TestHelper();
        helper.init();

        helper.setGround(data);
        WalkableBlockFinder finder = new WalkableBlockFinder(helper.world);
        finder.findWalkableBlocks(helper.map);

        WalkableBlock lu = helper.map.getCell(0, 0).blocks.get(0);
        WalkableBlock up = helper.map.getCell(1, 0).blocks.get(0);
        WalkableBlock ru = helper.map.getCell(2, 0).blocks.get(0);
        WalkableBlock left = helper.map.getCell(0, 1).blocks.get(0);
        WalkableBlock center = helper.map.getCell(1, 1).blocks.get(0);
        WalkableBlock right = helper.map.getCell(2, 1).blocks.get(0);
        WalkableBlock ld = helper.map.getCell(0, 2).blocks.get(0);
        WalkableBlock down = helper.map.getCell(1, 2).blocks.get(0);
        WalkableBlock rd = helper.map.getCell(2, 2).blocks.get(0);

        assertNeighbors(lu, null, null, up, left);
        assertNeighbors(up, lu, null, ru, center);
        assertNeighbors(ru, up, null, null, right);
        assertNeighbors(left, null, lu, center, ld);
        assertNeighbors(center, left, up, right, down);
        assertNeighbors(right, center, ru, null, rd);
        assertNeighbors(ld, null, left, down, null);
        assertNeighbors(down, ld, center, rd, null);
        assertNeighbors(rd, down, right, null, null);
    }

    private void assertNeighbors(WalkableBlock block, WalkableBlock left, WalkableBlock up, WalkableBlock right, WalkableBlock down) {
        Assert.assertSame(left, block.neighbors[HeightMap.DIR_LEFT]);
        Assert.assertSame(up, block.neighbors[HeightMap.DIR_UP]);
        Assert.assertSame(right, block.neighbors[HeightMap.DIR_RIGHT]);
        Assert.assertSame(down, block.neighbors[HeightMap.DIR_DOWN]);
    }

    private void assertWalkableBlocks(String[] data, String[] walkable) {
        final TestHelper helper = new TestHelper();
        helper.init();

        helper.setGround(data);
        WalkableBlockFinder finder = new WalkableBlockFinder(helper.world);
        finder.findWalkableBlocks(helper.map);

        String[] evaluate = helper.evaluate(new TestHelper.Runner() {
            @Override
            public char run(int x, int y, int z, char value) {
                return helper.map.getBlock(x, y, z) == null ? ' ' : 'X';
            }
        });

        Assert.assertArrayEquals(walkable, evaluate);

    }
}
