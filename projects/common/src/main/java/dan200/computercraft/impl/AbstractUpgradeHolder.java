// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.api.upgrades.UpgradeHandle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

/**
 * Holds a single instance of an upgrade.
 * <p>
 * Unlike {@link UpgradeHandle}, accesses to this object are <strong>NOT</strong> thread-safe. This is required for
 * compatibility with previous {@link ITurtleAccess}/{@link IPocketAccess} methods which did not enforce thread-safety.
 *
 * @param <U> The type of upgrade.
 */
public abstract class AbstractUpgradeHolder<U extends UpgradeBase> {
    private final U upgrade;
    private @Unmodifiable ItemStack item;
    private final CompoundTag data;

    private @Nullable UpgradeData<U> upgradeData;

    private final UpgradeHandle<U> handle;

    protected AbstractUpgradeHolder(UpgradeData<U> upgrade) {
        this.upgrade = upgrade.upgrade();
        item = upgrade.getUpgradeItem();
        data = upgrade.data().copy();
        handle = new UpgradeHandleImpl<>(this);
    }

    /**
     * Check whether this accesses to the {@linkplain #getHandle() handle} are valid, throwing an error if not. This is
     * not called for direct access of the upgrades via this class.
     * <p>
     * This function should typically check we're on the main thread, the owner is still active, and the holder is still
     * the equipped upgrade.
     */
    protected abstract void requireValid();

    /**
     * Mark this upgrade as having changed.
     */
    protected abstract void setChanged();

    /**
     * Remove this upgrade, invalidating the holder immediately.
     */
    protected abstract void remove();

    public final U getUpgrade() {
        return upgrade;
    }

    public final UpgradeData<U> getUpgradeData() {
        return upgradeData != null ? upgradeData : (upgradeData = UpgradeData.of(upgrade, item, data));
    }

    public final @Unmodifiable ItemStack getItem() {
        return item;
    }

    public final CompoundTag getData() {
        return data;
    }

    final void setItem(ItemStack item) {
        this.item = item.copy();
        this.upgradeData = null;
    }

    /**
     * Recompute the item from the upgrade's data. This should be called from methods like
     * {@link ITurtleAccess#updateUpgradeNBTData(TurtleSide)} to support upgrades that do not preserve their item.
     */
    public final void recomputeItem() {
        setItem(upgrade.getUpgradeItem(data));
    }

    public final UpgradeHandle<U> getHandle() {
        return handle;
    }
}
