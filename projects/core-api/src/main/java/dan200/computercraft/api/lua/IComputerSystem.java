// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

import dan200.computercraft.api.peripheral.IComputerAccess;

import javax.annotation.Nullable;

/**
 * An interface passed to {@link ILuaAPIFactory} in order to provide additional information
 * about a computer.
 */
public interface IComputerSystem extends IComputerAccess {
    /**
     * Get the label for this computer.
     *
     * @return This computer's label, or {@code null} if it is not set.
     */
    @Nullable
    String getLabel();
}
