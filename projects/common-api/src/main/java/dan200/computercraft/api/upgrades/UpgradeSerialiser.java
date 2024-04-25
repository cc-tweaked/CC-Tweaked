// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import com.google.gson.JsonObject;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.pocket.PocketUpgradeDataProvider;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.impl.upgrades.SerialiserWithCraftingItem;
import dan200.computercraft.impl.upgrades.SimpleSerialiser;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A serialiser for {@link ITurtleUpgrade} or {@link IPocketUpgrade}s.
 * <p>
 * These should be registered in a {@link Registry} while the game is loading, much like {@link RecipeSerializer}s.
 * <p>
 * This interface is very similar to {@link RecipeSerializer}; each serialiser should correspond to a specific upgrade
 * class. Upgrades are then read from JSON files in datapacks, allowing multiple instances of the upgrade to be
 * registered.
 * <p>
 * If your upgrade doesn't have any associated configurable parameters (like most upgrades), you can use
 * {@link #simple(Function)} or {@link #simpleWithCustomItem(BiFunction)} to create a basic upgrade serialiser.
 * <p>
 * Upgrades may be data generated via a {@link UpgradeDataProvider} (see {@link TurtleUpgradeDataProvider} and
 * {@link PocketUpgradeDataProvider}).
 *
 * @param <T> The upgrade that this class can serialise and deserialise.
 * @see ITurtleUpgrade
 * @see IPocketUpgrade
 */
public interface UpgradeSerialiser<T extends UpgradeBase> {
    /**
     * Read this upgrade from a JSON file in a datapack.
     *
     * @param id     The ID of this upgrade.
     * @param object The JSON object to load this upgrade from.
     * @return The constructed upgrade, with a {@link UpgradeBase#getUpgradeID()} equal to {@code id}.
     * @see net.minecraft.util.GsonHelper For additional JSON helper methods.
     */
    T fromJson(ResourceLocation id, JsonObject object);

    /**
     * Read this upgrade from a network packet, sent from the server.
     *
     * @param id     The ID of this upgrade.
     * @param buffer The buffer object to read this upgrade from.
     * @return The constructed upgrade, with a {@link UpgradeBase#getUpgradeID()} equal to {@code id}.
     */
    T fromNetwork(ResourceLocation id, RegistryFriendlyByteBuf buffer);

    /**
     * Write this upgrade to a network packet, to be sent to the client.
     *
     * @param buffer  The buffer object to write this upgrade to
     * @param upgrade The upgrade to write.
     */
    void toNetwork(RegistryFriendlyByteBuf buffer, T upgrade);

    /**
     * Create an upgrade serialiser for a simple upgrade. This is similar to a {@link SimpleCraftingRecipeSerializer},
     * but for upgrades.
     * <p>
     * If you might want to vary the item, it's suggested you use {@link #simpleWithCustomItem(BiFunction)} instead.
     *
     * @param factory Generate a new upgrade with a specific ID.
     * @param <T>     The type of the generated upgrade.
     * @return The serialiser for this upgrade
     */
    static <T extends UpgradeBase> UpgradeSerialiser<T> simple(Function<ResourceLocation, T> factory) {
        return new SimpleSerialiser<>(factory);
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
    static <T extends UpgradeBase> UpgradeSerialiser<T> simpleWithCustomItem(BiFunction<ResourceLocation, ItemStack, T> factory) {
        return new SerialiserWithCraftingItem<>(factory);
    }
}
