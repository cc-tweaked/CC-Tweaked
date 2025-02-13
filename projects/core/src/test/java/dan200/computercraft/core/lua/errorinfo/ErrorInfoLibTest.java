// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua.errorinfo;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorInfoLibTest {
    @Test
    public void testNilInfoForUnknownLibFunction() throws LuaError, CompileException {
        var state = newState();
        var thread = captureError(state, "string.forma()");

        assertEquals(
            new ErrorInfoLib.NilInfo(
                "call",
                new ErrorInfoLib.ValueSource(false, state.globals().rawget("string"), ValueFactory.valueOf("forma"))
            ),
            ErrorInfoLib.getInfoForNil(state, thread, 0)
        );
    }

    @Test
    public void testNilInfoForUnknownGlobal() throws LuaError, CompileException {
        var state = newState();
        var thread = captureError(state, "pront()");

        assertEquals(
            new ErrorInfoLib.NilInfo(
                "call",
                new ErrorInfoLib.ValueSource(true, state.globals(), ValueFactory.valueOf("pront"))
            ),
            ErrorInfoLib.getInfoForNil(state, thread, 0)
        );
    }

    @Test
    public void testNilInfoForComplexExpression() throws LuaError, CompileException {
        var state = newState();
        var thread = captureError(state, "x = { { y = 1 } }; for i = 1, #x do x[i].z() end");

        var inner = ((LuaTable) state.globals().rawget("x")).rawget(1);
        assertEquals(
            new ErrorInfoLib.NilInfo(
                "call",
                new ErrorInfoLib.ValueSource(false, inner, ValueFactory.valueOf("z"))
            ),
            ErrorInfoLib.getInfoForNil(state, thread, 0)
        );
    }

    private static LuaState newState() throws LuaError {
        var state = new LuaState();
        CoreLibraries.standardGlobals(state);
        return state;
    }

    private static LuaThread captureError(LuaState state, @Language("lua") String contents) throws CompileException, LuaError {
        var fn = LoadState.load(state, new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)), "=in.lua", state.globals());
        var thread = new LuaThread(state, fn);
        Assertions.assertThrows(LuaError.class, () -> LuaThread.run(thread, Constants.NIL));
        return thread;
    }
}
