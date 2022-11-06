/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.Colour;


/**
 * Interact with a computer's terminal or monitors, writing text and drawing
 * ASCII graphics.
 *
 * @cc.module term
 */
public class TermAPI extends TermMethods implements ILuaAPI {
    private final Terminal terminal;

    public TermAPI(IAPIEnvironment environment) {
        terminal = environment.getTerminal();
    }

    @Override
    public String[] getNames() {
        return new String[]{ "term" };
    }

    /**
     * Get the default palette value for a colour.
     *
     * @param colour The colour whose palette should be fetched.
     * @return The RGB values.
     * @throws LuaException When given an invalid colour.
     * @cc.treturn number The red channel, will be between 0 and 1.
     * @cc.treturn number The green channel, will be between 0 and 1.
     * @cc.treturn number The blue channel, will be between 0 and 1.
     * @cc.since 1.81.0
     * @see TermMethods#setPaletteColour(IArguments) To change the palette colour.
     */
    @LuaFunction({ "nativePaletteColour", "nativePaletteColor" })
    public final Object[] nativePaletteColour(int colour) throws LuaException {
        var actualColour = 15 - parseColour(colour);
        var c = Colour.fromInt(actualColour);
        return new Object[]{ c.getR(), c.getG(), c.getB() };
    }

    @Override
    public Terminal getTerminal() {
        return terminal;
    }
}
