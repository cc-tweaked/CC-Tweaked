// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.turtle;

import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import dan200.computercraft.impl.ComputerCraftAPIService;
import dan200.computercraft.impl.upgrades.SerialiserWithCraftingIngredient;
import dan200.computercraft.impl.upgrades.SerialiserWithCraftingItem;
import dan200.computercraft.impl.upgrades.SimpleSerialiser;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Reads a {@link ITurtleUpgrade} from disk and reads/writes it to a network packet.
 * <p>
 * These should be registered in a {@link Registry} while the game is loading, much like {@link RecipeSerializer}s.
 * <p>
 * If your turtle upgrade doesn't have any associated configurable parameters (like most upgrades), you can use
 * {@link #simple(Function)} or {@link #simpleWithCustomIngredient(BiFunction)} to create a basic upgrade serialiser.
 *
 * @param <T> The type of turtle upgrade this is responsible for serialising.
 * @see ITurtleUpgrade
 * @see TurtleUpgradeDataProvider
 * @see dan200.computercraft.api.client.turtle.TurtleUpgradeModeller
 */
public interface TurtleUpgradeSerialiser<T extends ITurtleUpgrade> extends UpgradeSerialiser<T> {
    /**
     * The ID for the associated registry.
     *
     * @return The registry key.
     */
    static ResourceKey<Registry<TurtleUpgradeSerialiser<?>>> registryId() {
        return ComputerCraftAPIService.get().turtleUpgradeRegistryId();
    }

    /**
     * Create an upgrade serialiser for a simple upgrade. This is similar to a {@link SimpleCraftingRecipeSerializer},
     * but for upgrades.
     * <p>
     * If you might want to vary the item, it's suggested you use {@link #simpleWithCustomIngredient(BiFunction)} instead.
     *
     * @param factory Generate a new upgrade with a specific ID.
     * @param <T>     The type of the generated upgrade.
     * @return The serialiser for this upgrade
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeSerialiser<T> simple(Function<ResourceLocation, T> factory) {
        final class Impl extends SimpleSerialiser<T> implements TurtleUpgradeSerialiser<T> {
            private Impl(Function<ResourceLocation, T> constructor) {
                super(constructor);
            }
        }

        return new Impl(factory);
    }

    /**
     * Create an upgrade serialiser for a simple upgrade whose crafting item can be specified.
     *
     * @param factory Generate a new upgrade with a specific ID and crafting item. The returned upgrade's
     *                {@link UpgradeBase#getCraftingItem()} <strong>MUST</strong> equal the provided item.
     * @param <T>     The type of the generated upgrade.
     * @return The serialiser for this upgrade.
     * @see #simple(Function)  For upgrades whose crafting stack should not vary.
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeSerialiser<T> simpleWithCustomItem(BiFunction<ResourceLocation, ItemStack, T> factory) {
        final class Impl extends SerialiserWithCraftingItem<T> implements TurtleUpgradeSerialiser<T> {
            private Impl(BiFunction<ResourceLocation, ItemStack, T> factory) {
                super(factory);
            }
        }

        return new Impl(factory);
    }

    /**
     * Create an upgrade serialiser for a simple upgrade whose crafting item can be specified.
     *
     * @param factory Generate a new upgrade with a specific ID and crafting item. The returned upgrade's
     *                {@link UpgradeBase#getCraftingIngredient()} <strong>MUST</strong> equal the provided ingredient.
     * @param <T>     The type of the generated upgrade.
     * @return The serialiser for this upgrade.
     * @see #simple(Function)  For upgrades whose crafting stack should not vary.
     * @since 1.121.0
     */
    static <T extends ITurtleUpgrade> TurtleUpgradeSerialiser<T> simpleWithCustomIngredient(BiFunction<ResourceLocation, Ingredient, T> factory) {
        final class Impl extends SerialiserWithCraftingIngredient<T> implements TurtleUpgradeSerialiser<T> {
            private Impl(BiFunction<ResourceLocation, Ingredient, T> factory) {
                super(factory);
            }
        }

        return new Impl(factory);
    }
}
