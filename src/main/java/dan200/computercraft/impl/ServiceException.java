/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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
