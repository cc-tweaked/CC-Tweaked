/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.Colour;

import javax.annotation.Nonnull;

public class TermAPI extends TermMethods implements ILuaAPI
{
    private final Terminal terminal;
    private final IComputerEnvironment environment;

    public TermAPI( IAPIEnvironment environment )
    {
        terminal = environment.getTerminal();
        this.environment = environment.getComputerEnvironment();
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "term" };
    }

    @LuaFunction( { "nativePaletteColour", "nativePaletteColor" } )
    public final Object[] nativePaletteColour( Object[] args ) throws LuaException
    {
        int colour = 15 - parseColour( args );
        Colour c = Colour.fromInt( colour );

        float[] rgb = c.getRGB();

        Object[] rgbObj = new Object[rgb.length];
        for( int i = 0; i < rgbObj.length; ++i ) rgbObj[i] = rgb[i];
        return rgbObj;
    }

    @Nonnull
    @Override
    public Terminal getTerminal()
    {
        return terminal;
    }

    @Override
    public boolean isColour()
    {
        return environment.isColour();
    }
}
