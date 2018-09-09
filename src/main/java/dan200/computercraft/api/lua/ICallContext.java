/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */

package dan200.computercraft.api.lua;

import javax.annotation.Nonnull;

/**
 * An interface passed to peripherals and {@link ILuaObject}s by computers or turtles, providing methods that allow the
 * method to interact with the invoking computer.
 */
public interface ICallContext
{
    /**
     * Queue a task to be executed on the main server thread at the beginning of next tick, but do not wait for it to
     * complete. This should be used when you need to interact with the world in a thread-safe manner but do not care
     * about the result or you wish to run asynchronously.
     *
     * When the task has finished, it will enqueue a {@code task_completed} event, which takes the task id, a success
     * value and the return values, or an error message if it failed. If you need to wait on this event, it may be
     * better to use {@link MethodResult#onMainThread(ILuaCallable)}.
     *
     * @param task The task to execute on the main thread.
     * @return The "id" of the task. This will be the first argument to the {@code task_completed} event.
     * @throws LuaException If the task could not be queued.
     */
    long issueMainThreadTask( @Nonnull ILuaTask task ) throws LuaException;
}
