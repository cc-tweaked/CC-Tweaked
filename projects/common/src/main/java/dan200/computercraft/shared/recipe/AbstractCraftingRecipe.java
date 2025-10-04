// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;

/**
 * An abstract {@link CraftingRecipe} that provides a skeleton implementation.
 */
public abstract class AbstractCraftingRecipe implements CraftingRecipe {
    protected final RecipeProperties properties;

    protected AbstractCraftingRecipe(RecipeProperties properties) {
        this.properties = properties;
    }

    @Override
    public final boolean showNotification() {
        return properties.showNotification();
    }

    @Override
    public final String group() {
        return properties.group();
    }

    @Override
    public final CraftingBookCategory category() {
        return properties.category();
    }
}
