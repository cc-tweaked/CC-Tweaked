/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import dan200.computercraft.api.turtle.event.TurtleEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class TurtleInspectCommand implements ITurtleCommand
{
    private final InteractDirection m_direction;

    public TurtleInspectCommand( InteractDirection direction )
    {
        m_direction = direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Get world direction from direction
        Direction direction = m_direction.toWorldDir( turtle );

        // Check if thing in front is air or not
        World world = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.offset( direction );

        BlockState state = world.getBlockState( newPosition );
        if( state.isAir() )
        {
            return TurtleCommandResult.failure( "No block to inspect" );
        }

        Block block = state.getBlock();
        String name = Registry.BLOCK.getId( block ).toString();

        Map<String, Object> table = new HashMap<>();
        table.put( "name", name );

        Map<Object, Object> stateTable = new HashMap<>();
        for( ImmutableMap.Entry<Property<?>, ?> entry : state.getEntries().entrySet() )
        {
            String propertyName = entry.getKey().getName();
            Object value = entry.getValue();
            if( value instanceof String || value instanceof Number || value instanceof Boolean )
            {
                stateTable.put( propertyName, value );
            }
            else
            {
                stateTable.put( propertyName, value.toString() );
            }
        }
        table.put( "state", stateTable );

        // Fire the event, exiting if it is cancelled
        TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, oldPosition, direction );
        TurtleBlockEvent.Inspect event = new TurtleBlockEvent.Inspect( turtle, turtlePlayer, world, newPosition, state, table );
        if( TurtleEvent.post( event ) ) return TurtleCommandResult.failure( event.getFailureMessage() );

        return TurtleCommandResult.success( new Object[] { table } );

    }
}
