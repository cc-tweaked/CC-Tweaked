// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.core.computer.GlobalEnvironment;
import dan200.computercraft.core.computer.TimeoutState;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.metrics.MetricsObserver;

/**
 * Arguments used to construct a {@link ILuaMachine}.
 *
 * @param context    The Lua context to execute main-thread tasks with.
 * @param metrics    A sink to submit metrics to.
 * @param timeout    The current timeout state. This should be used by the machine to interrupt its execution.
 * @param apis       APIs to inject into the global environment. Each API should be converted into a Lua object
 *                   (following the same rules as any other value), and then set to all names in {@link ILuaAPI#getNames()}.
 * @param luaMethods A {@link MethodSupplier} to find methods on returned values.
 * @param hostString A {@linkplain GlobalEnvironment#getHostString() host string} to identify the current environment.
 * @see ILuaMachine.Factory
 */
public record MachineEnvironment(
    ILuaContext context,
    MetricsObserver metrics,
    TimeoutState timeout,
    Iterable<ILuaAPI> apis,
    MethodSupplier<LuaMethod> luaMethods,
    String hostString
) {
}
