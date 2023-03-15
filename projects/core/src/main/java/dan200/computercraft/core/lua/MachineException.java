// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua;

import java.io.Serial;

/**
 * An exception thrown by a {@link ILuaMachine}.
 */
public class MachineException extends Exception {
    @Serial
    private static final long serialVersionUID = 400833668352232261L;

    /**
     * Create a new {@link MachineException}.
     *
     * @param message The message to display. This should be user-friendly, and not contain any internal information -
     *                that should just be logged to the console.
     */
    public MachineException(String message) {
        super(message);
    }
}
