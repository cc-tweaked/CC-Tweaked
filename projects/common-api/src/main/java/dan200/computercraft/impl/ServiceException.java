// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.io.Serial;

/**
 * A ComputerCraft-related service failed to load.
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 */
@ApiStatus.Internal
class ServiceException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -8392300691666423882L;

    ServiceException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
