package dan200.computercraft.core.lua;

// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0


import cc.tweaked.CCTweaked;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.asm.LuaMethod;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.VarArgFunction;

import java.util.logging.Level;

/**
 * An "optimised" version of {@code ResultInterpreterFunction} which is guaranteed to never yield.
 * <p>
 * As we never yield, we do not need to push a function to the stack, which removes a small amount of overhead.
 */
class BasicFunction extends VarArgFunction {
    private final CobaltLuaMachine machine;
    private final LuaMethod method;
    private final Object instance;
    private final String funcName;

    BasicFunction(CobaltLuaMachine machine, LuaMethod method, Object instance, String name) {
        this.machine = machine;
        this.method = method;
        this.instance = instance;
        funcName = name;
    }

    @Override
    public Varargs invoke(LuaState luaState, Varargs args) throws LuaError {
        VarargArguments arguments = VarargArguments.of(args);
        Object[] results;
        try {
            results = method.apply(instance, arguments);
        } catch (LuaException e) {
            throw wrap(e);
        } catch (Throwable t) {
            CCTweaked.LOG.log(Level.SEVERE, String.format("Error calling %s on %s", funcName, instance), t);
            throw new LuaError("Java Exception Thrown: " + t, 0);
        } finally {
            arguments.close();
        }

        return machine.toValues(results);
    }

    public static LuaError wrap(LuaException exception) {
        return exception.hasLevel() ? new LuaError(exception.getMessage()) : new LuaError(exception.getMessage(), exception.getLevel());
    }
}
