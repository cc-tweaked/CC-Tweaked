/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.Nonnull;

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
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public static final RecipeSerializer<ComputerUpgradeRecipe> SERIALIZER = new Serializer<>()
    {
        @Override
        protected ComputerUpgradeRecipe create( ResourceLocation identifier, String group, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family )
        {
            return new ComputerUpgradeRecipe( identifier, group, width, height, ingredients, result, family );
        }
    };
}
