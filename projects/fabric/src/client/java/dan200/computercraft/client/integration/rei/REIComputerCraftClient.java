// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration.rei;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.integration.RecipeModHelpers;
import dan200.computercraft.shared.platform.RegistryEntry;
import dev.architectury.event.EventResult;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.CollapsibleEntryRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side REI integration for ComputerCraft.
 * <p>
 * This is Fabric-only for now - getting the common jar working outside of architectury is awkward.
 */
public class REIComputerCraftClient implements REIClientPlugin {
    @Override
    public void registerEntries(EntryRegistry registry) {
        for (var stack : RecipeModHelpers.getExtraStacks(BasicDisplay.registryAccess())) {
            registry.addEntry(EntryStacks.of(stack));
        }
    }

    @Override
    public void registerCollapsibleEntries(CollapsibleEntryRegistry registry) {
        addCollapsableGroup(registry, ModRegistry.Items.TURTLE_NORMAL);
        addCollapsableGroup(registry, ModRegistry.Items.TURTLE_ADVANCED);
        addCollapsableGroup(registry, ModRegistry.Items.POCKET_COMPUTER_NORMAL);
        addCollapsableGroup(registry, ModRegistry.Items.POCKET_COMPUTER_ADVANCED);
    }

    private static void addCollapsableGroup(CollapsibleEntryRegistry registry, RegistryEntry<? extends Item> holder) {
        var item = holder.get();
        registry.group(holder.id(), new ItemStack(item).getDisplayName(), VanillaEntryTypes.ITEM, x -> x.getValue().is(item));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.registerDisplayGenerator(BuiltinPlugin.CRAFTING, new UpgradeDisplayGenerator());

        // Hide all upgrade recipes
        registry.registerVisibilityPredicate((category, display) ->
            display.getDisplayLocation().map(RecipeModHelpers::shouldRemoveRecipe).orElse(false)
                ? EventResult.interruptFalse() : EventResult.pass());
    }
}
