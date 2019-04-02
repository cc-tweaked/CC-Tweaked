/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nonnull;

/**
 * Represents a recipe which converts a computer from one form into another.
 */
public abstract class ComputerConvertRecipe extends ShapedRecipes
{
    public ComputerConvertRecipe( String group, @Nonnull CraftingHelper.ShapedPrimer primer, @Nonnull ItemStack result )
    {
        super( group, primer.width, primer.height, primer.input, result );
    }

    @Nonnull
    protected abstract ItemStack convert( @Nonnull IComputerItem item, @Nonnull ItemStack stack );

    @Override
    public boolean matches( @Nonnull InventoryCrafting inventory, @Nonnull World world )
    {
        if( !super.matches( inventory, world ) ) return false;

        for( int i = 0; i < inventory.getSizeInventory(); i++ )
        {
            if( inventory.getStackInSlot( i ).getItem() instanceof IComputerItem ) return true;
        }

        return false;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingResult( @Nonnull InventoryCrafting inventory )
    {
        // Find our computer item and convert it.
        for( int i = 0; i < inventory.getSizeInventory(); i++ )
        {
            ItemStack stack = inventory.getStackInSlot( i );
            if( stack.getItem() instanceof IComputerItem ) return convert( (IComputerItem) stack.getItem(), stack );
        }

        return ItemStack.EMPTY;
    }
}
