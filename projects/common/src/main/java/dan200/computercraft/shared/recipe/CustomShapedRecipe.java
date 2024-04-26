// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.function.Function;

/**
 * A custom version of {@link ShapedRecipe}, which can be converted to and from a {@link ShapedRecipeSpec}.
 */
public abstract class CustomShapedRecipe extends ShapedRecipe {
    private final ShapedRecipePattern pattern;
    private final ItemStack result;

    public CustomShapedRecipe(ShapedRecipeSpec recipe) {
        super(recipe.properties().group(), recipe.properties().category(), recipe.pattern(), recipe.result(), recipe.properties().showNotification());
        this.pattern = recipe.pattern();
        this.result = recipe.result();
    }

    public final ShapedRecipeSpec toSpec() {
        return new ShapedRecipeSpec(RecipeProperties.of(this), pattern, result);
    }

    @Override
    public abstract RecipeSerializer<? extends CustomShapedRecipe> getSerializer();

    public static <T extends CustomShapedRecipe> RecipeSerializer<T> serialiser(Function<ShapedRecipeSpec, T> factory) {
        return new BasicRecipeSerialiser<>(
            ShapedRecipeSpec.CODEC.xmap(factory, CustomShapedRecipe::toSpec),
            ShapedRecipeSpec.STREAM_CODEC.map(factory, CustomShapedRecipe::toSpec)
        );
    }
}
