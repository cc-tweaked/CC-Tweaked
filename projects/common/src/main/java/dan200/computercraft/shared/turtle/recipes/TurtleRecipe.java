// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.turtle.recipes;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.computer.recipe.ComputerFamilyRecipe;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class TurtleRecipe extends ComputerFamilyRecipe {
    public TurtleRecipe(ResourceLocation identifier, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family) {
        super(identifier, group, category, width, height, ingredients, result, family);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.TURTLE.get();
    }

    @Override
    protected ItemStack convert(IComputerItem item, ItemStack stack) {
        var computerID = item.getComputerID(stack);
        var label = item.getLabel(stack);

        return TurtleItemFactory.create(computerID, label, -1, getFamily(), null, null, 0, null, null, null);
    }

    public static class Serializer extends ComputerFamilyRecipe.Serializer<TurtleRecipe> {
        @Override
        protected TurtleRecipe create(ResourceLocation identifier, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family) {
            return new TurtleRecipe(identifier, group, category, width, height, ingredients, result, family);
        }
    }
}
