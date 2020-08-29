/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.recipes;

import javax.annotation.Nonnull;

import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.computer.recipe.ComputerFamilyRecipe;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

public final class TurtleRecipe extends ComputerFamilyRecipe {
    public static final RecipeSerializer<TurtleRecipe> SERIALIZER =
        new dan200.computercraft.shared.computer.recipe.ComputerFamilyRecipe.Serializer<TurtleRecipe>() {
        @Override
        protected TurtleRecipe create(Identifier identifier, String group, int width, int height, DefaultedList<Ingredient> ingredients, ItemStack result
            , ComputerFamily family) {
            return new TurtleRecipe(identifier, group, width, height, ingredients, result, family);
        }
    };

    private TurtleRecipe(Identifier identifier, String group, int width, int height, DefaultedList<Ingredient> ingredients, ItemStack result,
                         ComputerFamily family) {
        super(identifier, group, width, height, ingredients, result, family);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Nonnull
    @Override
    protected ItemStack convert(@Nonnull IComputerItem item, @Nonnull ItemStack stack) {
        int computerID = item.getComputerID(stack);
        String label = item.getLabel(stack);

        return TurtleItemFactory.create(computerID, label, -1, this.getFamily(), null, null, 0, null);
    }
}
