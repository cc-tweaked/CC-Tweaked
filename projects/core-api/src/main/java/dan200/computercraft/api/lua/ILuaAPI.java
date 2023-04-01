// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.lua;

/**
 * Represents a Lua object which is stored as a global variable on computer startup. This must either provide
 * {@link LuaFunction} annotated functions or implement {@link IDynamicLuaObject}.
 * <p>
 * Before implementing this interface, consider alternative methods of providing methods. It is generally preferred
 * to use peripherals to provide functionality to users.
 *
 * @see ILuaAPIFactory For providing custom APIs to computers.
 */
public interface ILuaAPI {
    /**
     * Get the globals this API will be assigned to. This will override any other global, so you should
     *
     * @return A list of globals this API will be assigned to.
     */
    String[] getNames();

    /**
     * Called when the computer is turned on.
     * <p>
     * One should only interact with the file system.
     */
    default void startup() {
    }

    /**
     * Called every time the computer is ticked. This can be used to process various.
     */
    default void update() {
    }

    /**
     * Called when the computer is turned off or unloaded.
     * <p>
     * This should reset the state of the object, disposing any remaining file handles, or other resources.
     */
    default void shutdown() {
    }
}
