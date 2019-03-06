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
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.api.turtle.event.TurtleInventoryEvent;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.ItemStorage;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TurtleDropCommand implements ITurtleCommand
{
    private final InteractDirection m_direction;
    private final int m_quantity;

    public TurtleDropCommand( InteractDirection direction, int quantity )
    {
        m_direction = direction;
        m_quantity = quantity;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Dropping nothing is easy
        if( m_quantity == 0 )
        {
            turtle.playAnimation( TurtleAnimation.Wait );
            return TurtleCommandResult.success();
        }

        // Get world direction from direction
        Direction direction = m_direction.toWorldDir( turtle );
        ItemStorage storage = ItemStorage.wrap( turtle.getInventory() );

        // Get things to drop
        ItemStack stack = InventoryUtil.takeItems( m_quantity, storage, turtle.getSelectedSlot(), 1, turtle.getSelectedSlot() );
        if( stack.isEmpty() )
        {
            return TurtleCommandResult.failure( "No items to drop" );
        }

        // Get inventory for thing in front
        World world = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.offset( direction );
        Direction side = direction.getOpposite();

        Inventory inventory = InventoryUtil.getInventory( world, newPosition, side );

        // Fire the event, restoring the inventory and exiting if it is cancelled.
        TurtlePlayer player = TurtlePlaceCommand.createPlayer( turtle, oldPosition, direction );
        TurtleInventoryEvent.Drop event = new TurtleInventoryEvent.Drop( turtle, player, world, newPosition, inventory, stack );
        if( TurtleEvent.post( event ) )
        {
            InventoryUtil.storeItems( stack, storage, turtle.getSelectedSlot() );
            return TurtleCommandResult.failure( event.getFailureMessage() );
        }

        if( inventory != null )
        {
            // Drop the item into the inventory
            ItemStack remainder = InventoryUtil.storeItems( stack, ItemStorage.wrap( inventory, side ) );
            if( !remainder.isEmpty() )
            {
                // Put the remainder back in the turtle
                InventoryUtil.storeItems( remainder, storage, turtle.getSelectedSlot() );
            }

            // Return true if we stored anything
            if( remainder != stack )
            {
                turtle.playAnimation( TurtleAnimation.Wait );
                return TurtleCommandResult.success();
            }
            else
            {
                return TurtleCommandResult.failure( "No space for items" );
            }
        }
        else
        {
            // Drop the item into the world
            WorldUtil.dropItemStack( stack, world, oldPosition, direction );
            world.playEvent( 1000, newPosition, 0 ); // BLOCK_DISPENSER_DISPENSE
            turtle.playAnimation( TurtleAnimation.Wait );
            return TurtleCommandResult.success();
        }
    }
}
