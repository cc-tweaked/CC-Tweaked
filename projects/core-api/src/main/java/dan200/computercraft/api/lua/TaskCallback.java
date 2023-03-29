// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.lua;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import javax.annotation.Nullable;

final class TaskCallback implements ILuaCallback, LuaTask {
    private final LuaTask task;

    private volatile @Nullable Object[] result;
    private volatile @MonotonicNonNull LuaException failure;

    private final long taskId;
    private final MethodResult pull = MethodResult.pullEvent("task_complete", this);

    private TaskCallback(ILuaContext context, LuaTask task) throws LuaException {
        this.task = task;
        taskId = context.issueMainThreadTask(this);
    }

    @Nullable
    @Override
    public Object[] execute() throws LuaException {
        // Store the result/exception: we read these back when receiving the task_complete event.
        try {
            result = task.execute();
            return null;
        } catch (LuaException e) {
            // We only care about storing LuaExceptions as we want also want to preserve custom error levels: other
            // exceptions won't have this extra data!
            failure = e;
            throw e;
        }
    }

    @Override
    public MethodResult resume(Object[] response) throws LuaException {
        if (response.length < 3 || !(response[1] instanceof Number eventTask) || !(response[2] instanceof Boolean isOk)) {
            return pull;
        }

        if (eventTask.longValue() != taskId) return pull;

        if (isOk) {
            return MethodResult.of(result);
        } else if (failure != null) {
            throw failure;
        } else if (response.length >= 4 && response[3] instanceof String message) {
            throw new LuaException(message);
        } else {
            throw new LuaException("error");
        }
    }

    static MethodResult make(ILuaContext context, LuaTask func) throws LuaException {
        return new TaskCallback(context, func).pull;
    }
}
