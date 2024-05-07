// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dan200.computercraft.shared.util.RegistryHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Function;

/**
 * A basic {@link RecipeSerializer} implementation.
 *
 * @param codec       The codec to read/write the recipe from JSON.
 * @param streamCodec The codec to read/write the recipe from a network stream.
 * @param <T>         The type of recipe to serialise.
 */
public record BasicRecipeSerialiser<T extends Recipe<?>>(
    MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec
) implements RecipeSerializer<T> {
    public BasicRecipeSerialiser(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        this.codec = codec.flatXmap(this::check, DataResult::success);
        this.streamCodec = streamCodec.map(x -> check(x).getOrThrow(), Function.identity());
    }

    private DataResult<T> check(T recipe) {
        if (recipe.getSerializer() == this) return DataResult.success(recipe);

        return DataResult.error(() ->
            "Expected serialiser to be " + RegistryHelper.getKeyOrThrow(BuiltInRegistries.RECIPE_SERIALIZER, this)
                + ", but was " + RegistryHelper.getKeyOrThrow(BuiltInRegistries.RECIPE_SERIALIZER, recipe.getSerializer())
        );
    }
}
