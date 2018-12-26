/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleInventoryEvent;
import dan200.computercraft.shared.util.InventoryUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.List;

public class TurtleSuckCommand implements ITurtleCommand
{
    private final InteractDirection m_direction;
    private final int m_quantity;

    public TurtleSuckCommand( InteractDirection direction, int quantity )
    {
        m_direction = direction;
        m_quantity = quantity;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Sucking nothing is easy
        if( m_quantity == 0 )
        {
            turtle.playAnimation( TurtleAnimation.Wait );
            return TurtleCommandResult.success();
        }

        // Get world direction from direction
        EnumFacing direction = m_direction.toWorldDir( turtle );

        // Get inventory for thing in front
        World world = turtle.getWorld();
        BlockPos oldPosition = turtle.getPosition();
        BlockPos newPosition = oldPosition.offset( direction );
        EnumFacing side = direction.getOpposite();

        IItemHandler inventory = InventoryUtil.getInventory( world, newPosition, side );

        // Fire the event, exiting if it is cancelled.
        TurtlePlayer player = TurtlePlaceCommand.createPlayer( turtle, oldPosition, direction );
        TurtleInventoryEvent.Suck event = new TurtleInventoryEvent.Suck( turtle, player, world, newPosition, inventory );
        if( MinecraftForge.EVENT_BUS.post( event ) )
        {
            return TurtleCommandResult.failure( event.getFailureMessage() );
        }

        if( inventory != null )
        {
            // Take from inventory of thing in front
            ItemStack stack = InventoryUtil.takeItems( m_quantity, inventory );
            if( !stack.isEmpty() )
            {
                // Try to place into the turtle
                ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot() );
                if( !remainder.isEmpty() )
                {
                    // Put the remainder back in the inventory
                    InventoryUtil.storeItems( remainder, inventory );
                }

                // Return true if we consumed anything
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
            return TurtleCommandResult.failure( "No items to take" );
        }
        else
        {
            // Suck up loose items off the ground
            AxisAlignedBB aabb = new AxisAlignedBB(
                newPosition.getX(), newPosition.getY(), newPosition.getZ(),
                newPosition.getX() + 1.0, newPosition.getY() + 1.0, newPosition.getZ() + 1.0
            );
            List<Entity> list = world.getEntitiesWithinAABBExcludingEntity( null, aabb );
            if( list.size() > 0 )
            {
                boolean foundItems = false;
                boolean storedItems = false;
                for( Entity entity : list )
                {
                    if( entity != null && entity instanceof EntityItem && !entity.isDead )
                    {
                        // Suck up the item
                        foundItems = true;
                        EntityItem entityItem = (EntityItem) entity;
                        ItemStack stack = entityItem.getItem().copy();
                        ItemStack storeStack;
                        ItemStack leaveStack;
                        if( stack.getCount() > m_quantity )
                        {
                            storeStack = stack.splitStack( m_quantity );
                            leaveStack = stack;
                        }
                        else
                        {
                            storeStack = stack;
                            leaveStack = ItemStack.EMPTY;
                        }
                        ItemStack remainder = InventoryUtil.storeItems( storeStack, turtle.getItemHandler(), turtle.getSelectedSlot() );
                        if( remainder != storeStack )
                        {
                            storedItems = true;
                            if( remainder.isEmpty() && leaveStack.isEmpty() )
                            {
                                entityItem.setDead();
                            }
                            else if( remainder.isEmpty() )
                            {
                                entityItem.setItem( leaveStack );
                            }
                            else if( leaveStack.isEmpty() )
                            {
                                entityItem.setItem( remainder );
                            }
                            else
                            {
                                leaveStack.grow( remainder.getCount() );
                                entityItem.setItem( leaveStack );
                            }
                            break;
                        }
                    }
                }

                if( foundItems )
                {
                    if( storedItems )
                    {
                        // Play fx
                        world.playBroadcastSound( 1000, oldPosition, 0 );
                        turtle.playAnimation( TurtleAnimation.Wait );
                        return TurtleCommandResult.success();
                    }
                    else
                    {
                        return TurtleCommandResult.failure( "No space for items" );
                    }
                }
            }
            return TurtleCommandResult.failure( "No items to take" );
        }
    }
}
