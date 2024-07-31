// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.component;

import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * A computer which has permission to perform administrative/op commands, such as the command computer.
 */
@ApiStatus.NonExtendable
public interface AdminComputer {
    /**
     * The permission level that this computer can operate at.
     *
     * @return The permission level for this computer.
     * @see CommandSourceStack#hasPermission(int)
     */
    default int permissionLevel() {
        return 2;
    }
}
