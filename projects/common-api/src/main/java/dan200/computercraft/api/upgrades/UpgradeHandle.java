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
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStackTemplate;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * A read-only handle to an upgrade, allowing an upgrade's item and data to be read.
 * <p>
 * All functions in this class must be called from the main thread, and will error if the called from another thread,
 * or if the upgrade has since been unequipped. Typically one will only interact with an {@link UpgradeHandle} from
 * {@link ITurtleUpgrade}/{@link IPocketUpgrade} functions (for instance,
 * {@link ITurtleUpgrade#update(ITurtleAccess, TurtleSide, MutableUpgradeHandle)} or
 * {@link IPocketUpgrade#onRightClick(IPocketAccess, MutableUpgradeHandle, IPeripheral)}),
 * where accesses should be safe.
 *
 * @param <U> The type of upgrade.
 * @since 1.121.0
 */
@ApiStatus.NonExtendable
public interface UpgradeHandle<U extends UpgradeBase> extends DataComponentGetter {
    /**
     * Get the upgrade for this handle.
     *
     * @return The upgrade for this handle.
     */
    Holder.Reference<U> getUpgradeHolder();

    /**
     * Get the upgrade for this handle.
     *
     * @return The upgrade for this handle.
     */
    default U getUpgrade() {
        return getUpgradeHolder().value();
    }

    /**
     * Get the upgrade's item.
     * <p>
     * This returns a defensive copy of the original item, so updates to this will not be reflected.
     *
     * @return The upgrade's item.
     * @see MutableUpgradeHandle#setItem(ItemStackTemplate)
     */
    ItemStackTemplate getItem();

    /**
     * Get the upgrade's additional data.
     *
     * @return The upgrade's additional data.
     * @see MutableUpgradeHandle#setData(DataComponentPatch)
     */
    DataComponentPatch getData();

    /**
     * Get a component from the upgrade's {@link #getData()} .
     *
     * @param component The component to get.
     * @param <T>       The type of the component's value.
     * @return The component, or {@code null} if not present.
     */
    @Override
    default <T> @Nullable T get(DataComponentType<? extends T> component) {
        return getData().get(DataComponentMap.EMPTY, component);
    }
}
