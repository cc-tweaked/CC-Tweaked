// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.computer;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaTask;
import dan200.computercraft.core.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class LuaContext implements ILuaContext {
    private static final Logger LOG = LoggerFactory.getLogger(LuaContext.class);
    private final Computer computer;

    LuaContext(Computer computer) {
        this.computer = computer;
    }

    @Override
    public long issueMainThreadTask(final LuaTask task) throws LuaException {
        // Issue command
        final var taskID = computer.getUniqueTaskId();
        final Runnable iTask = () -> {
            try {
                var results = task.execute();
                if (results != null) {
                    var eventArguments = new Object[results.length + 2];
                    eventArguments[0] = taskID;
                    eventArguments[1] = true;
                    System.arraycopy(results, 0, eventArguments, 2, results.length);
                    computer.queueEvent("task_complete", eventArguments);
                } else {
                    computer.queueEvent("task_complete", new Object[]{ taskID, true });
                }
            } catch (LuaException e) {
                computer.queueEvent("task_complete", new Object[]{ taskID, false, e.getMessage() });
            } catch (Exception t) {
                LOG.error(Logging.JAVA_ERROR, "Error running task", t);
                computer.queueEvent("task_complete", new Object[]{
                    taskID, false, "Java Exception Thrown: " + t,
                });
            }
        };
        if (computer.queueMainThread(iTask)) {
            return taskID;
        } else {
            throw new LuaException("Task limit exceeded");
        }
    }
}
