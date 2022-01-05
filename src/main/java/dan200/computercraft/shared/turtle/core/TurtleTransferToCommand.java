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
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public class TurtleTransferToCommand implements ITurtleCommand
{
    private final int slot;
    private final int quantity;

    public TurtleTransferToCommand( int slot, int limit )
    {
        this.slot = slot;
        quantity = limit;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Take stack
        ItemStack stack = InventoryUtil.takeItems( quantity, turtle.getItemHandler(), turtle.getSelectedSlot(), 1, turtle.getSelectedSlot() );
        if( stack.isEmpty() )
        {
            turtle.playAnimation( TurtleAnimation.WAIT );
            return TurtleCommandResult.success();
        }

        // Store stack
        ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), slot, 1, slot );
        if( !remainder.isEmpty() )
        {
            // Put the remainder back
            InventoryUtil.storeItems( remainder, turtle.getItemHandler(), turtle.getSelectedSlot(), 1, turtle.getSelectedSlot() );
        }

        // Return true if we moved anything
        if( remainder != stack )
        {
            turtle.playAnimation( TurtleAnimation.WAIT );
            return TurtleCommandResult.success();
        }
        else
        {
            return TurtleCommandResult.failure( "No space for items" );
        }
    }
}
