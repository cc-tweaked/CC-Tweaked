// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.apis;

import dan200.computer.core.IAPIEnvironment;
import dan200.computer.core.IComputerEnvironment;
import dan200.computer.core.ILuaAPI;
import dan200.computer.core.Terminal;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.util.Colour;


/**
 * Interact with a computer's terminal or monitors, writing text and drawing
 * ASCII graphics.
 *
 * @cc.module term
 */
public class TermAPI extends TermMethods implements ILuaAPI {
    private final Terminal terminal;
    private final IComputerEnvironment environment;

    public TermAPI(IAPIEnvironment environment) {
        this.environment = environment.getComputerEnvironment();
        terminal = environment.getTerminal();
    }

    @Override
    public String[] getNames() {
        return new String[]{ "term" };
    }

    @Override
    public void startup() {
    }

    @Override
    public void advance(double v) {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public String[] getMethodNames() {
        return new String[0];
    }

    @Override
    public Object[] callMethod(int i, Object[] objects) {
        throw new IllegalStateException();
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
        Colour c = Colour.fromInt(parseColour(colour));
        return new Object[]{ c.getR(), c.getG(), c.getB() };
    }

    @Override
    protected boolean isColour() {
        return environment.isColour();
    }

    @Override
    protected Terminal getTerminal() {
        return terminal;
    }
}
