// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.pocket.PocketUpgradeDataProvider;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.impl.upgrades.UpgradeTypeImpl;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import java.util.function.Function;

/**
 * The type of a {@linkplain ITurtleUpgrade turtle} or {@linkplain IPocketUpgrade pocket} upgrade.
 * <p>
 * Turtle and pocket computer upgrades are registered using Minecraft's dynamic registry system. As a result, they
 * follow a similar design to other dynamic content, such as {@linkplain Recipe recipes} or {@link LootItemFunction
 * loot functions}.
 * <p>
 * First, one adds a new class implementing {@link ITurtleUpgrade} or {@link IPocketUpgrade}). This is responsible for
 * handling all the logic of your upgrade.
 * <p>
 * However, the upgrades are not registered directly. Instead, each upgrade class should have a corresponding
 * {@link UpgradeType}, which is responsible for loading the upgrade from a datapack. The upgrade type should then be
 * registered in its appropriate registry ({@link ITurtleUpgrade#typeRegistry()},
 * {@link IPocketUpgrade#typeRegistry()}).
 * <p>
 * In order to register the actual upgrade, a JSON file referencing your upgrade type should be added to a datapack. It
 * is recommended to do this via the data generators (see {@link TurtleUpgradeDataProvider} and
 * {@link PocketUpgradeDataProvider}).
 *
 * @param <T> The upgrade subclass that this upgrade type represents.
 * @see ITurtleUpgrade
 * @see IPocketUpgrade
 */
public interface UpgradeType<T extends UpgradeBase> {
    /**
     * The codec to read and write this upgrade from a datapack.
     *
     * @return The codec for this upgrade.
     */
    MapCodec<T> codec();

    /**
     * Create a new upgrade type.
     *
     * @param codec The codec
     * @param <T>   The type of the generated upgrade.
     * @return The newly created upgrade type.
     */
    static <T extends UpgradeBase> UpgradeType<T> create(MapCodec<T> codec) {
        return new UpgradeTypeImpl<>(codec);
    }

    /**
     * Create an upgrade type for an upgrade that takes no arguments.
     * <p>
     * If you might want to vary the item, it's suggested you use {@link #simpleWithCustomItem(Function)} instead.
     *
     * @param instance Generate a new upgrade with a specific ID.
     * @param <T>      The type of the generated upgrade.
     * @return A new upgrade type.
     */
    static <T extends UpgradeBase> UpgradeType<T> simple(T instance) {
        return create(MapCodec.unit(instance));
    }

    /**
     * Create an upgrade type for a simple upgrade whose crafting item can be specified.
     *
     * @param factory Generate a new upgrade with a specific ID and crafting item. The returned upgrade's
     *                {@link UpgradeBase#getCraftingItem()} <strong>MUST</strong> equal the provided item.
     * @param <T>     The type of the generated upgrade.
     * @return A new upgrade type.
     * @see #simple(UpgradeBase)  For upgrades whose crafting stack should not vary.
     */
    static <T extends UpgradeBase> UpgradeType<T> simpleWithCustomItem(Function<ItemStack, T> factory) {
        return create(BuiltInRegistries.ITEM.byNameCodec()
            .xmap(x -> factory.apply(new ItemStack(x)), x -> x.getCraftingItem().getItem())
            .fieldOf("item"));
    }
}
