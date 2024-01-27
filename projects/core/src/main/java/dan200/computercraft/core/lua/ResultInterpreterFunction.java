// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.ILuaCallback;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.methods.LuaMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.ResumableVarArgFunction;


/**
 * Calls a {@link LuaMethod}, and interprets the resulting {@link MethodResult}, either returning the result or yielding
 * and resuming the supplied continuation.
 */
class ResultInterpreterFunction extends ResumableVarArgFunction<ResultInterpreterFunction.Container> {
    private static final Logger LOG = LoggerFactory.getLogger(ResultInterpreterFunction.class);

    static class Container {
        ILuaCallback callback;
        final int errorAdjust;

        Container(ILuaCallback callback, int errorAdjust) {
            this.callback = callback;
            this.errorAdjust = errorAdjust;
        }
    }

    private final CobaltLuaMachine machine;
    private final LuaMethod method;
    private final Object instance;
    private final ILuaContext context;
    private final String funcName;

    ResultInterpreterFunction(CobaltLuaMachine machine, LuaMethod method, Object instance, ILuaContext context, String name) {
        this.machine = machine;
        this.method = method;
        this.instance = instance;
        this.context = context;
        funcName = name;
    }

    @Override
    protected Varargs invoke(LuaState state, DebugFrame debugFrame, Varargs args) throws LuaError, UnwindThrowable {
        var arguments = VarargArguments.of(args);
        MethodResult results;
        try {
            results = method.apply(instance, context, arguments);
        } catch (LuaException e) {
            throw wrap(e, 0);
        } catch (Throwable t) {
            LOG.error(Logging.JAVA_ERROR, "Error calling {} on {}", funcName, instance, t);
            throw new LuaError("Java Exception Thrown: " + t, 0);
        } finally {
            arguments.close();
        }

        var callback = results.getCallback();
        var ret = machine.toValues(results.getResult());

        if (callback == null) return ret;

        debugFrame.state = new Container(callback, results.getErrorAdjust());
        return LuaThread.yield(state, ret);
    }

    @Override
    public Varargs resume(LuaState state, Container container, Varargs args) throws LuaError, UnwindThrowable {
        MethodResult results;
        var arguments = CobaltLuaMachine.toObjects(args);
        try {
            results = container.callback.resume(arguments);
        } catch (LuaException e) {
            throw wrap(e, container.errorAdjust);
        } catch (Throwable t) {
            LOG.error(Logging.JAVA_ERROR, "Error calling {} on {}", funcName, container.callback, t);
            throw new LuaError("Java Exception Thrown: " + t, 0);
        }

        var ret = machine.toValues(results.getResult());

        var callback = results.getCallback();
        if (callback == null) return ret;

        container.callback = callback;
        return LuaThread.yield(state, ret);
    }

    public static LuaError wrap(LuaException exception, int adjust) {
        if (!exception.hasLevel() && adjust == 0) return new LuaError(exception.getMessage());

        var level = exception.getLevel();
        return new LuaError(exception.getMessage(), level <= 0 ? level : level + adjust);
    }
}
