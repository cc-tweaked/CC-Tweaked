// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
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
        MoreCodecs.ITEM_STACK_WITH_NBT.fieldOf("result").forGetter(ShapedRecipeSpec::result)
    ).apply(instance, ShapedRecipeSpec::new));

    public static ShapedRecipeSpec fromNetwork(FriendlyByteBuf buffer) {
        var properties = RecipeProperties.fromNetwork(buffer);
        var template = ShapedRecipePattern.fromNetwork(buffer);
        var result = buffer.readItem();
        return new ShapedRecipeSpec(properties, template, result);
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        properties().toNetwork(buffer);
        pattern().toNetwork(buffer);
        buffer.writeItem(result());
    }
}
