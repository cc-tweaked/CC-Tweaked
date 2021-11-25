/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ISidedInventoryProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class InventoryUtil
{
    private InventoryUtil() {}
    // Methods for comparing things:

    public static boolean areItemsEqual( @Nonnull ItemStack a, @Nonnull ItemStack b )
    {
        return a == b || ItemStack.matches( a, b );
    }

    public static boolean areItemsStackable( @Nonnull ItemStack a, @Nonnull ItemStack b )
    {
        return a == b || ItemHandlerHelper.canItemStacksStack( a, b );
    }

    // Methods for finding inventories:

    @Nullable
    public static IItemHandler getInventory( @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Direction side )
    {
        // Look for tile with inventory
        TileEntity tileEntity = world.getBlockEntity( pos );
        if( tileEntity != null )
        {
            LazyOptional<IItemHandler> itemHandler = tileEntity.getCapability( CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side );
            if( itemHandler.isPresent() )
            {
                return itemHandler.orElseThrow( NullPointerException::new );
            }
            else if( tileEntity instanceof ISidedInventory )
            {
                return new SidedInvWrapper( (ISidedInventory) tileEntity, side );
            }
            else if( tileEntity instanceof IInventory )
            {
                return new InvWrapper( (IInventory) tileEntity );
            }
        }

        BlockState block = world.getBlockState( pos );
        if( block.getBlock() instanceof ISidedInventoryProvider )
        {
            ISidedInventory inventory = ((ISidedInventoryProvider) block.getBlock()).getContainer( block, world, pos );
            return new SidedInvWrapper( inventory, side );
        }

        // Look for entity with inventory
        Vector3d vecStart = new Vector3d(
            pos.getX() + 0.5 + 0.6 * side.getStepX(),
            pos.getY() + 0.5 + 0.6 * side.getStepY(),
            pos.getZ() + 0.5 + 0.6 * side.getStepZ()
        );
        Direction dir = side.getOpposite();
        Vector3d vecDir = new Vector3d(
            dir.getStepX(), dir.getStepY(), dir.getStepZ()
        );
        Pair<Entity, Vector3d> hit = WorldUtil.rayTraceEntities( world, vecStart, vecDir, 1.1 );
        if( hit != null )
        {
            Entity entity = hit.getKey();
            if( entity instanceof IInventory )
            {
                return new InvWrapper( (IInventory) entity );
            }
        }
        return null;
    }

    // Methods for placing into inventories:

    @Nonnull
    public static ItemStack storeItems( @Nonnull ItemStack itemstack, IItemHandler inventory, int begin )
    {
        return storeItems( itemstack, inventory, 0, inventory.getSlots(), begin );
    }

    @Nonnull
    public static ItemStack storeItems( @Nonnull ItemStack itemstack, IItemHandler inventory )
    {
        return storeItems( itemstack, inventory, 0, inventory.getSlots(), 0 );
    }

    @Nonnull
    public static ItemStack storeItems( @Nonnull ItemStack stack, IItemHandler inventory, int start, int range, int begin )
    {
        if( stack.isEmpty() ) return ItemStack.EMPTY;

        // Inspect the slots in order and try to find empty or stackable slots
        ItemStack remainder = stack.copy();
        for( int i = 0; i < range; i++ )
        {
            int slot = start + (i + begin - start) % range;
            if( remainder.isEmpty() ) break;
            remainder = inventory.insertItem( slot, remainder, false );
        }
        return areItemsEqual( stack, remainder ) ? stack : remainder;
    }

    // Methods for taking out of inventories

    @Nonnull
    public static ItemStack takeItems( int count, IItemHandler inventory, int begin )
    {
        return takeItems( count, inventory, 0, inventory.getSlots(), begin );
    }

    @Nonnull
    public static ItemStack takeItems( int count, IItemHandler inventory )
    {
        return takeItems( count, inventory, 0, inventory.getSlots(), 0 );
    }

    @Nonnull
    public static ItemStack takeItems( int count, IItemHandler inventory, int start, int range, int begin )
    {
        // Combine multiple stacks from inventory into one if necessary
        ItemStack partialStack = ItemStack.EMPTY;
        for( int i = 0; i < range; i++ )
        {
            int slot = start + (i + begin - start) % range;

            // If we've extracted all items, return
            if( count <= 0 ) break;

            // If this doesn't slot, abort.
            ItemStack stack = inventory.getStackInSlot( slot );
            if( !stack.isEmpty() && (partialStack.isEmpty() || areItemsStackable( stack, partialStack )) )
            {
                ItemStack extracted = inventory.extractItem( slot, count, false );
                if( !extracted.isEmpty() )
                {
                    if( partialStack.isEmpty() )
                    {
                        // If we've extracted for this first time, then limit the count to the maximum stack size.
                        partialStack = extracted;
                        count = Math.min( count, extracted.getMaxStackSize() );
                    }
                    else
                    {
                        partialStack.grow( extracted.getCount() );
                    }

                    count -= extracted.getCount();
                }
            }

        }

        return partialStack;
    }
}
