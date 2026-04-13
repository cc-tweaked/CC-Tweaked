// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Common properties that appear in all {@link CraftingRecipe}s.
 *
 * @param book   Book-related properties about crafting recipes.
 * @param common Common properties about all recipes.
 */
public record RecipeProperties(CraftingRecipe.CraftingBookInfo book, Recipe.CommonInfo common) {
    public static final MapCodec<RecipeProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(RecipeProperties::book),
        Recipe.CommonInfo.MAP_CODEC.forGetter(RecipeProperties::common)
    ).apply(instance, RecipeProperties::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeProperties> STREAM_CODEC = StreamCodec.composite(
        CraftingRecipe.CraftingBookInfo.STREAM_CODEC, RecipeProperties::book,
        Recipe.CommonInfo.STREAM_CODEC, RecipeProperties::common,
        RecipeProperties::new
    );

    public static RecipeProperties of(CraftingRecipe recipe) {
        return new RecipeProperties(
            new CraftingRecipe.CraftingBookInfo(recipe.category(), recipe.group()),
            new Recipe.CommonInfo(recipe.showNotification())
        );
    }
}
