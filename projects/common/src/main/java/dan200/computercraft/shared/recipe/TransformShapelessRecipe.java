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
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;

/**
 * A {@link ShapelessRecipe} that applies a list of {@linkplain RecipeFunction recipe functions}.
 */
public class TransformShapelessRecipe extends CustomShapelessRecipe {
    public static final MapCodec<TransformShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ShapelessRecipeSpec.CODEC.forGetter(TransformShapelessRecipe::toSpec),
        RecipeFunction.LIST_CODEC.fieldOf("function").forGetter(x -> x.functions)
    ).apply(instance, TransformShapelessRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
        ShapelessRecipeSpec.STREAM_CODEC, CustomShapelessRecipe::toSpec,
        RecipeFunction.LIST_STREAM_CODEC, x -> x.functions,
        TransformShapelessRecipe::new
    );

    private final List<RecipeFunction> functions;

    public TransformShapelessRecipe(ShapelessRecipeSpec spec, List<RecipeFunction> functions) {
        super(spec);
        this.functions = functions;
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, HolderLookup.Provider registryAccess) {
        var result = super.assemble(inventory, registryAccess);
        for (var function : functions) result = function.apply(inventory, result);
        return result;
    }

    @Override
    public RecipeSerializer<TransformShapelessRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.TRANSFORM_SHAPELESS.get();
    }
}
