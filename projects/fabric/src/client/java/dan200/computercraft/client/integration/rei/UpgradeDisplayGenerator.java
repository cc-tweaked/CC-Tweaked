// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration.rei;

import dan200.computercraft.shared.integration.UpgradeRecipeGenerator;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultShapedDisplay;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Provides custom recipe and usage hints for pocket/turtle upgrades.
 */
class UpgradeDisplayGenerator implements DynamicDisplayGenerator<DefaultCraftingDisplay<?>> {
    private final UpgradeRecipeGenerator<DefaultCraftingDisplay<?>> resolver = new UpgradeRecipeGenerator<>(GeneratedShapedDisplay::new);

    @Override
    public Optional<List<DefaultCraftingDisplay<?>>> getRecipeFor(EntryStack<?> entry) {
        return entry.getType() == VanillaEntryTypes.ITEM ? Optional.of(resolver.findRecipesWithOutput(entry.castValue())) : Optional.empty();
    }

    @Override
    public Optional<List<DefaultCraftingDisplay<?>>> getUsageFor(EntryStack<?> entry) {
        return entry.getType() == VanillaEntryTypes.ITEM ? Optional.of(resolver.findRecipesWithInput(entry.castValue())) : Optional.empty();
    }

    /**
     * Similar to {@link DefaultShapedDisplay}, but does not require a {@link RecipeHolder}.
     */
    private static class GeneratedShapedDisplay extends DefaultCraftingDisplay<ShapedRecipe> {
        private final int width, height;

        GeneratedShapedDisplay(ShapedRecipe recipe) {
            super(
                EntryIngredients.ofIngredients(recipe.getIngredients()),
                Collections.singletonList(EntryIngredients.of(recipe.getResultItem(BasicDisplay.registryAccess()))),
                Optional.empty()
            );
            width = recipe.getWidth();
            height = recipe.getHeight();
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }
}
