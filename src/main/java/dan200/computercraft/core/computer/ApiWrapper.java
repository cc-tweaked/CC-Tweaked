/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

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
        return this.delegate.getNames();
    }

    @Override
    public void startup() {
        this.delegate.startup();
    }

    @Override
    public void update() {
        this.delegate.update();
    }

    @Override
    public void shutdown() {
        this.delegate.shutdown();
        this.system.unmountAll();
    }

    public ILuaAPI getDelegate() {
        return this.delegate;
    }
}
