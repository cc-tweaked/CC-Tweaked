// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.core.NonNullList;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Map;
import java.util.Set;

// TODO: Replace some things with Forge??

public final class RecipeUtil {
    private RecipeUtil() {
    }

    public record ShapedTemplate(int width, int height, NonNullList<Ingredient> ingredients) {
    }

    public static ShapedTemplate getTemplate(JsonObject json) {
        Map<Character, Ingredient> ingMap = Maps.newHashMap();
        for (var entry : GsonHelper.getAsJsonObject(json, "key").entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }
            if (" ".equals(entry.getKey())) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }

            ingMap.put(entry.getKey().charAt(0), Ingredient.fromJson(entry.getValue()));
        }

        ingMap.put(' ', Ingredient.EMPTY);

        var patternJ = GsonHelper.getAsJsonArray(json, "pattern");

        if (patternJ.size() == 0) {
            throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        }

        var pattern = new String[patternJ.size()];
        for (var x = 0; x < pattern.length; x++) {
            var line = GsonHelper.convertToString(patternJ.get(x), "pattern[" + x + "]");
            if (x > 0 && pattern[0].length() != line.length()) {
                throw new JsonSyntaxException("Invalid pattern: each row must  be the same width");
            }
            pattern[x] = line;
        }

        var width = pattern[0].length();
        var height = pattern.length;
        var ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

        Set<Character> missingKeys = Sets.newHashSet(ingMap.keySet());
        missingKeys.remove(' ');

        var ingredientIdx = 0;
        for (var line : pattern) {
            for (var i = 0; i < line.length(); i++) {
                var chr = line.charAt(i);

                var ing = ingMap.get(chr);
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

    public static ComputerFamily getFamily(JsonObject json, String name) {
        var familyName = GsonHelper.getAsString(json, name);
        for (var family : ComputerFamily.values()) {
            if (family.name().equalsIgnoreCase(familyName)) return family;
        }

        throw new JsonSyntaxException("Unknown computer family '" + familyName + "' for field " + name);
    }
}
