// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A fake {@link ShapedRecipe}, which appears in the recipe book (and other recipe mods), but cannot be crafted.
 * <p>
 * This is used to represent examples for our {@link CustomRecipe}s.
 */
public final class ImpostorShapedRecipe extends CustomShapedRecipe {
    public static final MapCodec<ImpostorShapedRecipe> CODEC = ShapedRecipeSpec.CODEC.xmap(ImpostorShapedRecipe::new, CustomShapedRecipe::toSpec);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImpostorShapedRecipe> STREAM_CODEC = ShapedRecipeSpec.STREAM_CODEC.map(ImpostorShapedRecipe::new, CustomShapedRecipe::toSpec);
    public static final RecipeSerializer<ImpostorShapedRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    public ImpostorShapedRecipe(ShapedRecipeSpec recipe) {
        super(recipe);
    }

    @Override
    public boolean matches(CraftingInput inv, Level world) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inventory) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<ImpostorShapedRecipe> getSerializer() {
        return SERIALIZER;
    }
}
