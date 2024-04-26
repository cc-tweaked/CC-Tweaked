// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.recipe.function.RecipeFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.List;

/**
 * A {@link ShapedRecipe} that applies a list of {@linkplain RecipeFunction recipe functions}.
 */
public final class TransformShapedRecipe extends CustomShapedRecipe {
    public static final MapCodec<TransformShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ShapedRecipeSpec.CODEC.forGetter(TransformShapedRecipe::toSpec),
            RecipeFunction.LIST_CODEC.fieldOf("function").forGetter(x -> x.functions)
        ).apply(instance, TransformShapedRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformShapedRecipe> STREAM_CODEC = StreamCodec.composite(
        ShapedRecipeSpec.STREAM_CODEC, TransformShapedRecipe::toSpec,
        RecipeFunction.LIST_STREAM_CODEC, x -> x.functions,
        TransformShapedRecipe::new
    );

    private final List<RecipeFunction> functions;

    public TransformShapedRecipe(ShapedRecipeSpec recipe, List<RecipeFunction> functions) {
        super(recipe);
        this.functions = functions;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, HolderLookup.Provider registryAccess) {
        var result = super.assemble(inventory, registryAccess);
        for (var function : functions) result = function.apply(inventory, result);
        return result;
    }

    @Override
    public RecipeSerializer<TransformShapedRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.TRANSFORM_SHAPED.get();
    }
}
