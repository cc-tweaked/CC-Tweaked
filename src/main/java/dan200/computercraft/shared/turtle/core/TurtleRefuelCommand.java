/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.ItemStorage;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class TurtleRefuelCommand implements ITurtleCommand
{
    private final int m_limit;

    public TurtleRefuelCommand( int limit )
    {
        m_limit = limit;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        if( m_limit == 0 )
        {
            // If limit is zero, just check the item is combustible
            ItemStack dummyStack = turtle.getInventory().getInvStack( turtle.getSelectedSlot() );
            if( !dummyStack.isEmpty() )
            {
                return refuel( turtle, dummyStack, true );
            }
        }
        else
        {
            // Otherwise, refuel for real
            // Remove items from inventory
            ItemStorage storage = ItemStorage.wrap( turtle.getInventory() );
            ItemStack stack = InventoryUtil.takeItems( m_limit, storage, turtle.getSelectedSlot(), 1, turtle.getSelectedSlot() );
            if( !stack.isEmpty() )
            {
                TurtleCommandResult result = refuel( turtle, stack, false );
                if( !result.isSuccess() )
                {
                    // If the items weren't burnt, put them back
                    InventoryUtil.storeItems( stack, storage, turtle.getSelectedSlot() );
                }
                return result;
            }
        }
        return TurtleCommandResult.failure( "No items to combust" );
    }

    private int getFuelPerItem( @Nonnull ItemStack stack )
    {
        return (FurnaceBlockEntity.createFuelTimeMap().getOrDefault( stack.getItem(), 0 ) * 5) / 100;
    }

    private TurtleCommandResult refuel( ITurtleAccess turtle, @Nonnull ItemStack stack, boolean testOnly )
    {
        // Check if item is fuel
        int fuelPerItem = getFuelPerItem( stack );
        if( fuelPerItem <= 0 )
        {
            return TurtleCommandResult.failure( "Items not combustible" );
        }

        TurtleActionEvent event = new TurtleActionEvent( turtle, TurtleAction.REFUEL );
        if( TurtleEvent.post( event ) )
        {
            return TurtleCommandResult.failure( event.getFailureMessage() );
        }

        if( !testOnly )
        {
            // Determine fuel to give and replacement item to leave behind
            int fuelToGive = fuelPerItem * stack.getAmount();
            Item replacement = stack.getItem().getRecipeRemainder();

            // Update fuel level
            turtle.addFuel( fuelToGive );

            // Store the replacement item in the inventory
            if( replacement != null )
            {
                InventoryUtil.storeItems( new ItemStack( replacement, stack.getAmount() ), ItemStorage.wrap( turtle.getInventory() ), turtle.getSelectedSlot() );
            }

            // Animate
            turtle.playAnimation( TurtleAnimation.Wait );
        }

        return TurtleCommandResult.success();
    }
}
