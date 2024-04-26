// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.impl.RegistryHelper;
import dan200.computercraft.shared.recipe.TransformShapedRecipe;
import dan200.computercraft.shared.recipe.TransformShapelessRecipe;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import java.util.List;

/**
 * A function that is applied to the result of a recipe, mutating it in some way. These can be used from within a recipe
 * JSON file to define basic dynamic recipes, rather than having to fall back to Java.
 * <p>
 * For instance, the recipe to convert a normal computer to a turtle, is defined as a basic shaped recipes plus an
 * additional {@link CopyComponents} function, that copies the id and label from the computer to the turtle.
 * <p>
 * The design and implementation of these are very similar to Minecraft's existing {@linkplain LootItemFunction loot
 * functions}.
 *
 * @see TransformShapedRecipe
 * @see TransformShapelessRecipe
 */
public interface RecipeFunction {
    /**
     * The registry where {@link RecipeFunction}s are registered.
     */
    ResourceKey<Registry<Type<?>>> REGISTRY = ResourceKey.createRegistryKey(new ResourceLocation(ComputerCraftAPI.MOD_ID, "recipe_function"));

    /**
     * The codec to read and write {@link RecipeFunction}s with.
     */
    Codec<RecipeFunction> CODEC = Codec.lazyInitialized(() -> RegistryHelper.getRegistry(REGISTRY).byNameCodec().dispatch(RecipeFunction::getType, Type::codec));

    /**
     * A codec for a list of functions.
     */
    Codec<List<RecipeFunction>> LIST_CODEC = CODEC.listOf(1, Integer.MAX_VALUE);

    /**
     * The {@link StreamCodec} equivalent of {@link #CODEC}.
     */
    StreamCodec<RegistryFriendlyByteBuf, RecipeFunction> STREAM_CODEC = ByteBufCodecs.registry(REGISTRY).dispatch(RecipeFunction::getType, Type::streamCodec);

    /**
     * The {@link StreamCodec} equivalent of {@link #LIST_CODEC}.
     */
    StreamCodec<RegistryFriendlyByteBuf, List<RecipeFunction>> LIST_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs.list());

    /**
     * Get the type of this recipe function.
     *
     * @return The type of this recipe function.
     */
    Type<?> getType();

    /**
     * Apply this recipe function, modifying the result item.
     *
     * @param container The current crafting container.
     * @param result    The result item to modify. This may be mutated in place.
     * @return The new result item. This may be {@code result}.
     */
    ItemStack apply(CraftingContainer container, ItemStack result);

    /**
     * Properties about a type of {@link RecipeFunction}. These are stored in {@linkplain #REGISTRY a Minecraft
     * registry}, and returned by {@link #getType()}.
     *
     * @param codec       The codec to read and write this class of recipe functions with.
     * @param streamCodec The network codec to read and write this class of recipe functions with.
     * @param <T>         The type of recipe function.
     */
    record Type<T extends RecipeFunction>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
    }
}
