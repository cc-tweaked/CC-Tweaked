/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * Represents a recipe which converts a computer from one form into another.
 */
public abstract class ComputerConvertRecipe extends ShapedRecipe
{
    private final String group;

    public ComputerConvertRecipe( ResourceLocation identifier, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result )
    {
        super( identifier, group, width, height, ingredients, result );
        this.group = group;
    }

    @Nonnull
    protected abstract ItemStack convert( @Nonnull IComputerItem item, @Nonnull ItemStack stack );

    @Override
    public boolean matches( @Nonnull CraftingContainer inventory, @Nonnull Level world )
    {
        if( !super.matches( inventory, world ) ) return false;

        for( int i = 0; i < inventory.getContainerSize(); i++ )
        {
            if( inventory.getItem( i ).getItem() instanceof IComputerItem ) return true;
        }

        return false;
    }

    @Nonnull
    @Override
    public ItemStack assemble( @Nonnull CraftingContainer inventory )
    {
        // Find our computer item and convert it.
        for( int i = 0; i < inventory.getContainerSize(); i++ )
        {
            ItemStack stack = inventory.getItem( i );
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
