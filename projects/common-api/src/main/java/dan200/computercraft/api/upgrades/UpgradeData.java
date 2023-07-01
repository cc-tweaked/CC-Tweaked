// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;

/**
 * An upgrade (i.e. a {@link ITurtleUpgrade}) and its current upgrade data.
 * <p>
 * <strong>IMPORTANT:</strong> The {@link #data()} in an upgrade data is often a reference to the original upgrade data.
 * Be careful to take a {@linkplain #copy() defensive copy} if you plan to use the data in this upgrade.
 *
 * @param upgrade The current upgrade.
 * @param data    The upgrade's data.
 * @param <T>     The type of upgrade, either {@link ITurtleUpgrade} or {@link IPocketUpgrade}.
 */
public record UpgradeData<T extends UpgradeBase>(T upgrade, CompoundTag data) {
    /**
     * A utility method to construct a new {@link UpgradeData} instance.
     *
     * @param upgrade An upgrade.
     * @param data    The upgrade's data.
     * @param <T>     The type of upgrade.
     * @return The new {@link UpgradeData} instance.
     */
    public static <T extends UpgradeBase> UpgradeData<T> of(T upgrade, CompoundTag data) {
        return new UpgradeData<>(upgrade, data);
    }

    /**
     * Create an {@link UpgradeData} containing the default {@linkplain #data() data} for an upgrade.
     *
     * @param upgrade The upgrade instance.
     * @param <T>     The type of upgrade.
     * @return The default upgrade data.
     */
    public static <T extends UpgradeBase> UpgradeData<T> ofDefault(T upgrade) {
        return of(upgrade, upgrade.getUpgradeData(upgrade.getCraftingItem()));
    }

    /**
     * Take a copy of a (possibly {@code null}) {@link UpgradeData} instance.
     *
     * @param upgrade The copied upgrade data.
     * @param <T>     The type of upgrade.
     * @return The newly created upgrade data.
     */
    @Contract("!null -> !null; null -> null")
    public static <T extends UpgradeBase> @Nullable UpgradeData<T> copyOf(@Nullable UpgradeData<T> upgrade) {
        return upgrade == null ? null : upgrade.copy();
    }

    /**
     * Get the {@linkplain UpgradeBase#getUpgradeItem(CompoundTag) upgrade item} for this upgrade.
     * <p>
     * This returns a defensive copy of the item, to prevent accidental mutation of the upgrade data or original
     * {@linkplain UpgradeBase#getCraftingItem() upgrade stack}.
     *
     * @return This upgrade's item.
     */
    public ItemStack getUpgradeItem() {
        return upgrade.getUpgradeItem(data).copy();
    }

    /**
     * Take a copy of this {@link UpgradeData}. This returns a new instance with the same upgrade and a fresh copy of
     * the upgrade data.
     *
     * @return A copy of the current instance.
     */
    public UpgradeData<T> copy() {
        return new UpgradeData<>(upgrade(), data().copy());
    }
}
