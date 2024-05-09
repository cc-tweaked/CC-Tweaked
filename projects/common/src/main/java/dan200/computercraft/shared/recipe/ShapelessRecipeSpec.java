// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.network.codec.MoreStreamCodecs;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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
    /**
     * A list of {@link Ingredient}s, usable in a {@linkplain ShapelessRecipe shapeless recipe}.
     */
    private static final Codec<NonNullList<Ingredient>> INGREDIENT_CODEC = Ingredient.CODEC_NONEMPTY.listOf().flatXmap(list -> {
        var ingredients = list.stream().filter(ingredient -> !ingredient.isEmpty()).toArray(Ingredient[]::new);
        if (ingredients.length == 0) return DataResult.error(() -> "No ingredients for shapeless recipe");
        if (ingredients.length > 9) return DataResult.error(() -> "Too many ingredients for shapeless recipe");
        return DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
    }, DataResult::success);

    public static final MapCodec<ShapelessRecipeSpec> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RecipeProperties.CODEC.forGetter(ShapelessRecipeSpec::properties),
        INGREDIENT_CODEC.fieldOf("ingredients").forGetter(ShapelessRecipeSpec::ingredients),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ShapelessRecipeSpec::result)
    ).apply(instance, ShapelessRecipeSpec::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessRecipeSpec> STREAM_CODEC = StreamCodec.composite(
        RecipeProperties.STREAM_CODEC, ShapelessRecipeSpec::properties,
        MoreStreamCodecs.nonNullList(Ingredient.CONTENTS_STREAM_CODEC, Ingredient.EMPTY), ShapelessRecipeSpec::ingredients,
        ItemStack.STREAM_CODEC, ShapelessRecipeSpec::result,
        ShapelessRecipeSpec::new
    );

    /**
     * Create a basic {@link ShapelessRecipe} from this spec.
     *
     * @return The newly constructed recipe.
     */
    public ShapelessRecipe create() {
        return new ShapelessRecipe(properties().group(), properties().category(), result(), ingredients());
    }
}
