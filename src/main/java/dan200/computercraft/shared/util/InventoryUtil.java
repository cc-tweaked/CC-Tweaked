/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public final class InventoryUtil
{
    private InventoryUtil() {}
    // Methods for comparing things:

    public static boolean areItemsStackable( @Nonnull ItemStack a, @Nonnull ItemStack b )
    {
        return a == b || (a.getItem() == b.getItem() && ItemStack.tagMatches( a, b ));
    }

    // Methods for finding inventories:

    public static Container getInventory( Level world, BlockPos pos, Direction side )
    {
        // Look for tile with inventory
        int y = pos.getY();
        if( y >= 0 && y < world.getHeight() )
        {
            // Check if block is InventoryProvider
            BlockState blockState = world.getBlockState( pos );
            Block block = blockState.getBlock();
            if( block instanceof WorldlyContainerHolder )
            {
                return ((WorldlyContainerHolder) block).getContainer( blockState, world, pos );
            }
            // Check if block is BlockEntity w/ Inventory
            if( blockState.hasBlockEntity() )
            {
                BlockEntity tileEntity = world.getBlockEntity( pos );

                Container inventory = getInventory( tileEntity );
                if( inventory != null )
                {
                    return inventory;
                }
            }
        }

        // Look for entity with inventory
        Vec3 vecStart = new Vec3( pos.getX() + 0.5 + 0.6 * side.getStepX(),
            pos.getY() + 0.5 + 0.6 * side.getStepY(),
            pos.getZ() + 0.5 + 0.6 * side.getStepZ() );
        Direction dir = side.getOpposite();
        Vec3 vecDir = new Vec3( dir.getStepX(), dir.getStepY(), dir.getStepZ() );
        Pair<Entity, Vec3> hit = WorldUtil.rayTraceEntities( world, vecStart, vecDir, 1.1 );
        if( hit != null )
        {
            Entity entity = hit.getKey();
            if( entity instanceof Container )
            {
                return (Container) entity;
            }
        }
        return null;
    }

    public static Container getInventory( BlockEntity tileEntity )
    {
        Level world = tileEntity.getLevel();
        BlockPos pos = tileEntity.getBlockPos();
        BlockState blockState = world.getBlockState( pos );
        Block block = blockState.getBlock();

        if( tileEntity instanceof Container )
        {
            Container inventory = (Container) tileEntity;
            if( inventory instanceof ChestBlockEntity && block instanceof ChestBlock )
            {
                return ChestBlock.getContainer( (ChestBlock) block, blockState, world, pos, true );
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
        return a == b || ItemStack.matches( a, b );
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
                count = Math.min( count, extracted.getMaxStackSize() );
            }
            else
            {
                partialStack.grow( extracted.getCount() );
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
