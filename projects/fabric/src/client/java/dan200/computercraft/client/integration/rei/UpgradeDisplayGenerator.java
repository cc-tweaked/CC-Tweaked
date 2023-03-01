// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration.rei;

import dan200.computercraft.shared.integration.UpgradeRecipeGenerator;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay;

import java.util.List;
import java.util.Optional;

/**
 * Provides custom recipe and usage hints for pocket/turtle upgrades.
 */
class UpgradeDisplayGenerator implements DynamicDisplayGenerator<DefaultCraftingDisplay<?>> {
    private final UpgradeRecipeGenerator<DefaultCraftingDisplay<?>> resolver = new UpgradeRecipeGenerator<>(DefaultCraftingDisplay::of);

    @Override
    public Optional<List<DefaultCraftingDisplay<?>>> getRecipeFor(EntryStack<?> entry) {
        return entry.getType() == VanillaEntryTypes.ITEM ? Optional.of(resolver.findRecipesWithOutput(entry.castValue())) : Optional.empty();
    }

    @Override
    public Optional<List<DefaultCraftingDisplay<?>>> getUsageFor(EntryStack<?> entry) {
        return entry.getType() == VanillaEntryTypes.ITEM ? Optional.of(resolver.findRecipesWithInput(entry.castValue())) : Optional.empty();
    }
}
