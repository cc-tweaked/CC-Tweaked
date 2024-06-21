// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.recipe;

import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/**
 * A fake {@link ShapelessRecipe}, which appears in the recipe book (and other recipe mods), but cannot be crafted.
 * <p>
 * This is used to represent examples for our {@link CustomRecipe}s.
 */
public final class ImpostorShapelessRecipe extends CustomShapelessRecipe {
    public ImpostorShapelessRecipe(ShapelessRecipeSpec recipe) {
        super(recipe);
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider access) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<ImpostorShapelessRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.IMPOSTOR_SHAPELESS.get();
    }
}
