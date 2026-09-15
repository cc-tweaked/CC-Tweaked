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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;

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
        var level = (ServerLevel) turtle.getLevel();
        var oldPosition = turtle.getPosition();
        var newPosition = oldPosition.relative(direction);

        var turtlePlayer = TurtlePlayer.getWithPosition(turtle, oldPosition, direction);
        var canEnterResult = canEnter(turtlePlayer, level, newPosition);
        if (!canEnterResult.isSuccess()) return canEnterResult;

        // Check existing block is air or replaceable.
        var existingState = level.getBlockState(newPosition);
        if (!(WorldUtil.isEmptyBlock(existingState) || existingState.canBeReplaced())) {
            return TurtleCommandResult.failure("Movement obstructed");
        }

        // Check there isn't an entity in the way.
        var turtleShape = level.getBlockState(oldPosition).getCollisionShape(level, oldPosition)
            .move(newPosition.getX(), newPosition.getY(), newPosition.getZ());
        if (!level.isUnobstructed(null, turtleShape) && !canPushEntities(level, turtleShape.bounds())) {
            return TurtleCommandResult.failure("Movement obstructed");
        }

        // Check fuel level
        if (turtle.isFuelNeeded() && turtle.getFuelLevel() < 1) return TurtleCommandResult.failure("Out of fuel");

        // Move
        if (!turtle.teleportTo(level, newPosition)) return TurtleCommandResult.failure("Movement failed");

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


    /**
     * Determine if all entities in the given bounds can be pushed by the turtle.
     *
     * @param level  The current level.
     * @param bounds The bounding box.
     * @return Whether all entities can be pushed.
     */
    private boolean canPushEntities(Level level, AABB bounds) {
        if (!Config.turtlesCanPush) return false;

        // Check there is space for all the pushable entities to be pushed
        return level.getEntities((Entity) null, bounds, e -> e.isAlive()
            && !e.isSpectator() && e.blocksBuilding && e.getPistonPushReaction() == PushReaction.IGNORE_ENTITY
        ).isEmpty();
    }
}
