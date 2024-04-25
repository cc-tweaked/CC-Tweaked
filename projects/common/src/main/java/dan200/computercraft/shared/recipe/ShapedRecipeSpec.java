// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A description of a {@link ShapedRecipe}.
 * <p>
 * This is meant to be used in conjunction with {@link CustomShapedRecipe} for more reusable serialisation and
 * deserialisation of {@link ShapedRecipe}-like recipes.
 *
 * @param properties The common properties of this recipe.
 * @param pattern    The shaped template of the recipe.
 * @param result     The result of the recipe.
 */
public record ShapedRecipeSpec(RecipeProperties properties, ShapedRecipePattern pattern, ItemStack result) {
    public static final MapCodec<ShapedRecipeSpec> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        RecipeProperties.CODEC.forGetter(ShapedRecipeSpec::properties),
        ShapedRecipePattern.MAP_CODEC.forGetter(ShapedRecipeSpec::pattern),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(ShapedRecipeSpec::result)
    ).apply(instance, ShapedRecipeSpec::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShapedRecipeSpec> STREAM_CODEC = StreamCodec.composite(
        RecipeProperties.STREAM_CODEC, ShapedRecipeSpec::properties,
        ShapedRecipePattern.STREAM_CODEC, ShapedRecipeSpec::pattern,
        ItemStack.STREAM_CODEC, ShapedRecipeSpec::result,
        ShapedRecipeSpec::new
    );
}
