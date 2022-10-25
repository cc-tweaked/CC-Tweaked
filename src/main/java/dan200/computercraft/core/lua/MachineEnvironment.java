/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.metrics.Metrics;
import dan200.computercraft.core.metrics.MetricsObserver;

/**
 * Arguments used to construct a {@link ILuaMachine}.
 *
 * @param context    The Lua context to execute main-thread tasks with.
 * @param metrics    A sink to submit metrics to. You do not need to submit task timings here, it should only be for additional
 *                   metrics such as {@link Metrics#COROUTINES_CREATED}
 * @param timeout    The current timeout state. This should be used by the machine to interrupt its execution.
 * @param hostString A {@linkplain GlobalEnvironment#getHostString() host string} to identify the current environment.
 * @see ILuaMachine.Factory
 */
public record MachineEnvironment(ILuaContext context, MetricsObserver metrics, TimeoutState timeout, String hostString)
{
}
