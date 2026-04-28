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
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/**
 * A fake {@link ShapelessRecipe}, which appears in the recipe book (and other recipe mods), but cannot be crafted.
 * <p>
 * This is used to represent examples for our {@link CustomRecipe}s.
 */
public final class ImpostorShapelessRecipe extends CustomShapelessRecipe {
    public static final MapCodec<ImpostorShapelessRecipe> CODEC = ShapelessRecipeSpec.CODEC.xmap(ImpostorShapelessRecipe::new, CustomShapelessRecipe::toSpec);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImpostorShapelessRecipe> STREAM_CODEC = ShapelessRecipeSpec.STREAM_CODEC.map(ImpostorShapelessRecipe::new, CustomShapelessRecipe::toSpec);
    public static final RecipeSerializer<ImpostorShapelessRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

    public ImpostorShapelessRecipe(ShapelessRecipeSpec recipe) {
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
    public RecipeSerializer<ImpostorShapelessRecipe> getSerializer() {
        return SERIALIZER;
    }
}
