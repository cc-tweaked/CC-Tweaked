/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.recipe;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ComputerUpgradeRecipe extends ComputerFamilyRecipe {
    private ComputerUpgradeRecipe(ResourceLocation identifier, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family) {
        super(identifier, group, category, width, height, ingredients, result, family);
    }

    @Override
    protected ItemStack convert(IComputerItem item, ItemStack stack) {
        return item.withFamily(stack, getFamily());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.RecipeSerializers.COMPUTER_UPGRADE.get();
    }

    public static class Serializer extends ComputerFamilyRecipe.Serializer<ComputerUpgradeRecipe> {
        @Override
        protected ComputerUpgradeRecipe create(ResourceLocation identifier, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, ComputerFamily family) {
            return new ComputerUpgradeRecipe(identifier, group, category, width, height, ingredients, result, family);
        }
    }
}
