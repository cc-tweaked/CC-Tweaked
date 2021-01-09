/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

import dan200.computercraft.shared.computer.recipe.ComputerFamilyRecipe.Serializer;

public class ComputerUpgradeRecipe extends ComputerFamilyRecipe
{
    public ComputerUpgradeRecipe( ResourceLocation identifier, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family )
    {
        super( identifier, group, width, height, ingredients, result, family );
    }

    @Nonnull
    @Override
    protected ItemStack convert( @Nonnull IComputerItem item, @Nonnull ItemStack stack )
    {
        return item.withFamily( stack, getFamily() );
    }

    @Nonnull
    @Override
    public IRecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final IRecipeSerializer<ComputerUpgradeRecipe> SERIALIZER = new Serializer<ComputerUpgradeRecipe>()
    {
        @Override
        protected ComputerUpgradeRecipe create( ResourceLocation identifier, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family )
        {
            return new ComputerUpgradeRecipe( identifier, group, width, height, ingredients, result, family );
        }
    };
}
