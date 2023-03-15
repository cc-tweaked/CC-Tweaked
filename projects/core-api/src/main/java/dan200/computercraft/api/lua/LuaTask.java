// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.lua;

import javax.annotation.Nullable;

/**
 * A task which can be executed via {@link ILuaContext#issueMainThreadTask(LuaTask)} This will be run on the main
 * thread, at the beginning of the
 * next tick.
 *
 * @see ILuaContext#issueMainThreadTask(LuaTask)
 */
@FunctionalInterface
public interface LuaTask {
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
