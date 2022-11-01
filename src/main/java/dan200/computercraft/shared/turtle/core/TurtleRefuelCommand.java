/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.impl.TurtleRefuelHandlers;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class TurtleRefuelCommand implements ITurtleCommand
{
    private final int limit;

    public TurtleRefuelCommand( int limit )
    {
        this.limit = limit;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        int slot = turtle.getSelectedSlot();
        ItemStack stack = turtle.getInventory().getItem( slot );
        if( stack.isEmpty() ) return TurtleCommandResult.failure( "No items to combust" );

        var refuelled = TurtleRefuelHandlers.refuel( turtle, stack, slot, limit );
        if( refuelled.isEmpty() ) return TurtleCommandResult.failure( "Items not combustible" );

        var newFuel = refuelled.getAsInt();
        if( newFuel != 0 )
        {
            turtle.addFuel( newFuel );
            turtle.playAnimation( TurtleAnimation.WAIT );
        }

        return TurtleCommandResult.success();
    }
}
