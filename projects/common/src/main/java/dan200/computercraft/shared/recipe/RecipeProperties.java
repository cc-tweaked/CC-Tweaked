// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;

/**
 * Common properties that appear in all {@link CraftingRecipe}s.
 *
 * @param group            The (optional) group of the recipe, see {@link CraftingRecipe#getGroup()}.
 * @param category         The category the recipe appears in, see {@link CraftingRecipe#category()}.
 * @param showNotification Show notifications when the recipe is unlocked, see {@link CraftingRecipe#showNotification()}.
 */
public record RecipeProperties(String group, CraftingBookCategory category, boolean showNotification) {
    public static final MapCodec<RecipeProperties> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.optionalFieldOf("group", "").forGetter(RecipeProperties::group),
        CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(RecipeProperties::category),
        Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(RecipeProperties::showNotification)
    ).apply(instance, RecipeProperties::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipeProperties> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, RecipeProperties::group,
        CraftingBookCategory.STREAM_CODEC, RecipeProperties::category,
        ByteBufCodecs.BOOL, RecipeProperties::showNotification,
        RecipeProperties::new
    );

    public static RecipeProperties of(CraftingRecipe recipe) {
        return new RecipeProperties(recipe.getGroup(), recipe.category(), recipe.showNotification());
    }
}
