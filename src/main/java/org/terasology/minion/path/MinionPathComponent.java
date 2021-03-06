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
package org.terasology.minion.path;

import org.terasology.entitySystem.Component;
import org.terasology.math.Vector3i;
import org.terasology.pathfinding.model.Path;

/**
 * @author synopia
 */
public class MinionPathComponent implements Component {
    /**
     * if set to a value other then null, this minion is requested to move to this position
     * once reached, targetBlock is set to null
     */
    public Vector3i targetBlock;

    public enum PathState {
        IDLE,
        NEW_TARGET,
        PATH_REQUESTED,
        PATH_RECEIVED,
        MOVING_PATH,
        FINISHED_MOVING
    }

    public transient Path path;
    public transient int pathId = -1;
    public transient int pathStep;
    public transient PathState pathState = PathState.IDLE;
}
