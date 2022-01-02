/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.IDynamicLuaObject;
import dan200.computercraft.api.lua.ILuaAPI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;

/**
 * Represents a machine which will execute Lua code. Technically this API is flexible enough to support many languages,
 * but you'd need a way to provide alternative ROMs, BIOSes, etc...
 *
 * There should only be one concrete implementation at any one time, which is currently {@link CobaltLuaMachine}. If
 * external mod authors are interested in registering their own machines, we can look into how we can provide some
 * mechanism for registering these.
 *
 * This should provide implementations of {@link dan200.computercraft.api.lua.ILuaContext}, and the ability to convert
 * {@link IDynamicLuaObject}s into something the VM understands, as well as handling method calls.
 */
public interface ILuaMachine
{
    /**
     * Inject an API into the global environment of this machine. This should construct an object, as it would for any
     * {@link IDynamicLuaObject} and set it to all names in {@link ILuaAPI#getNames()}.
     *
     * Called before {@link #loadBios(InputStream)}.
     *
     * @param api The API to register.
     */
    void addAPI( @Nonnull ILuaAPI api );

    /**
     * Create a function from the provided program, and set it up to run when {@link #handleEvent(String, Object[])} is
     * called.
     *
     * This should destroy the machine if it failed to load the bios.
     *
     * @param bios The stream containing the boot program.
     * @return The result of loading this machine. Will either be OK, or the error message when loading the bios.
     */
    MachineResult loadBios( @Nonnull InputStream bios );

    /**
     * Resume the machine, either starting or resuming the coroutine.
     *
     * This should destroy the machine if it failed to execute successfully.
     *
     * @param eventName The name of the event. This is {@code null} when first starting the machine. Note, this may
     *                  do nothing if it does not match the event filter.
     * @param arguments The arguments for this event.
     * @return The result of loading this machine. Will either be OK, or the error message that occurred when
     * executing.
     */
    MachineResult handleEvent( @Nullable String eventName, @Nullable Object[] arguments );

    /**
     * Close the Lua machine, aborting any running functions and deleting the internal state.
     */
    void close();
}
