// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dan200.computercraft.impl.RegistryHelper;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.function.Function;

/**
 * A custom version of {@link ShapelessRecipe}, which can be converted to and from a {@link ShapelessRecipeSpec}.
 * <p>
 * This recipe may both be used as a normal recipe (behaving mostly the same as {@link ShapelessRecipe}, with
 * {@linkplain MoreCodecs#ITEM_STACK_WITH_NBT support for putting nbt on the result}), or subclassed to
 * customise the crafting behaviour.
 */
public class CustomShapelessRecipe extends ShapelessRecipe {
    private final ItemStack result;
    private final boolean showNotification;

    public CustomShapelessRecipe(ShapelessRecipeSpec recipe) {
        super(recipe.properties().group(), recipe.properties().category(), recipe.result(), recipe.ingredients());
        this.result = recipe.result();
        this.showNotification = recipe.properties().showNotification();
    }

    public final ShapelessRecipeSpec toSpec() {
        return new ShapelessRecipeSpec(RecipeProperties.of(this), getIngredients(), result);
    }

    @Override
    public final boolean showNotification() {
        return showNotification;
    }

    @Override
    public RecipeSerializer<? extends CustomShapelessRecipe> getSerializer() {
        return ModRegistry.RecipeSerializers.SHAPELESS.get();
    }

    public static <T extends CustomShapelessRecipe> RecipeSerializer<T> serialiser(Function<ShapelessRecipeSpec, T> factory) {
        return new CustomShapelessRecipe.Serialiser<>(r -> DataResult.success(factory.apply(r)));
    }

    public static <T extends CustomShapelessRecipe> RecipeSerializer<T> validatingSerialiser(Function<ShapelessRecipeSpec, DataResult<T>> factory) {
        return new CustomShapelessRecipe.Serialiser<>(factory);
    }

    private static final class Serialiser<T extends CustomShapelessRecipe> implements RecipeSerializer<T> {
        private final Function<ShapelessRecipeSpec, DataResult<T>> factory;
        private final Codec<T> codec;

        private Serialiser(Function<ShapelessRecipeSpec, DataResult<T>> factory) {
            this.factory = r -> factory.apply(r).flatMap(x -> {
                if (x.getSerializer() != this) {
                    return DataResult.error(() -> "Expected serialiser to be " + RegistryHelper.getKeyOrThrow(BuiltInRegistries.RECIPE_SERIALIZER, this)
                        + ", but was " + RegistryHelper.getKeyOrThrow(BuiltInRegistries.RECIPE_SERIALIZER, x.getSerializer()));
                }
                return DataResult.success(x);
            });
            this.codec = ShapelessRecipeSpec.CODEC.flatXmap(factory, x -> DataResult.success(x.toSpec())).codec();
        }

        @Override
        public Codec<T> codec() {
            return codec;
        }

        @Override
        public T fromNetwork(FriendlyByteBuf buffer) {
            return Util.getOrThrow(factory.apply(ShapelessRecipeSpec.fromNetwork(buffer)), IllegalStateException::new);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, T recipe) {
            recipe.toSpec().toNetwork(buffer);
        }
    }
}
