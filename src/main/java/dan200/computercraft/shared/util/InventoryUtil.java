/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public final class InventoryUtil
{
    private InventoryUtil() {}
    // Methods for comparing things:

    public static boolean areItemsStackable( @Nonnull ItemStack a, @Nonnull ItemStack b )
    {
        return a == b || (a.getItem() == b.getItem() && ItemStack.areTagsEqual( a, b ));
    }

    // Methods for finding inventories:

    public static Inventory getInventory( World world, BlockPos pos, Direction side )
    {
        // Look for tile with inventory
        int y = pos.getY();
        if( y >= 0 && y < world.getHeight() )
        {
            // Check if block is InventoryProvider
            BlockState blockState = world.getBlockState( pos );
            Block block = blockState.getBlock();
            if( block instanceof InventoryProvider )
            {
                return ((InventoryProvider) block).getInventory( blockState, world, pos );
            }
            // Check if block is BlockEntity w/ Inventory
            if( blockState.hasBlockEntity() )
            {
                BlockEntity tileEntity = world.getBlockEntity( pos );

                Inventory inventory = getInventory( tileEntity );
                if( inventory != null )
                {
                    return inventory;
                }
            }
        }

        // Look for entity with inventory
        Vec3d vecStart = new Vec3d( pos.getX() + 0.5 + 0.6 * side.getOffsetX(),
            pos.getY() + 0.5 + 0.6 * side.getOffsetY(),
            pos.getZ() + 0.5 + 0.6 * side.getOffsetZ() );
        Direction dir = side.getOpposite();
        Vec3d vecDir = new Vec3d( dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ() );
        Pair<Entity, Vec3d> hit = WorldUtil.rayTraceEntities( world, vecStart, vecDir, 1.1 );
        if( hit != null )
        {
            Entity entity = hit.getKey();
            if( entity instanceof Inventory )
            {
                return (Inventory) entity;
            }
        }
        return null;
    }

    public static Inventory getInventory( BlockEntity tileEntity )
    {
        World world = tileEntity.getWorld();
        BlockPos pos = tileEntity.getPos();
        BlockState blockState = world.getBlockState( pos );
        Block block = blockState.getBlock();

        if( tileEntity instanceof Inventory )
        {
            Inventory inventory = (Inventory) tileEntity;
            if( inventory instanceof ChestBlockEntity && block instanceof ChestBlock )
            {
                return ChestBlock.getInventory( (ChestBlock) block, blockState, world, pos, true );
            }
            return inventory;
        }

        return null;
    }

    @Nonnull
    public static ItemStack storeItems( @Nonnull ItemStack itemstack, ItemStorage inventory, int begin )
    {
        return storeItems( itemstack, inventory, 0, inventory.size(), begin );
    }

    // Methods for placing into inventories:

    @Nonnull
    public static ItemStack storeItems( @Nonnull ItemStack stack, ItemStorage inventory, int start, int range, int begin )
    {
        if( stack.isEmpty() )
        {
            return ItemStack.EMPTY;
        }

        // Inspect the slots in order and try to find empty or stackable slots
        ItemStack remainder = stack.copy();
        for( int i = 0; i < range; i++ )
        {
            int slot = start + (i + begin - start) % range;
            if( remainder.isEmpty() )
            {
                break;
            }
            remainder = inventory.store( slot, remainder, false );
        }
        return areItemsEqual( stack, remainder ) ? stack : remainder;
    }

    public static boolean areItemsEqual( @Nonnull ItemStack a, @Nonnull ItemStack b )
    {
        return a == b || ItemStack.areEqual( a, b );
    }

    @Nonnull
    public static ItemStack storeItems( @Nonnull ItemStack itemstack, ItemStorage inventory )
    {
        return storeItems( itemstack, inventory, 0, inventory.size(), 0 );
    }

    // Methods for taking out of inventories

    @Nonnull
    public static ItemStack takeItems( int count, ItemStorage inventory, int begin )
    {
        return takeItems( count, inventory, 0, inventory.size(), begin );
    }

    @Nonnull
    public static ItemStack takeItems( int count, ItemStorage inventory, int start, int range, int begin )
    {
        ItemStack partialStack = ItemStack.EMPTY;
        for( int i = 0; i < range; i++ )
        {
            int slot = start + (i + begin - start) % range;

            if( count <= 0 )
            {
                break;
            }

            // If this doesn't slot, abort.
            ItemStack extracted = inventory.take( slot, count, partialStack, false );
            if( extracted.isEmpty() )
            {
                continue;
            }

            count -= extracted.getCount();
            if( partialStack.isEmpty() )
            {
                // If we've extracted for this first time, then limit the count to the maximum stack size.
                partialStack = extracted;
                count = Math.min( count, extracted.getMaxCount() );
            }
            else
            {
                partialStack.increment( extracted.getCount() );
            }
        }

        return partialStack;
    }

    @Nonnull
    public static ItemStack takeItems( int count, ItemStorage inventory )
    {
        return takeItems( count, inventory, 0, inventory.size(), 0 );
    }
}
