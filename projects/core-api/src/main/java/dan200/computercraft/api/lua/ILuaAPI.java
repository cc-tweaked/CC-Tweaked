// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.lua;

import javax.annotation.Nullable;

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
     * Get the globals this API will be assigned to.
     * <p>
     * This will override any other global, so you should be careful to pick a unique name. Alternatively, you may
     * return the empty array here, and instead override {@link #getModuleName()}.
     *
     * @return A list of globals this API will be assigned to.
     */
    String[] getNames();

    /**
     * Get the module name this API should be available as.
     * <p>
     * Rather than (or as well as) making this API available as a global, APIs can be exposed as {@code require}able
     * modules. This is generally more idiomatic, as it avoids polluting the global environment.
     * <p>
     * Modules defined here take precedence over user-defined modules, and so like with {@link #getNames()}, you should
     * be careful to pick a unique name. It is recommended that module names should be camel case, and live under a
     * namespace associated with your mod. For instance, {@code "mod_id.a_custom_api"}.
     *
     * @return The module name of this API, or {@code null} if this API should not be loadable as a module.
     */
    default @Nullable String getModuleName() {
        return null;
    }

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
