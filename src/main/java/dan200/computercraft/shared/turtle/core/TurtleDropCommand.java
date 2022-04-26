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
import dan200.computercraft.api.turtle.event.TurtleInventoryEvent;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

public class TurtleDropCommand implements ITurtleCommand
{
    private final InteractDirection direction;
    private final int quantity;

    public TurtleDropCommand( InteractDirection direction, int quantity )
    {
        this.direction = direction;
        this.quantity = quantity;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Dropping nothing is easy
        if( quantity == 0 )
        {
            turtle.playAnimation( TurtleAnimation.WAIT );
            return TurtleCommandResult.success();
        }

        // Get world direction from direction
        Direction direction = this.direction.toWorldDir( turtle );

        // Get things to drop
        ItemStack stack = InventoryUtil.takeItems( quantity, turtle.getItemHandler(), turtle.getSelectedSlot(), 1, turtle.getSelectedSlot() );
        if( stack.isEmpty() )
        {
            return TurtleCommandResult.failure( "No items to drop" );
        }

        // Get inventory for thing in front
        World world = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.relative( direction );
        Direction side = direction.getOpposite();

        IItemHandler inventory = InventoryUtil.getInventory( world, newPosition, side );

        // Fire the event, restoring the inventory and exiting if it is cancelled.
        TurtlePlayer player = TurtlePlayer.getWithPosition( turtle, oldPosition, direction );
        TurtleInventoryEvent.Drop event = new TurtleInventoryEvent.Drop( turtle, player, world, newPosition, inventory, stack );
        if( MinecraftForge.EVENT_BUS.post( event ) )
        {
            InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot() );
            return TurtleCommandResult.failure( event.getFailureMessage() );
        }

        if( inventory != null )
        {
            // Drop the item into the inventory
            ItemStack remainder = InventoryUtil.storeItems( stack, inventory );
            if( !remainder.isEmpty() )
            {
                // Put the remainder back in the turtle
                InventoryUtil.storeItems( remainder, turtle.getItemHandler(), turtle.getSelectedSlot() );
            }

            // Return true if we stored anything
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
        else
        {
            // Drop the item into the world
            WorldUtil.dropItemStack( stack, world, oldPosition, direction );
            world.globalLevelEvent( 1000, newPosition, 0 );
            turtle.playAnimation( TurtleAnimation.WAIT );
            return TurtleCommandResult.success();
        }
    }
}
