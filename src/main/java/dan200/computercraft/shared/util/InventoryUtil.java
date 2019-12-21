/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public final class InventoryUtil
{
    private InventoryUtil() {}
    // Methods for comparing things:

    public static boolean areItemsEqual( @Nonnull ItemStack a, @Nonnull ItemStack b )
    {
        return a == b || ItemStack.areItemStacksEqual( a, b );
    }

    public static boolean areItemsStackable( @Nonnull ItemStack a, @Nonnull ItemStack b )
    {
        return a == b || ItemHandlerHelper.canItemStacksStack( a, b );
    }

    /**
     * Determines if two items are "mostly" equivalent. Namely, they have the same item and damage, and identical
     * share stacks.
     *
     * This is largely based on {@link net.minecraftforge.common.crafting.IngredientNBT#apply(ItemStack)}. It is
     * sufficient to ensure basic information (such as enchantments) are the same, while not having to worry about
     * capabilities.
     *
     * @param a The first stack to check
     * @param b The second stack to check
     * @return If these items are largely the same.
     */
    public static boolean areItemsSimilar( @Nonnull ItemStack a, @Nonnull ItemStack b )
    {
        if( a == b ) return true;
        if( a.isEmpty() ) return !b.isEmpty();

        if( a.getItem() != b.getItem() || a.getItemDamage() != b.getItemDamage() ) return false;

        // A more expanded form of ItemStack.areItemStackShareTagsEqual, but allowing an empty tag to be equal to a
        // null one.
        NBTTagCompound shareTagA = a.getItem().getNBTShareTag( a );
        NBTTagCompound shareTagB = b.getItem().getNBTShareTag( b );
        if( shareTagA == shareTagB ) return true;
        if( shareTagA == null ) return shareTagB.isEmpty();
        if( shareTagB == null ) return shareTagA.isEmpty();
        return shareTagA.equals( shareTagB );
    }

    // Methods for finding inventories:

    public static IItemHandler getInventory( World world, BlockPos pos, EnumFacing side )
    {
        // Look for tile with inventory
        TileEntity tileEntity = world.getTileEntity( pos );
        if( tileEntity != null )
        {
            IItemHandler itemHandler = tileEntity.getCapability( CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side );
            if( itemHandler != null )
            {
                return itemHandler;
            }
            else if( side != null && tileEntity instanceof ISidedInventory )
            {
                return new SidedInvWrapper( (ISidedInventory) tileEntity, side );
            }
            else if( tileEntity instanceof IInventory )
            {
                return new InvWrapper( (IInventory) tileEntity );
            }
        }

        // Look for entity with inventory
        Vec3d vecStart = new Vec3d(
            pos.getX() + 0.5 + 0.6 * side.getXOffset(),
            pos.getY() + 0.5 + 0.6 * side.getYOffset(),
            pos.getZ() + 0.5 + 0.6 * side.getZOffset()
        );
        EnumFacing dir = side.getOpposite();
        Vec3d vecDir = new Vec3d(
            dir.getXOffset(), dir.getYOffset(), dir.getZOffset()
        );
        Pair<Entity, Vec3d> hit = WorldUtil.rayTraceEntities( world, vecStart, vecDir, 1.1 );
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

            if( count <= 0 ) break;

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
