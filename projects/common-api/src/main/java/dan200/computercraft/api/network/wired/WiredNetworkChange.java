// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.network.wired;

import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.Map;

/**
 * Represents a change to the objects on a wired network.
 *
 * @see WiredElement#networkChanged(WiredNetworkChange)
 */
public interface WiredNetworkChange {
    /**
     * A set of peripherals which have been removed. Note that there may be entries with the same name
     * in the added and removed set, but with a different peripheral.
     *
     * @return The set of removed peripherals.
     */
    Map<String, IPeripheral> peripheralsRemoved();

    /**
     * A set of peripherals which have been added. Note that there may be entries with the same name
     * in the added and removed set, but with a different peripheral.
     *
     * @return The set of added peripherals.
     */
    Map<String, IPeripheral> peripheralsAdded();
}
