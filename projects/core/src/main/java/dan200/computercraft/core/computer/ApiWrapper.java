// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import dan200.computercraft.api.lua.ILuaAPI;

import javax.annotation.Nullable;

/**
 * A wrapper for {@link ILuaAPI}s which optionally manages the lifecycle of a {@link ComputerSystem}.
 */
final class ApiWrapper {
    private final ILuaAPI api;
    private final @Nullable ComputerSystem system;

    ApiWrapper(ILuaAPI api, @Nullable ComputerSystem system) {
        this.api = api;
        this.system = system;
    }

    public void startup() {
        api.startup();
    }

    public void update() {
        api.update();
    }

    public void shutdown() {
        api.shutdown();
        if (system != null) system.unmountAll();
    }

    public ILuaAPI api() {
        return api;
    }
}
