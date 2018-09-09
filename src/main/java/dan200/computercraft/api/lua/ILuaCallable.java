/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;

/**
 * A function which calls performs an action in a specific context (such as on the server thread) and returns a result.
 *
 * @see MethodResult#onMainThread(ILuaCallable)
 * @see ILuaContext#executeMainThreadTask(ILuaTask)
 */
@FunctionalInterface
public interface ILuaCallable
{
    /**
     * Run the code within the specified context and return the result to continue with.
     *
     * @return The result of executing this function. Note that this may not be evaluated within the same context as
     * this call is.
     * @throws LuaException If you throw any exception from this function, a lua error will be raised with the
     *                      same message as your exception. Use this to throw appropriate errors if the wrong
     *                      arguments are supplied to your method.
     */
    @Nonnull
    MethodResult execute() throws LuaException;
}
