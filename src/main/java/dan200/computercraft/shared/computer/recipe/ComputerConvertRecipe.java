/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Represents a recipe which converts a computer from one form into another.
 */
public abstract class ComputerConvertRecipe extends ShapedRecipe
{
    private final String group;

    public ComputerConvertRecipe( Identifier identifier, String group, int width, int height, DefaultedList<Ingredient> ingredients, ItemStack result )
    {
        super( identifier, group, width, height, ingredients, result );
        this.group = group;
    }

    @Nonnull
    protected abstract ItemStack convert( @Nonnull IComputerItem item, @Nonnull ItemStack stack );

    @Override
    public boolean matches( @Nonnull CraftingInventory inventory, @Nonnull World world )
    {
        if( !super.matches( inventory, world ) ) return false;

        for( int i = 0; i < inventory.size(); i++ )
        {
            if( inventory.getStack( i ).getItem() instanceof IComputerItem ) return true;
        }

        return false;
    }

    @Nonnull
    @Override
    public ItemStack craft( @Nonnull CraftingInventory inventory )
    {
        // Find our computer item and convert it.
        for( int i = 0; i < inventory.size(); i++ )
        {
            ItemStack stack = inventory.getStack( i );
            if( stack.getItem() instanceof IComputerItem ) return convert( (IComputerItem) stack.getItem(), stack );
        }

        return ItemStack.EMPTY;
    }

    @Nonnull
    @Override
    public String getGroup()
    {
        return group;
    }
}
