/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import dan200.computercraft.shared.peripheral.generic.data.BlockData;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class TurtleInspectCommand implements ITurtleCommand
{
    private final InteractDirection direction;

    public TurtleInspectCommand( InteractDirection direction )
    {
        this.direction = direction;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Get world direction from direction
        Direction direction = this.direction.toWorldDir( turtle );

        // Check if thing in front is air or not
        World world = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.offset( direction );

        BlockState state = world.getBlockState( newPosition );
        if( state.getBlock().isAir( state, world, newPosition ) )
        {
            return TurtleCommandResult.failure( "No block to inspect" );
        }

        Map<String, Object> table = BlockData.fill( new HashMap<>(), state );

        // Fire the event, exiting if it is cancelled
        TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, oldPosition, direction );
        TurtleBlockEvent.Inspect event = new TurtleBlockEvent.Inspect( turtle, turtlePlayer, world, newPosition, state, table );
        if( MinecraftForge.EVENT_BUS.post( event ) ) return TurtleCommandResult.failure( event.getFailureMessage() );

        return TurtleCommandResult.success( new Object[] { table } );

    }
}
