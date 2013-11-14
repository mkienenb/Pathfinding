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
package org.terasology.minion.move;

import org.terasology.behavior.tree.Behavior;
import org.terasology.behavior.tree.BehaviorTree;
import org.terasology.behavior.tree.Node;
import org.terasology.behavior.tree.Status;
import org.terasology.engine.CoreRegistry;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.minion.path.MinionPathComponent;
import org.terasology.pathfinding.componentSystem.PathfinderSystem;
import org.terasology.pathfinding.model.WalkableBlock;

import javax.vecmath.Vector3f;

/**
 * @author synopia
 */
public class SetTargetLocalPlayer extends Behavior<EntityRef> {
    public SetTargetLocalPlayer(Node<EntityRef> node) {
        super(node);
    }

    @Override
    public Status update(EntityRef minion, float dt) {
        LocalPlayer localPlayer = CoreRegistry.get(LocalPlayer.class);
        Vector3f position = localPlayer.getPosition();
        WalkableBlock block = CoreRegistry.get(PathfinderSystem.class).getBlock(position);
        if (block != null) {
            MinionPathComponent pathComponent = minion.getComponent(MinionPathComponent.class);
            if (pathComponent == null) {
                pathComponent = new MinionPathComponent();
                minion.addComponent(pathComponent);
            }
            pathComponent.targetBlock = block.getBlockPosition();
            pathComponent.pathState = MinionPathComponent.PathState.NEW_TARGET;
            minion.saveComponent(pathComponent);
        }
        return Status.SUCCESS;
    }

    @Override
    public SetTargetLocalPlayerNode getNode() {
        return (SetTargetLocalPlayerNode) super.getNode();
    }

    public static class SetTargetLocalPlayerNode implements Node<EntityRef> {
        @Override
        public SetTargetLocalPlayer create(BehaviorTree<EntityRef> tree) {
            return new SetTargetLocalPlayer(this);
        }
    }
}
