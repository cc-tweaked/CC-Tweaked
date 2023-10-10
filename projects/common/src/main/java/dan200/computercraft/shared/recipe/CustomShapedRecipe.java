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
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * A custom version of {@link ShapedRecipe}, which can be converted to and from a {@link ShapedRecipeSpec}.
 * <p>
 * This recipe may both be used as a normal recipe (behaving mostly the same as {@link ShapedRecipe}, with
 * {@linkplain RecipeUtil#itemStackFromJson(JsonObject) support for putting nbt on the result}), or subclassed to
 * customise the crafting behaviour.
 */
public class CustomShapedRecipe extends ShapedRecipe {
    private final ItemStack result;

    public CustomShapedRecipe(ResourceLocation id, ShapedRecipeSpec recipe) {
        super(
            id,
            recipe.properties().group(), recipe.properties().category(),
            recipe.template().width(), recipe.template().height(), recipe.template().ingredients(),
            recipe.result()
        );
        this.result = recipe.result();
    }

    public final ShapedRecipeSpec toSpec() {
        return new ShapedRecipeSpec(RecipeProperties.of(this), ShapedTemplate.of(this), result);
    }

    @Override
    public RecipeSerializer<? extends CustomShapedRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.SHAPED.get();
    }

    public interface Factory<R> {
        R create(ResourceLocation id, ShapedRecipeSpec recipe);
    }

    public static <T extends CustomShapedRecipe> RecipeSerializer<T> serialiser(CustomShapedRecipe.Factory<T> factory) {
        return new Serialiser<>((id, r) -> DataResult.success(factory.create(id, r)));
    }

    public static <T extends CustomShapedRecipe> RecipeSerializer<T> validatingSerialiser(CustomShapedRecipe.Factory<DataResult<T>> factory) {
        return new Serialiser<>(factory);
    }

    private record Serialiser<T extends CustomShapedRecipe>(
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
            return Util.getOrThrow(factory.create(id, ShapedRecipeSpec.fromJson(json)), JsonParseException::new);
        }

        @Override
        public T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return Util.getOrThrow(factory.create(id, ShapedRecipeSpec.fromNetwork(buffer)), IllegalStateException::new);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, T recipe) {
            recipe.toSpec().toNetwork(buffer);
        }
    }
}
