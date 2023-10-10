// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DataResult;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A custom version of {@link ShapelessRecipe}, which can be converted to and from a {@link ShapelessRecipeSpec}.
 * <p>
 * This recipe may both be used as a normal recipe (behaving mostly the same as {@link ShapelessRecipe}, with
 * {@linkplain RecipeUtil#itemStackFromJson(JsonObject) support for putting nbt on the result}), or subclassed to
 * customise the crafting behaviour.
 */
public class CustomShapelessRecipe extends ShapelessRecipe {
    private final ItemStack result;

    public CustomShapelessRecipe(ResourceLocation id, ShapelessRecipeSpec recipe) {
        super(id, recipe.properties().group(), recipe.properties().category(), recipe.result(), recipe.ingredients());
        this.result = recipe.result();
    }

    public final ShapelessRecipeSpec toSpec() {
        return new ShapelessRecipeSpec(RecipeProperties.of(this), getIngredients(), result);
    }

    @Override
    public RecipeSerializer<? extends CustomShapelessRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.SHAPELESS.get();
    }

    public interface Factory<R> {
        R create(ResourceLocation id, ShapelessRecipeSpec recipe);
    }

    public static <T extends CustomShapelessRecipe> RecipeSerializer<T> serialiser(Factory<T> factory) {
        return new CustomShapelessRecipe.Serialiser<>((id, r) -> DataResult.success(factory.create(id, r)));
    }

    public static <T extends CustomShapelessRecipe> RecipeSerializer<T> validatingSerialiser(Factory<DataResult<T>> factory) {
        return new CustomShapelessRecipe.Serialiser<>(factory);
    }

    private record Serialiser<T extends CustomShapelessRecipe>(
        Factory<DataResult<T>> factory
    ) implements RecipeSerializer<T> {
        private Serialiser(Factory<DataResult<T>> factory) {
            this.factory = (id, r) -> factory.create(id, r).flatMap(x -> {
                if (x.getSerializer() != this) {
                    return DataResult.error(() -> "Expected serialiser to be " + this + ", but was " + x.getSerializer());
                }
                return DataResult.success(x);
            });
        }

        @Override
        public T fromJson(ResourceLocation id, JsonObject json) {
            return Util.getOrThrow(factory.create(id, ShapelessRecipeSpec.fromJson(json)), JsonParseException::new);
        }

        @Override
        public T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return Util.getOrThrow(factory.create(id, ShapelessRecipeSpec.fromNetwork(buffer)), IllegalStateException::new);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, T recipe) {
            recipe.toSpec().toNetwork(buffer);
        }
    }
}
