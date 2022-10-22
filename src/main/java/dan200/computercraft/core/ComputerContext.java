/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core;

import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.computer.mainthread.MainThreadScheduler;
import dan200.computercraft.core.lua.CobaltLuaMachine;
import dan200.computercraft.core.lua.ILuaMachine;

/**
 * The global context under which computers run.
 */
public final class ComputerContext implements AutoCloseable
{
    private final GlobalEnvironment globalEnvironment;
    private final MainThreadScheduler mainThreadScheduler;
    private final ILuaMachine.Factory factory;

    public ComputerContext( GlobalEnvironment globalEnvironment, MainThreadScheduler mainThreadScheduler, ILuaMachine.Factory factory )
    {
        this.globalEnvironment = globalEnvironment;
        this.mainThreadScheduler = mainThreadScheduler;
        this.factory = factory;
    }

    /**
     * Create a default {@link ComputerContext} with the given global environment.
     *
     * @param environment         The current global environment.
     * @param mainThreadScheduler The main thread scheduler to use.
     */
    public ComputerContext( GlobalEnvironment environment, MainThreadScheduler mainThreadScheduler )
    {
        this( environment, mainThreadScheduler, CobaltLuaMachine::new );
    }

    /**
     * The global environment.
     *
     * @return The current global environment.
     */
    public GlobalEnvironment globalEnvironment()
    {
        return globalEnvironment;
    }

    /**
     * The {@link MainThreadScheduler} instance used to run main-thread tasks.
     *
     * @return The current main thread scheduler.
     */
    public MainThreadScheduler mainThreadScheduler()
    {
        return mainThreadScheduler;
    }

    /**
     * The factory to create new Lua machines.
     *
     * @return The current Lua machine factory.
     */
    public ILuaMachine.Factory luaFactory()
    {
        return factory;
    }

    /**
     * Close the current {@link ComputerContext}, disposing of any resources inside.
     */
    @Override
    public void close()
    {
    }
}
