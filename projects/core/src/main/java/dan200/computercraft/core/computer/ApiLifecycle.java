// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import dan200.computercraft.api.lua.ILuaAPI;

/**
 * Hooks for managing the lifecycle of an API. This allows adding additional logic to an API's {@link ILuaAPI#startup()}
 * and {@link ILuaAPI#shutdown()} methods.
 *
 * @see ILuaAPI
 * @see Computer#addApi(ILuaAPI, ApiLifecycle)
 */
public interface ApiLifecycle {
    /**
     * Called before the API's {@link ILuaAPI#startup()} method, may be used to set up resources.
     */
    default void startup() {
    }

    /**
     * Called after the API's {@link ILuaAPI#shutdown()} method, may be used to tear down resources.
     */
    default void shutdown() {
    }
}
