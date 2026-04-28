// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A custom version of {@link ShapedRecipe}, which can be converted to and from a {@link ShapedRecipeSpec}.
 * <p>
 * Ideally this wouldn't need to extend {@link ShapedRecipe}, but JEI still special-cases that class rather than relying
 * on {@link Recipe#display()}.
 */
public abstract class CustomShapedRecipe extends ShapedRecipe {
    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;

    public CustomShapedRecipe(ShapedRecipeSpec recipe) {
        super(recipe.properties().common(), recipe.properties().book(), recipe.pattern(), recipe.result());
        this.pattern = recipe.pattern();
        this.result = recipe.result();
    }

    public final ShapedRecipeSpec toSpec() {
        return new ShapedRecipeSpec(RecipeProperties.of(this), pattern, result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) getSerializer0();
    }

    protected abstract RecipeSerializer<? extends CustomShapedRecipe> getSerializer0();
}
