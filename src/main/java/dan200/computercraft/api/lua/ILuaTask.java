/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.lua;

import javax.annotation.Nullable;

/**
 * A task which can be executed via {@link ILuaContext#issueMainThreadTask(ILuaTask)} This will be run on the main
 * thread, at the beginning of the
 * next tick.
 *
 * @see ILuaContext#issueMainThreadTask(ILuaTask)
 */
@FunctionalInterface
public interface ILuaTask
{
    /**
     * Execute this task.
     *
     * @return The arguments to add to the {@code task_completed} event.
     * @throws LuaException If you throw any exception from this function, a lua error will be raised with the
     *                      same message as your exception. Use this to throw appropriate errors if the wrong
     *                      arguments are supplied to your method.
     */
    @Nullable
    Object[] execute() throws LuaException;
}
