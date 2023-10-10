// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.peripheral;

import dan200.computercraft.api.peripheral.IPeripheral;

/**
 * A peripheral which will be updated every time the computer ticks.
 */
public interface TickablePeripheral extends IPeripheral {
    void tick();
}
