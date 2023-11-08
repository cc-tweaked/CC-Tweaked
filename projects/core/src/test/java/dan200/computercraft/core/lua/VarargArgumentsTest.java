// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.lua;

import dan200.computercraft.api.lua.LuaException;
import org.junit.jupiter.api.Test;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.ValueFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VarargArgumentsTest {
    private static LuaTable tableWithCustomType() {
        var metatable = new LuaTable();
        try {
            metatable.rawset(Constants.NAME, ValueFactory.valueOf("some type"));
        } catch (LuaError e) {
            throw new IllegalStateException("Cannot create metatable", e);
        }

        var table = new LuaTable();
        table.setMetatable(null, metatable);
        return table;
    }

    @Test
    public void testGet() {
        var args = VarargArguments.of(tableWithCustomType());
        assertEquals(Map.of(), args.get(0));
        assertEquals("some type", args.getType(0));
    }

    @Test
    public void testGetAfterEscape() {
        var args = VarargArguments.of(tableWithCustomType());
        args.escapes();
        args.close();

        assertEquals(Map.of(), args.get(0));
        assertEquals("some type", args.getType(0));
    }

    @Test
    public void testGetAfterEscapeDrop() throws LuaException {
        var args = VarargArguments.of(ValueFactory.varargsOf(Constants.NIL, tableWithCustomType()));
        args.escapes();
        args.close();

        assertEquals(Map.of(), args.drop(1).get(0));
        assertEquals("some type", args.drop(1).getType(0));
    }

    @Test
    public void testGetAfterClose() {
        var args = VarargArguments.of(tableWithCustomType());
        args.close();

        assertThrows(IllegalStateException.class, () -> args.get(0));
        assertThrows(IllegalStateException.class, () -> args.getType(0));
    }

    @Test
    public void testEscapeAfterClose() {
        var args = VarargArguments.of(tableWithCustomType());
        args.close();

        assertThrows(IllegalStateException.class, args::escapes);
    }
}
