// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import dan200.computercraft.test.shared.MinecraftArbitraries;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * {@link Arbitrary} implementations for recipes.
 */
public final class RecipeArbitraries {
    public static Arbitrary<RecipeProperties> recipeProperties() {
        return Combinators.combine(
            Arbitraries.strings().ofMinLength(1).withChars("abcdefghijklmnopqrstuvwxyz_"),
            Arbitraries.of(CraftingBookCategory.values())
        ).as(RecipeProperties::new);
    }

    public static Arbitrary<ShapelessRecipeSpec> shapelessRecipeSpec() {
        return Combinators.combine(
            recipeProperties(),
            MinecraftArbitraries.ingredient().array(Ingredient[].class).ofMinSize(1).map(x -> NonNullList.of(Ingredient.EMPTY, x)),
            MinecraftArbitraries.nonEmptyItemStack()
        ).as(ShapelessRecipeSpec::new);
    }

    public static Arbitrary<ShapedTemplate> shapedTemplate() {
        return Combinators.combine(Arbitraries.integers().between(1, 3), Arbitraries.integers().between(1, 3))
            .as(IntIntImmutablePair::new)
            .flatMap(x -> MinecraftArbitraries.ingredient().array(Ingredient[].class).ofSize(x.leftInt() * x.rightInt())
                .map(i -> new ShapedTemplate(x.leftInt(), x.rightInt(), NonNullList.of(Ingredient.EMPTY, i)))
            );
    }

    public static Arbitrary<ShapedRecipeSpec> shapedRecipeSpec() {
        return Combinators.combine(
            recipeProperties(),
            shapedTemplate(),
            MinecraftArbitraries.nonEmptyItemStack()
        ).as(ShapedRecipeSpec::new);
    }
}
