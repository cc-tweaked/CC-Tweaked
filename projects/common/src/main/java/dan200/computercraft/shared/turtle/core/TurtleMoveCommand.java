// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TurtleMoveCommand implements TurtleCommand {
    private final MoveDirection direction;

    public TurtleMoveCommand(MoveDirection direction) {
        this.direction = direction;
    }

    @Override
    public TurtleCommandResult execute(ITurtleAccess turtle) {
        // Get world direction from direction
        var direction = this.direction.toWorldDir(turtle);

        // Check if we can move
        var oldWorld = (ServerLevel) turtle.getLevel();
        var oldPosition = turtle.getPosition();
        var newPosition = oldPosition.relative(direction);

        var turtlePlayer = TurtlePlayer.getWithPosition(turtle, oldPosition, direction);
        var canEnterResult = canEnter(turtlePlayer, oldWorld, newPosition);
        if (!canEnterResult.isSuccess()) {
            return canEnterResult;
        }

        // Check existing block is air or replaceable
        var state = oldWorld.getBlockState(newPosition);
        if (!oldWorld.isEmptyBlock(newPosition) &&
            !WorldUtil.isLiquidBlock(oldWorld, newPosition) &&
            !state.canBeReplaced()) {
            return TurtleCommandResult.failure("Movement obstructed");
        }

        // Check there isn't anything in the way
        var collision = state.getCollisionShape(oldWorld, oldPosition).move(
            newPosition.getX(),
            newPosition.getY(),
            newPosition.getZ()
        );

        if (!oldWorld.isUnobstructed(null, collision)) {
            if (!Config.turtlesCanPush || this.direction == MoveDirection.UP || this.direction == MoveDirection.DOWN) {
                return TurtleCommandResult.failure("Movement obstructed");
            }

            // Check there is space for all the pushable entities to be pushed
            var list = oldWorld.getEntitiesOfClass(Entity.class, getBox(collision), x -> x != null && x.isAlive() && x.blocksBuilding);
            for (var entity : list) {
                var pushedBB = entity.getBoundingBox().move(
                    direction.getStepX(),
                    direction.getStepY(),
                    direction.getStepZ()
                );
                if (!oldWorld.isUnobstructed(null, Shapes.create(pushedBB))) {
                    return TurtleCommandResult.failure("Movement obstructed");
                }
            }
        }

        // Check fuel level
        if (turtle.isFuelNeeded() && turtle.getFuelLevel() < 1) {
            return TurtleCommandResult.failure("Out of fuel");
        }

        // Move
        if (!turtle.teleportTo(oldWorld, newPosition)) return TurtleCommandResult.failure("Movement failed");

        // Consume fuel
        turtle.consumeFuel(1);

        // Animate
        switch (this.direction) {
            case FORWARD -> turtle.playAnimation(TurtleAnimation.MOVE_FORWARD);
            case BACK -> turtle.playAnimation(TurtleAnimation.MOVE_BACK);
            case UP -> turtle.playAnimation(TurtleAnimation.MOVE_UP);
            case DOWN -> turtle.playAnimation(TurtleAnimation.MOVE_DOWN);
        }
        return TurtleCommandResult.success();
    }

    private static TurtleCommandResult canEnter(TurtlePlayer turtlePlayer, ServerLevel world, BlockPos position) {
        if (world.isOutsideBuildHeight(position)) {
            return TurtleCommandResult.failure(position.getY() < 0 ? "Too low to move" : "Too high to move");
        }
        if (!world.isInWorldBounds(position)) return TurtleCommandResult.failure("Cannot leave the world");

        // Check spawn protection
        if (turtlePlayer.isBlockProtected(world, position)) {
            return TurtleCommandResult.failure("Cannot enter protected area");
        }

        if (!world.isLoaded(position)) return TurtleCommandResult.failure("Cannot leave loaded world");
        if (!world.getWorldBorder().isWithinBounds(position)) {
            return TurtleCommandResult.failure("Cannot pass the world border");
        }

        return TurtleCommandResult.success();
    }

    private static AABB getBox(VoxelShape shape) {
        return shape.isEmpty() ? EMPTY_BOX : shape.bounds();
    }

    private static final AABB EMPTY_BOX = new AABB(0, 0, 0, 0, 0, 0);
}
