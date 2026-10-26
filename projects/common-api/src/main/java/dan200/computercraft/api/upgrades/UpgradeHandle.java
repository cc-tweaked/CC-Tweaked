// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

/**
 * A mutable handle to an upgrade, allowing an upgrade's item and data to be read and written to.
 * <p>
 * All functions in this class must be called from the main thread, and will error if the called from another thread,
 * or if the upgrade has since been unequipped. Typically one will only interact with an {@link UpgradeHandle} from
 * {@link ITurtleUpgrade}/{@link IPocketUpgrade} functions (for instance,
 * {@link ITurtleUpgrade#update(ITurtleAccess, TurtleSide)} or {@link IPocketUpgrade#onRightClick(Level, IPocketAccess, IPeripheral)}),
 * where accesses should be safe.
 *
 * @param <U> The type of upgrade.
 * @since 1.121.0
 */
@ApiStatus.NonExtendable
public interface UpgradeHandle<U extends UpgradeBase> {
    /**
     * Get the upgrade for this handle.
     *
     * @return The upgrade for this handle.
     */
    U getUpgrade();

    /**
     * Get the upgrade's item.
     * <p>
     * This returns a defensive copy of the original item, so updates to this will not be reflected. Call
     * {@link #setItem(ItemStack)} to change the item,
     *
     * @return The upgrade's item.
     * @see UpgradeData#getUpgradeItem()
     */
    ItemStack getItem();

    /**
     * Set the upgrade's item.
     *
     * @param item The new item.
     * @throws IllegalStateException If the item is not valid for this upgrade.
     * @see #getItem()
     */
    void setItem(ItemStack item);

    /**
     * Get the upgrade's additional data.
     * <p>
     * This data may be mutated in-place. When doing so {@link #setDataChanged()} must be called afterwards.
     *
     * @return The upgrade's additional data.
     * @see UpgradeData#data()
     * @see #setDataChanged()
     */
    CompoundTag getData();

    /**
     * Mark the {@linkplain #getData() upgrade's data} as having changed.
     *
     * @see #getData()
     */
    void setDataChanged();

    /**
     * Remove this upgrade, unequipping it and destroying the item.
     * <p>
     * This is typically used when the item is consumed (e.g. a tool using all durability). This is equivalent to
     * calling {@link ITurtleAccess#setUpgradeWithData(TurtleSide, UpgradeData)} or {@link IPocketAccess#setUpgrade(UpgradeData)}
     * with {@code null}.
     */
    void remove();
}
