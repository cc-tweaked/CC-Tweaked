// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeHandle;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

/**
 * An upgrade (i.e. a {@link ITurtleUpgrade}) and its current upgrade data.
 *
 * @param <T> The type of upgrade, either {@link ITurtleUpgrade} or {@link IPocketUpgrade}.
 */
public final class UpgradeData<T extends UpgradeBase> implements UpgradeHandle<T> {
    private final Holder.Reference<T> upgrade;
    private final DataComponentPatch data;
    private final @Nullable ItemStackTemplate stack;

    private UpgradeData(Holder.Reference<T> upgrade, DataComponentPatch data, @Nullable ItemStackTemplate stack) {
        if (!upgrade.isBound()) throw new IllegalArgumentException("Holder is not bound");

        this.upgrade = upgrade;
        this.data = data;
        this.stack = stack;

        if (stack != null) {
            if (stack.count() != 1) {
                throw new IllegalArgumentException(
                    "Error constructing UpgradeData for " + upgrade.key().identifier() + ": Item " + stack + " must be of size 1."
                );
            }

            if (!upgrade.value().getCraftingIngredient().acceptsItem(stack.item())) {
                throw new IllegalArgumentException(
                    "Error constructing UpgradeData for " + upgrade.key().identifier() + ": Item " + stack + " is not suitable for this upgrade."
                );
            }
        }
    }

    /**
     * A utility method to construct a new {@link UpgradeData} instance.
     *
     * @param upgrade The upgrade.
     * @param data    The upgrade's data. This is <strong>not</strong> copied, so should not be modified after use.
     * @param <T>     The type of upgrade.
     * @return The new {@link UpgradeData} instance.
     * @deprecated Use {@link #of(Holder.Reference, ItemStackTemplate, DataComponentPatch)}
     */
    @Deprecated(since = "1.121.0")
    public static <T extends UpgradeBase> UpgradeData<T> of(Holder.Reference<T> upgrade, DataComponentPatch data) {
        return new UpgradeData<>(upgrade, data, null);
    }

    /**
     * A utility method to construct a new {@link UpgradeData} instance.
     *
     * @param <T>     The type of upgrade.
     * @param upgrade An upgrade.
     * @param stack   The upgrade's stack.
     * @param data    The upgrade's data.
     * @return The new {@link UpgradeData} instance.
     * @since 1.121.0
     */
    public static <T extends UpgradeBase> UpgradeData<T> of(Holder.Reference<T> upgrade, ItemStackTemplate stack, DataComponentPatch data) {
        return new UpgradeData<>(upgrade, data, stack);
    }

    public static <T extends UpgradeBase> UpgradeData<T> of(UpgradeHandle<T> upgrade) {
        return upgrade instanceof UpgradeData<T> data ? data : UpgradeData.of(upgrade.getUpgradeHolder(), upgrade.getItem(), upgrade.getData());
    }

    /**
     * Create an {@link UpgradeData} containing the default {@linkplain #getData() data} for an upgrade.
     *
     * @param holder The upgrade instance.
     * @param <T>    The type of upgrade.
     * @return The default upgrade data.
     */
    public static <T extends UpgradeBase> UpgradeData<T> ofDefault(Holder.Reference<T> holder) {
        return of(holder, holder.value().getUpgradeItem(DataComponentPatch.EMPTY), DataComponentPatch.EMPTY);
    }

    /**
     * Get the current upgrade.
     *
     * @return The current upgrade
     */
    @Override
    public Holder.Reference<T> getUpgradeHolder() {
        return upgrade;
    }

    /**
     * Get the upgrade data.
     *
     * @return The upgrade data.
     */
    @Override
    public DataComponentPatch getData() {
        return data;
    }

    /**
     * Get the {@linkplain UpgradeBase#getUpgradeItem(DataComponentPatch) upgrade item} for this upgrade.
     *
     * @return This upgrade's item.
     */
    @Override
    public ItemStackTemplate getItem() {
        return stack == null ? getUpgrade().getUpgradeItem(data) : stack;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;
        return obj instanceof UpgradeData<?> that
            && upgrade == that.upgrade && data.equals(that.data)
            && ((stack == null && that.stack == null) || getItem().equals(that.getItem()));
    }

    @Override
    public int hashCode() {
        return 31 * upgrade.hashCode() + data.hashCode();
    }

    @Override
    public String toString() {
        return "UpgradeData[upgrade=" + upgrade.key().identifier() + ", data=" + data + ", stack=" + stack + "]";
    }
}
