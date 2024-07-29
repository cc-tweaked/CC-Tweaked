// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import dan200.computercraft.api.lua.ILuaAPI;

import javax.annotation.Nullable;

/**
 * A wrapper for {@link ILuaAPI}s which provides an optional shutdown hook to clean up resources.
 *
 * @param api       The original API.
 * @param lifecycle The optional lifecycle hooks for this API.
 */
record ApiWrapper(ILuaAPI api, @Nullable ApiLifecycle lifecycle) {
    public void startup() {
        if (lifecycle != null) lifecycle.startup();
        api.startup();
    }

    public void update() {
        api.update();
    }

    public void shutdown() {
        api.shutdown();
        if (lifecycle != null) lifecycle.shutdown();
    }

    public ILuaAPI api() {
        return api;
    }
}
