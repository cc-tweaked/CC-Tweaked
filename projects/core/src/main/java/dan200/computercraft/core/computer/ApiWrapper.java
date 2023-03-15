// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import dan200.computercraft.api.lua.ILuaAPI;

/**
 * A wrapper for {@link ILuaAPI}s which cleans up after a {@link ComputerSystem} when the computer is shutdown.
 */
final class ApiWrapper implements ILuaAPI {
    private final ILuaAPI delegate;
    private final ComputerSystem system;

    ApiWrapper(ILuaAPI delegate, ComputerSystem system) {
        this.delegate = delegate;
        this.system = system;
    }

    @Override
    public String[] getNames() {
        return delegate.getNames();
    }

    @Override
    public void startup() {
        delegate.startup();
    }

    @Override
    public void update() {
        delegate.update();
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
        system.unmountAll();
    }

    public ILuaAPI getDelegate() {
        return delegate;
    }
}
