// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

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

    public static NonNullList<Ingredient> readShapelessIngredients(JsonObject json) {
        NonNullList<Ingredient> ingredients = NonNullList.create();

        var ingredientsList = GsonHelper.getAsJsonArray(json, "ingredients");
        for (var i = 0; i < ingredientsList.size(); ++i) {
            var ingredient = Ingredient.fromJson(ingredientsList.get(i));
            if (!ingredient.isEmpty()) ingredients.add(ingredient);
        }

        if (ingredients.isEmpty()) throw new JsonParseException("No ingredients for shapeless recipe");
        if (ingredients.size() > 9) {
            throw new JsonParseException("Too many ingredients for shapeless recipe the max is 9");
        }

        return ingredients;
    }

    /**
     * Extends {@link ShapedRecipe#itemStackFromJson(JsonObject)} with support for the {@code nbt} field.
     *
     * @param json The json to extract the item from.
     * @return The parsed item stack.
     */
    public static ItemStack itemStackFromJson(JsonObject json) {
        var item = ShapedRecipe.itemFromJson(json);
        if (json.has("data")) throw new JsonParseException("Disallowed data tag found");

        var count = GsonHelper.getAsInt(json, "count", 1);
        if (count < 1) throw new JsonSyntaxException("Invalid output count: " + count);

        var stack = new ItemStack(item, count);

        var nbt = json.get("nbt");
        if (nbt != null) {
            stack.setTag(Util.getOrThrow(MoreCodecs.TAG.parse(JsonOps.INSTANCE, nbt), JsonParseException::new));
        }
        return stack;
    }

}
