/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import javax.annotation.Nonnull;

public class ComputerUpgradeRecipe extends ComputerFamilyRecipe
{
    public ComputerUpgradeRecipe( Identifier identifier, String group, int width, int height, DefaultedList<Ingredient> ingredients, ItemStack result, ComputerFamily family )
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

    public static final RecipeSerializer<ComputerUpgradeRecipe> SERIALIZER = new dan200.computercraft.shared.computer.recipe.ComputerFamilyRecipe.Serializer<ComputerUpgradeRecipe>()
    {
        @Override
        protected ComputerUpgradeRecipe create( Identifier identifier, String group, int width, int height, DefaultedList<Ingredient> ingredients, ItemStack result, ComputerFamily family )
        {
            return new ComputerUpgradeRecipe( identifier, group, width, height, ingredients, result, family );
        }
    };
}
