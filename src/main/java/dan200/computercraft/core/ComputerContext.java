/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core;

import dan200.computercraft.core.computer.ComputerThread;
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
    private final ComputerThread computerScheduler;
    private final MainThreadScheduler mainThreadScheduler;
    private final ILuaMachine.Factory factory;

    public ComputerContext(
        GlobalEnvironment globalEnvironment, ComputerThread computerScheduler,
        MainThreadScheduler mainThreadScheduler, ILuaMachine.Factory factory
    )
    {
        this.globalEnvironment = globalEnvironment;
        this.computerScheduler = computerScheduler;
        this.mainThreadScheduler = mainThreadScheduler;
        this.factory = factory;
    }

    /**
     * Create a default {@link ComputerContext} with the given global environment.
     *
     * @param environment         The current global environment.
     * @param threads             The number of threads to use for the {@link #computerScheduler()}
     * @param mainThreadScheduler The main thread scheduler to use.
     */
    public ComputerContext( GlobalEnvironment environment, int threads, MainThreadScheduler mainThreadScheduler )
    {
        this( environment, new ComputerThread( threads ), mainThreadScheduler, CobaltLuaMachine::new );
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
     * The {@link ComputerThread} instance under which computers are run. This is closed when the context is closed, and
     * so should be unique per-context.
     *
     * @return The current computer thread manager.
     */
    public ComputerThread computerScheduler()
    {
        return computerScheduler;
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
        computerScheduler().stop();
    }
}
