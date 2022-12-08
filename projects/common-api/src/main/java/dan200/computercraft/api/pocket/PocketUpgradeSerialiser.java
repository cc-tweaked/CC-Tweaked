/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.pocket;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import dan200.computercraft.impl.upgrades.SerialiserWithCraftingItem;
import dan200.computercraft.impl.upgrades.SimpleSerialiser;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Reads a {@link IPocketUpgrade} from disk and reads/writes it to a network packet.
 * <p>
 * This follows the same format as {@link dan200.computercraft.api.turtle.TurtleUpgradeSerialiser} - consult the
 * documentation there for more information.
 *
 * @param <T> The type of pocket computer upgrade this is responsible for serialising.
 * @see IPocketUpgrade
 * @see PocketUpgradeDataProvider
 */
public interface PocketUpgradeSerialiser<T extends IPocketUpgrade> extends UpgradeSerialiser<T> {
    /**
     * The ID for the associated registry.
     */
    ResourceKey<Registry<PocketUpgradeSerialiser<?>>> REGISTRY_ID = ResourceKey.createRegistryKey(new ResourceLocation(ComputerCraftAPI.MOD_ID, "pocket_upgrade_serialiser"));

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
    static <T extends IPocketUpgrade> PocketUpgradeSerialiser<T> simple(Function<ResourceLocation, T> factory) {
        final class Impl extends SimpleSerialiser<T> implements PocketUpgradeSerialiser<T> {
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
    static <T extends IPocketUpgrade> PocketUpgradeSerialiser<T> simpleWithCustomItem(BiFunction<ResourceLocation, ItemStack, T> factory) {
        final class Impl extends SerialiserWithCraftingItem<T> implements PocketUpgradeSerialiser<T> {
            private Impl(BiFunction<ResourceLocation, ItemStack, T> factory) {
                super(factory);
            }
        }

        return new Impl(factory);
    }
}
