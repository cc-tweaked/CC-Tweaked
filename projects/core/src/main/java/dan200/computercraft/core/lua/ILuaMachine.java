// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.lua;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a machine which will execute Lua code. Technically this API is flexible enough to support many languages,
 * but you'd need a way to provide alternative ROMs, BIOSes, etc...
 * <p>
 * There should only be one concrete implementation at any one time, which is currently {@link CobaltLuaMachine}. If
 * external mod authors are interested in registering their own machines, we can look into how we can provide some
 * mechanism for registering these.
 */
public interface ILuaMachine {
    /**
     * Resume the machine, either starting or resuming the coroutine.
     * <p>
     * This should destroy the machine if it failed to execute successfully.
     *
     * @param eventName The name of the event. This is {@code null} when first starting the machine. Note, this may
     *                  do nothing if it does not match the event filter.
     * @param arguments The arguments for this event.
     * @return The result of loading this machine. Will either be OK, or the error message that occurred when
     * executing.
     */
    MachineResult handleEvent(@Nullable String eventName, @Nullable Object[] arguments);

    /**
     * Print some information about the internal execution state.
     * <p>
     * This function is purely intended for debugging, its output should not be relied on in any way.
     *
     * @param out The buffer to write to.
     */
    void printExecutionState(StringBuilder out);

    /**
     * Close the Lua machine, aborting any running functions and deleting the internal state.
     */
    void close();

    interface Factory {
        /**
         * Attempt to create a Lua machine.
         *
         * @param environment The environment under which to create the machine.
         * @param bios        The {@link InputStream} which contains the initial function to run. This should be used to
         *                    load the initial function - it should <em>NOT</em> be executed.
         * @return The successfully created machine, or an error.
         * @throws IOException      If reading the underlying {@link InputStream} failed.
         * @throws MachineException An error occurred while creating the machine.
         */
        ILuaMachine create(MachineEnvironment environment, InputStream bios) throws IOException, MachineException;
    }
}
