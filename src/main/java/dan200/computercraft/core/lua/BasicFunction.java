/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.asm.LuaMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.VarArgFunction;

/**
 * An "optimised" version of {@link ResultInterpreterFunction} which is guaranteed to never yield.
 * <p>
 * As we never yield, we do not need to push a function to the stack, which removes a small amount of overhead.
 */
class BasicFunction extends VarArgFunction {
    private static final Logger LOG = LoggerFactory.getLogger(BasicFunction.class);
    private final CobaltLuaMachine machine;
    private final LuaMethod method;
    private final Object instance;
    private final ILuaContext context;
    private final String name;

    BasicFunction(CobaltLuaMachine machine, LuaMethod method, Object instance, ILuaContext context, String name) {
        this.machine = machine;
        this.method = method;
        this.instance = instance;
        this.context = context;
        this.name = name;
    }

    @Override
    public Varargs invoke(LuaState luaState, Varargs args) throws LuaError {
        var arguments = VarargArguments.of(args);
        MethodResult results;
        try {
            results = method.apply(instance, context, arguments);
        } catch (LuaException e) {
            throw wrap(e);
        } catch (Throwable t) {
            LOG.error(Logging.JAVA_ERROR, "Error calling {} on {}", name, instance, t);
            throw new LuaError("Java Exception Thrown: " + t, 0);
        } finally {
            arguments.close();
        }

        if (results.getCallback() != null) {
            throw new IllegalStateException("Cannot have a yielding non-yielding function");
        }
        return machine.toValues(results.getResult());
    }

    public static LuaError wrap(LuaException exception) {
        return exception.hasLevel() ? new LuaError(exception.getMessage()) : new LuaError(exception.getMessage(), exception.getLevel());
    }
}
