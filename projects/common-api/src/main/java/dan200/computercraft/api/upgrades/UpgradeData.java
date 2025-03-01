// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;

/**
 * An upgrade (i.e. a {@link ITurtleUpgrade}) and its current upgrade data.
 *
 * @param holder The current upgrade holder.
 * @param data   The upgrade's data.
 * @param <T>    The type of upgrade, either {@link ITurtleUpgrade} or {@link IPocketUpgrade}.
 */
public record UpgradeData<T extends UpgradeBase>(Holder.Reference<T> holder, DataComponentPatch data) {
    /**
     * A utility method to construct a new {@link UpgradeData} instance.
     *
     * @param holder An upgrade.
     * @param data   The upgrade's data.
     * @param <T>    The type of upgrade.
     * @return The new {@link UpgradeData} instance.
     */
    public static <T extends UpgradeBase> UpgradeData<T> of(Holder.Reference<T> holder, DataComponentPatch data) {
        return new UpgradeData<>(holder, data);
    }

    /**
     * Create an {@link UpgradeData} containing the default {@linkplain #data() data} for an upgrade.
     *
     * @param holder The upgrade instance.
     * @param <T>    The type of upgrade.
     * @return The default upgrade data.
     */
    public static <T extends UpgradeBase> UpgradeData<T> ofDefault(Holder.Reference<T> holder) {
        var upgrade = holder.value();
        return of(holder, upgrade.getUpgradeData(upgrade.getCraftingItem()));
    }

    public UpgradeData {
        if (!holder.isBound()) throw new IllegalArgumentException("Holder is not bound");
    }

    /**
     * Get the current upgrade.
     *
     * @return The current upgrade.
     */
    public T upgrade() {
        return holder().value();
    }

    /**
     * Get the {@linkplain UpgradeBase#getUpgradeItem(DataComponentPatch) upgrade item} for this upgrade.
     * <p>
     * This returns a defensive copy of the item, to prevent accidental mutation of the upgrade data or original
     * {@linkplain UpgradeBase#getCraftingItem() upgrade stack}.
     *
     * @return This upgrade's item.
     */
    public ItemStack getUpgradeItem() {
        return upgrade().getUpgradeItem(data).copy();
    }
}
