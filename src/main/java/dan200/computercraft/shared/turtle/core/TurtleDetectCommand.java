/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import javax.annotation.Nonnull;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.util.WorldUtil;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class TurtleDetectCommand implements ITurtleCommand {
    private final InteractDirection m_direction;

    public TurtleDetectCommand(InteractDirection direction) {
        this.m_direction = direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        // Get world direction from direction
        Direction direction = this.m_direction.toWorldDir(turtle);

        // Check if thing in front is air or not
        World world = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.offset(direction);

        if (!WorldUtil.isLiquidBlock(world, newPosition) && !world.isAir(newPosition)) {
            return TurtleCommandResult.success();
        }

        return TurtleCommandResult.failure();
    }
}
