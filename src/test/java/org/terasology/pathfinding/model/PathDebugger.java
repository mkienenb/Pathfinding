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

import com.google.common.collect.Sets;
import org.terasology.math.Vector3i;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.util.Set;

/**
 * @author synopia
 */
public class PathDebugger extends JFrame {
    private Pathfinder pathfinder;
    private TestHelper helper;
    private final int mapWidth;
    private final int mapHeight;
    private int level;
    private WalkableBlock hovered;
    private WalkableBlock start;
    private WalkableBlock target;
    private Path path;

    public PathDebugger() throws HeadlessException {
        mapWidth = 160;
        mapHeight = 100;
        helper = new TestHelper();
//        helper.init(new MazeChunkGenerator(mapWidth, mapHeight, 4, 0, 20));
//        helper = new TestHelper(new PathfinderTestGenerator(true));
        pathfinder = new Pathfinder(helper.world);
        for (int x = 0; x < mapWidth / 16 + 1; x++) {
            for (int z = 0; z < mapHeight / 16 + 1; z++) {
                pathfinder.init(new Vector3i(x, 0, z));
            }
        }
        level = 6;
        add(new DebugPanel());
    }

    private boolean isEntrance(WalkableBlock block) {
        boolean isEntrance = false;
        for (Entrance entrance : block.floor.entrances()) {
            if (entrance.getAbstractBlock() == block) {
                isEntrance = true;
                break;
            }
        }
        return isEntrance;
    }

    public static void main(String[] args) {
        PathDebugger debugger = new PathDebugger();
        debugger.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        debugger.pack();
        debugger.setVisible(true);
    }

    private final class DebugPanel extends JPanel {
        private DebugPanel() {
            addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    int hoverX = e.getX() * mapWidth / getWidth();
                    int hoverZ = e.getY() * mapHeight / getHeight();
                    hovered = pathfinder.getBlock(new Vector3i(hoverX, level, hoverZ));
                    repaint();
                }

            });
            addMouseWheelListener(new MouseAdapter() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    level += e.getWheelRotation();
                    repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    int clickedX = e.getX() * mapWidth / getWidth();
                    int clickedZ = e.getY() * mapHeight / getHeight();
                    WalkableBlock block = pathfinder.getBlock(new Vector3i(clickedX, level, clickedZ));
                    if (start == null) {
                        start = block;
                    } else {
                        if (target == null) {
                            target = block;
                            path = pathfinder.findPath(target, start);
                        } else {
                            start = block;
                            target = null;
                        }
                    }
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Floor hoveredFloor = null;
            if (hovered != null) {
                hoveredFloor = hovered.floor;
            }
            for (int x = 0; x < mapWidth; x++) {
                for (int z = 0; z < mapHeight; z++) {
                    int screenX = x * getWidth() / mapWidth;
                    int screenY = z * getHeight() / mapHeight;
                    int tileWidth = (x + 1) * getWidth() / mapWidth - screenX;
                    int tileHeight = (z + 1) * getHeight() / mapHeight - screenY;
                    WalkableBlock block = pathfinder.getBlock(new Vector3i(x, level, z));

                    if (block != null) {
                        boolean isEntrance = isEntrance(block);

                        if (block.floor == hoveredFloor) {
                            if (isEntrance) {
                                g.setColor(Color.red);

                            } else {
                                g.setColor(Color.blue);
                            }
                        } else {
                            if (isEntrance) {
                                g.setColor(Color.lightGray);
                            } else {
                                g.setColor(Color.cyan);
                            }
                        }
                    } else {
                        g.setColor(Color.black);
                    }
                    g.fillRect(screenX, screenY, tileWidth, tileHeight);
                    if (block != null && (block == start || block == target)) {
                        g.setColor(Color.yellow);
                        g.fillOval(screenX, screenY, tileWidth, tileHeight);
                    }
                }
            }
            if (path != null) {
                for (WalkableBlock block : path) {
                    if (block.height() != level) {
                        continue;
                    }
                    int screenX = block.x() * getWidth() / mapWidth;
                    int screenY = block.z() * getHeight() / mapHeight;
                    int tileWidth = (block.x() + 1) * getWidth() / mapWidth - screenX;
                    int tileHeight = (block.z() + 1) * getHeight() / mapHeight - screenY;
                    g.setColor(Color.yellow);
                    g.fillOval(screenX, screenY, tileWidth, tileHeight);
                }
            }

            if (hovered != null) {
                boolean isEntrance = isEntrance(hovered);

                int screenX = hovered.x() * getWidth() / mapWidth;
                int screenY = hovered.z() * getHeight() / mapHeight;
                int tileWidth = (hovered.x() + 1) * getWidth() / mapWidth - screenX;
                int tileHeight = (hovered.z() + 1) * getHeight() / mapHeight - screenY;
                int x = screenX + tileWidth / 2;
                int y = screenY + tileHeight / 2;
                Set<Entrance> entrances;
                if (isEntrance) {
                    entrances = Sets.newHashSet();
                    for (Floor floor : hovered.floor.neighborRegions) {
                        entrances.addAll(floor.entrances());
                    }
                } else {
                    entrances = Sets.newHashSet(hovered.floor.entrances());
                }

                for (Entrance entrance : entrances) {
                    WalkableBlock block = entrance.getAbstractBlock();
                    screenX = block.x() * getWidth() / mapWidth;
                    screenY = block.z() * getHeight() / mapHeight;
                    tileWidth = (block.x() + 1) * getWidth() / mapWidth - screenX;
                    tileHeight = (block.z() + 1) * getHeight() / mapHeight - screenY;
                    int ex = screenX + tileWidth / 2;
                    int ey = screenY + tileHeight / 2;
                    if (block.height() == level) {
                        g.setColor(Color.BLACK);
                    } else {
                        g.setColor(Color.LIGHT_GRAY);
                    }
                    g.drawLine(x, y, ex, ey);
                }
            }
        }
    }
}
