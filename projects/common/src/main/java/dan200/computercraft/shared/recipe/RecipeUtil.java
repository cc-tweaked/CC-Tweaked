// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Ingredient;

public final class RecipeUtil {
    private RecipeUtil() {
    }

    public static NonNullList<Ingredient> readIngredients(FriendlyByteBuf buffer) {
        var count = buffer.readVarInt();
        var ingredients = NonNullList.withSize(count, Ingredient.EMPTY);
        for (var i = 0; i < ingredients.size(); i++) ingredients.set(i, Ingredient.fromNetwork(buffer));
        return ingredients;
    }

    public static void writeIngredients(FriendlyByteBuf buffer, NonNullList<Ingredient> ingredients) {
        buffer.writeCollection(ingredients, (a, b) -> b.toNetwork(a));
    }
}
