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
import me.shedaniel.rei.plugin.common.displays.crafting.CraftingDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapedDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;

import java.util.List;
import java.util.Optional;

/**
 * Provides custom recipe and usage hints for pocket/turtle upgrades.
 */
class UpgradeDisplayGenerator implements DynamicDisplayGenerator<CraftingDisplay> {
    private final UpgradeRecipeGenerator<CraftingDisplay> resolver = new UpgradeRecipeGenerator<>(UpgradeDisplayGenerator::makeDisplay, BasicDisplay.registryAccess());

    @Override
    public Optional<List<CraftingDisplay>> getRecipeFor(EntryStack<?> entry) {
        return entry.getType() == VanillaEntryTypes.ITEM ? Optional.of(resolver.findRecipesWithOutput(entry.castValue())) : Optional.empty();
    }

    @Override
    public Optional<List<CraftingDisplay>> getUsageFor(EntryStack<?> entry) {
        return entry.getType() == VanillaEntryTypes.ITEM ? Optional.of(resolver.findRecipesWithInput(entry.castValue())) : Optional.empty();
    }

    private static CraftingDisplay makeDisplay(ShapedCraftingRecipeDisplay recipe) {
        return new DefaultCustomShapedDisplay(
            EntryIngredients.ofSlotDisplays(recipe.ingredients()),
            List.of(EntryIngredients.ofSlotDisplay(recipe.result())),
            Optional.empty(),
            recipe.width(),
            recipe.height()
        );
    }
}
