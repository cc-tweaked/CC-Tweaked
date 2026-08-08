// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.integration;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.items.TurtleItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
    public static boolean shouldRemoveRecipe(Identifier id) {
        if (!id.getNamespace().equals(ComputerCraftAPI.MOD_ID)) return false;

        var path = id.getPath();
        return path.startsWith("turtle_normal/") || path.startsWith("turtle_advanced/")
            || path.startsWith("pocket_normal/") || path.startsWith("pocket_advanced/");
    }

    static <T> void forEachRegistry(HolderLookup.Provider registries, ResourceKey<Registry<T>> registry, Consumer<Holder.Reference<T>> consumer) {
        registries.lookup(registry).map(HolderLookup::listElements).orElse(Stream.empty()).forEach(consumer);
    }
}
