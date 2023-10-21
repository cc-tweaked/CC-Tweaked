// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Utilities for recipe mod plugins (such as JEI).
 */
public final class RecipeModHelpers {
    static final List<Supplier<TurtleItem>> TURTLES = List.of(ModRegistry.Items.TURTLE_NORMAL, ModRegistry.Items.TURTLE_ADVANCED);
    static final List<Supplier<PocketComputerItem>> POCKET_COMPUTERS = List.of(ModRegistry.Items.POCKET_COMPUTER_NORMAL, ModRegistry.Items.POCKET_COMPUTER_ADVANCED);

    private RecipeModHelpers() {
    }

    /**
     * Determine if a recipe should be hidden. This should be used in conjunction with {@link UpgradeRecipeGenerator}
     * to hide our upgrade crafting recipes.
     *
     * @param id The recipe ID.
     * @return Whether it should be removed.
     */
    public static boolean shouldRemoveRecipe(ResourceLocation id) {
        if (!id.getNamespace().equals(ComputerCraftAPI.MOD_ID)) return false;

        var path = id.getPath();
        return path.startsWith("turtle_normal/") || path.startsWith("turtle_advanced/")
            || path.startsWith("pocket_normal/") || path.startsWith("pocket_advanced/");
    }

    /**
     * Get additional ComputerCraft-related items which may not be visible in a creative tab. This includes upgraded
     * turtle and pocket computers for each upgrade.
     *
     * @return The additional stacks to show.
     */
    public static List<ItemStack> getExtraStacks() {
        List<ItemStack> upgradeItems = new ArrayList<>();
        for (var turtleSupplier : TURTLES) {
            var turtle = turtleSupplier.get();
            for (var upgrade : TurtleUpgrades.instance().getUpgrades()) {
                upgradeItems.add(turtle.create(-1, null, -1, null, UpgradeData.ofDefault(upgrade), 0, null));
            }
        }

        for (var pocketSupplier : POCKET_COMPUTERS) {
            var pocket = pocketSupplier.get();
            for (var upgrade : PocketUpgrades.instance().getUpgrades()) {
                upgradeItems.add(pocket.create(-1, null, -1, UpgradeData.ofDefault(upgrade)));
            }
        }

        return upgradeItems;
    }
}
