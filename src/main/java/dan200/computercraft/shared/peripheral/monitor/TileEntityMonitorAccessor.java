// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computer.core.Terminal;

public interface TileEntityMonitorAccessor {
    Terminal cct$getOriginTerminal();

    void cct$setTextScale(int scale);

    int cct$getTextScale();

    boolean isColour();
}
