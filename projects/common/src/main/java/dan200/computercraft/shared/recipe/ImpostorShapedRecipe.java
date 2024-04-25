// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.recipe;

import dan200.computercraft.shared.ModRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A fake {@link ShapedRecipe}, which appears in the recipe book (and other recipe mods), but cannot be crafted.
 * <p>
 * This is used to represent examples for our {@link CustomRecipe}s.
 */
public final class ImpostorShapedRecipe extends CustomShapedRecipe {
    public ImpostorShapedRecipe(ShapedRecipeSpec recipe) {
        super(recipe);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level world) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, HolderLookup.Provider registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<ImpostorShapedRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.IMPOSTOR_SHAPED.get();
    }
}
