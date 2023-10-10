// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;

/**
 * Common properties that appear in all {@link CraftingRecipe}s.
 *
 * @param group    The (optional) group of the recipe, see {@link CraftingRecipe#getGroup()}.
 * @param category The category the recipe appears in, see {@link CraftingRecipe#category()}.
 */
public record RecipeProperties(String group, CraftingBookCategory category) {
    public static RecipeProperties of(CraftingRecipe recipe) {
        return new RecipeProperties(recipe.getGroup(), recipe.category());
    }

    public static RecipeProperties fromJson(JsonObject json) {
        var group = GsonHelper.getAsString(json, "group", "");
        var category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
        return new RecipeProperties(group, category);
    }

    public static RecipeProperties fromNetwork(FriendlyByteBuf buffer) {
        var group = buffer.readUtf();
        var category = buffer.readEnum(CraftingBookCategory.class);
        return new RecipeProperties(group, category);
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeUtf(group());
        buffer.writeEnum(category());
    }
}
