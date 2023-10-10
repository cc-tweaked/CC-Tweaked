// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The template for {@linkplain ShapedRecipe shaped recipes}. This largely exists for parsing shaped recipes from JSON.
 *
 * @param width       The width of the recipe, see {@link ShapedRecipe#getWidth()}.
 * @param height      The height of the recipe, see {@link ShapedRecipe#getHeight()}.
 * @param ingredients The ingredients in the recipe, see {@link ShapedRecipe#getIngredients()}
 */
public record ShapedTemplate(int width, int height, NonNullList<Ingredient> ingredients) {
    public static ShapedTemplate of(ShapedRecipe recipe) {
        return new ShapedTemplate(recipe.getWidth(), recipe.getHeight(), recipe.getIngredients());
    }

    public static ShapedTemplate fromJson(JsonObject json) {
        Map<Character, Ingredient> key = new HashMap<>();
        for (var entry : GsonHelper.getAsJsonObject(json, "key").entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }
            if (" ".equals(entry.getKey())) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }

            key.put(entry.getKey().charAt(0), Ingredient.fromJson(entry.getValue()));
        }

        var patternList = GsonHelper.getAsJsonArray(json, "pattern");
        if (patternList.size() == 0) {
            throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        }

        var pattern = new String[patternList.size()];
        for (var x = 0; x < pattern.length; x++) {
            var line = GsonHelper.convertToString(patternList.get(x), "pattern[" + x + "]");
            if (x > 0 && pattern[0].length() != line.length()) {
                throw new JsonSyntaxException("Invalid pattern: each row must  be the same width");
            }
            pattern[x] = line;
        }

        var width = pattern[0].length();
        var height = pattern.length;
        var ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

        Set<Character> missingKeys = new HashSet<>(key.keySet());

        var ingredientIdx = 0;
        for (var line : pattern) {
            for (var x = 0; x < line.length(); x++) {
                var chr = line.charAt(x);
                var ing = chr == ' ' ? Ingredient.EMPTY : key.get(chr);
                if (ing == null) {
                    throw new JsonSyntaxException("Pattern references symbol '" + chr + "' but it's not defined in the key");
                }
                ingredients.set(ingredientIdx++, ing);
                missingKeys.remove(chr);
            }
        }

        if (!missingKeys.isEmpty()) {
            throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + missingKeys);
        }

        return new ShapedTemplate(width, height, ingredients);
    }

    public static ShapedTemplate fromNetwork(FriendlyByteBuf buffer) {
        var width = buffer.readVarInt();
        var height = buffer.readVarInt();
        var ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (var i = 0; i < ingredients.size(); ++i) ingredients.set(i, Ingredient.fromNetwork(buffer));
        return new ShapedTemplate(width, height, ingredients);
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeVarInt(width());
        buffer.writeVarInt(height());
        for (var ingredient : ingredients) ingredient.toNetwork(buffer);
    }

}
