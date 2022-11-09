/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.integration;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utilities for recipe mod plugins (such as JEI).
 */
public final class RecipeModHelpers {
    static final List<ComputerFamily> MAIN_FAMILIES = Arrays.asList(ComputerFamily.NORMAL, ComputerFamily.ADVANCED);

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
        for (var family : MAIN_FAMILIES) {
            for (var upgrade : TurtleUpgrades.instance().getUpgrades()) {
                upgradeItems.add(TurtleItemFactory.create(-1, null, -1, family, null, upgrade, 0, null));
            }

            for (var upgrade : PocketUpgrades.instance().getUpgrades()) {
                upgradeItems.add(PocketComputerItemFactory.create(-1, null, -1, family, upgrade));
            }
        }

        return upgradeItems;
    }
}
