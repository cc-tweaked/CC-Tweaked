/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import dan200.computercraft.api.turtle.event.TurtleEvent;

import dan200.computercraft.shared.peripheral.generic.data.BlockData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class TurtleInspectCommand implements ITurtleCommand {
    private final InteractDirection direction;

    public TurtleInspectCommand(InteractDirection direction) {
        this.direction = direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute(@Nonnull ITurtleAccess turtle) {
        // Get world direction from direction
        Direction direction = this.direction.toWorldDir(turtle);

        // Check if thing in front is air or not
        World world = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.offset(direction);

        BlockState state = world.getBlockState(newPosition);
        if (state.isAir()) {
            return TurtleCommandResult.failure("No block to inspect");
        }

        Map<String, Object> table = BlockData.fill( new HashMap<>(), state );

        // Fire the event, exiting if it is cancelled
        TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer(turtle, oldPosition, direction);
        TurtleBlockEvent.Inspect event = new TurtleBlockEvent.Inspect(turtle, turtlePlayer, world, newPosition, state, table);
        if (TurtleEvent.post(event)) {
            return TurtleCommandResult.failure(event.getFailureMessage());
        }

        return TurtleCommandResult.success(new Object[] {table});
    }

    @SuppressWarnings ({
        "unchecked",
        "rawtypes"
    })
    private static Object getPropertyValue(Property property, Comparable value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return property.name(value);
    }
}
