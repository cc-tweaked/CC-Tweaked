// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import dan200.computercraft.test.core.StructuralEquality;
import dan200.computercraft.test.shared.MinecraftEqualities;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * {@link StructuralEquality} implementations for recipes.
 */
public final class RecipeEqualities {
    private RecipeEqualities() {
    }

    public static final StructuralEquality<ShapelessRecipeSpec> shapelessRecipeSpec = StructuralEquality.all(
        StructuralEquality.at("properties", ShapelessRecipeSpec::properties),
        StructuralEquality.at("ingredients", ShapelessRecipeSpec::ingredients, MinecraftEqualities.ingredient.list()),
        StructuralEquality.at("result", ShapelessRecipeSpec::result, MinecraftEqualities.itemStack)
    );

    public static final StructuralEquality<ShapedRecipePattern> shapedPattern = StructuralEquality.all(
        StructuralEquality.at("width", ShapedRecipePattern::width),
        StructuralEquality.at("height", ShapedRecipePattern::height),
        StructuralEquality.at("ingredients", ShapedRecipePattern::ingredients, MinecraftEqualities.ingredient.list())
    );

    public static final StructuralEquality<ShapedRecipeSpec> shapedRecipeSpec = StructuralEquality.all(
        StructuralEquality.at("properties", ShapedRecipeSpec::properties),
        StructuralEquality.at("ingredients", ShapedRecipeSpec::pattern, shapedPattern),
        StructuralEquality.at("result", ShapedRecipeSpec::result, MinecraftEqualities.itemStack)
    );
}
