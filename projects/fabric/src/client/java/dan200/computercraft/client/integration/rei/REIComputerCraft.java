// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.integration.rei;

import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.integration.RecipeModHelpers;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import dev.architectury.event.EventResult;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;

/**
 * REI integration for ComputerCraft.
 * <p>
 * This is Fabric-only for now - getting the common jar working outside of architectury is awkward.
 */
public class REIComputerCraft implements REIClientPlugin {
    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        registry.register((context, stack) -> {
            var turtle = (TurtleItem) stack.getItem();

            long hash = 1;

            var left = turtle.getUpgrade(stack, TurtleSide.LEFT);
            var right = turtle.getUpgrade(stack, TurtleSide.RIGHT);
            if (left != null) hash = hash * 31 + left.getUpgradeID().hashCode();
            if (right != null) hash = hash * 31 + right.getUpgradeID().hashCode();

            return hash;
        }, ModRegistry.Items.TURTLE_NORMAL.get(), ModRegistry.Items.TURTLE_ADVANCED.get());

        registry.register((context, stack) -> {
            var upgrade = PocketComputerItem.getUpgrade(stack);
            return upgrade == null ? 1 : upgrade.getUpgradeID().hashCode();
        }, ModRegistry.Items.POCKET_COMPUTER_NORMAL.get(), ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get());
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
