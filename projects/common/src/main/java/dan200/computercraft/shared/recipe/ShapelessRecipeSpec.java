// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A description of a {@link ShapelessRecipe}.
 * <p>
 * This is meant to be used in conjunction with {@link CustomShapelessRecipe} for more reusable serialisation and
 * deserialisation of {@link ShapelessRecipe}-like recipes.
 *
 * @param properties  The common properties of this recipe.
 * @param ingredients The ingredients of the recipe.
 * @param result      The result of the recipe.
 */
public record ShapelessRecipeSpec(RecipeProperties properties, NonNullList<Ingredient> ingredients, ItemStack result) {
    public static final MapCodec<ShapelessRecipeSpec> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RecipeProperties.CODEC.forGetter(ShapelessRecipeSpec::properties),
        MoreCodecs.SHAPELESS_INGREDIENTS.fieldOf("ingredients").forGetter(ShapelessRecipeSpec::ingredients),
        MoreCodecs.ITEM_STACK_WITH_NBT.fieldOf("result").forGetter(ShapelessRecipeSpec::result)
    ).apply(instance, ShapelessRecipeSpec::new));

    public static ShapelessRecipeSpec fromNetwork(FriendlyByteBuf buffer) {
        var properties = RecipeProperties.fromNetwork(buffer);
        var ingredients = RecipeUtil.readIngredients(buffer);
        var result = buffer.readItem();

        return new ShapelessRecipeSpec(properties, ingredients, result);
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        properties().toNetwork(buffer);
        RecipeUtil.writeIngredients(buffer, ingredients());
        buffer.writeItem(result());
    }
}
