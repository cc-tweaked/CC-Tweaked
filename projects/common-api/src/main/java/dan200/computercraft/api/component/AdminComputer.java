// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.component;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import org.jetbrains.annotations.ApiStatus;

/**
 * A computer which has permission to perform administrative/op commands, such as the command computer.
 */
@ApiStatus.NonExtendable
public interface AdminComputer {
    /**
     * The permissions that this computer has.
     *
     * @return The permissions for this computer.
     * @see CommandSourceStack#permissions()
     */
    default PermissionSet permissions() {
        return LevelBasedPermissionSet.GAMEMASTER;
    }
}
