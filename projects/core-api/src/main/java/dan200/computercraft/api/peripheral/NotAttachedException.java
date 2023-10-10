// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral;

import java.io.Serial;

/**
 * Thrown when performing operations on {@link IComputerAccess} when the current peripheral is no longer attached to
 * the computer.
 */
public class NotAttachedException extends IllegalStateException {
    @Serial
    private static final long serialVersionUID = 1221244785535553536L;

    public NotAttachedException() {
        super("You are not attached to this computer");
    }

    public NotAttachedException(String s) {
        super(s);
    }
}
