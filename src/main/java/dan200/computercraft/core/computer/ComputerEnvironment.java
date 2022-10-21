/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.filesystem.FileMount;

import javax.annotation.Nullable;

public interface ComputerEnvironment
{
    /**
     * Get the current in-game day.
     *
     * @return The current day.
     */
    int getDay();

    /**
     * Get the current in-game time of day.
     *
     * @return The current time.
     */
    double getTimeOfDay();

    /**
     * Construct the mount for this computer's user-writable data.
     *
     * @return The constructed mount or {@code null} if the mount could not be created.
     * @see FileMount
     */
    @Nullable
    IWritableMount createRootMount();
}
