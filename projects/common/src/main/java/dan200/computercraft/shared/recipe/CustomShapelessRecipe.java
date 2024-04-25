// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.function.Function;

/**
 * A custom version of {@link ShapelessRecipe}, which can be converted to and from a {@link ShapelessRecipeSpec}.
 */
public abstract class CustomShapelessRecipe extends ShapelessRecipe {
    private final ItemStack result;
    private final boolean showNotification;

    protected CustomShapelessRecipe(ShapelessRecipeSpec recipe) {
        super(recipe.properties().group(), recipe.properties().category(), recipe.result(), recipe.ingredients());
        this.result = recipe.result();
        this.showNotification = recipe.properties().showNotification();
    }

    public final ShapelessRecipeSpec toSpec() {
        return new ShapelessRecipeSpec(RecipeProperties.of(this), getIngredients(), result);
    }

    @Override
    public final boolean showNotification() {
        return showNotification;
    }

    @Override
    public abstract RecipeSerializer<? extends CustomShapelessRecipe> getSerializer();

    public static <T extends CustomShapelessRecipe> RecipeSerializer<T> serialiser(Function<ShapelessRecipeSpec, T> factory) {
        return new BasicRecipeSerialiser<>(
            ShapelessRecipeSpec.CODEC.xmap(factory, CustomShapelessRecipe::toSpec),
            ShapelessRecipeSpec.STREAM_CODEC.map(factory, CustomShapelessRecipe::toSpec)
        );
    }
}
