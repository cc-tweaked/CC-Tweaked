// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import dan200.computercraft.test.core.StructuralEquality;
import dan200.computercraft.test.shared.MinecraftEqualities;

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

    public static final StructuralEquality<ShapedTemplate> shapedTemplate = StructuralEquality.all(
        StructuralEquality.at("width", ShapedTemplate::width),
        StructuralEquality.at("height", ShapedTemplate::height),
        StructuralEquality.at("ingredients", ShapedTemplate::ingredients, MinecraftEqualities.ingredient.list())
    );

    public static final StructuralEquality<ShapedRecipeSpec> shapedRecipeSpec = StructuralEquality.all(
        StructuralEquality.at("properties", ShapedRecipeSpec::properties),
        StructuralEquality.at("ingredients", ShapedRecipeSpec::template, shapedTemplate),
        StructuralEquality.at("result", ShapedRecipeSpec::result, MinecraftEqualities.itemStack)
    );
}
