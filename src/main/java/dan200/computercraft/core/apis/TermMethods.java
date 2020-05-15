/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.Palette;
import dan200.computercraft.shared.util.StringUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;

/**
 * A base class for all objects which interact with a terminal. Namely the {@link TermAPI} and monitors.
 */
public abstract class TermMethods
{
    private static int getHighestBit( int group )
    {
        int bit = 0;
        while( group > 0 )
        {
            group >>= 1;
            bit++;
        }
        return bit;
    }

    @Nonnull
    public abstract Terminal getTerminal() throws LuaException;

    public abstract boolean isColour() throws LuaException;

    @LuaFunction
    public final void write( IArguments arguments ) throws LuaException
    {
        String text = StringUtil.toString( arguments.get( 0 ) );
        Terminal terminal = getTerminal();
        synchronized( terminal )
        {
            terminal.write( text );
            terminal.setCursorPos( terminal.getCursorX() + text.length(), terminal.getCursorY() );
        }
    }

    @LuaFunction
    public final void scroll( int y ) throws LuaException
    {
        getTerminal().scroll( y );
    }

    @LuaFunction
    public final Object[] getCursorPos() throws LuaException
    {
        Terminal terminal = getTerminal();
        return new Object[] { terminal.getCursorX() + 1, terminal.getCursorY() + 1 };
    }

    @LuaFunction
    public final void setCursorPos( int x, int y ) throws LuaException
    {
        Terminal terminal = getTerminal();
        synchronized( terminal )
        {
            terminal.setCursorPos( x - 1, y - 1 );
        }
    }

    @LuaFunction
    public final boolean getCursorBlink() throws LuaException
    {
        return getTerminal().getCursorBlink();
    }

    @LuaFunction
    public final void setCursorBlink( boolean blink ) throws LuaException
    {
        Terminal terminal = getTerminal();
        synchronized( terminal )
        {
            terminal.setCursorBlink( blink );
        }
    }

    @LuaFunction
    public final Object[] getSize() throws LuaException
    {
        Terminal terminal = getTerminal();
        return new Object[] { terminal.getWidth(), terminal.getHeight() };
    }

    @LuaFunction
    public final void clear() throws LuaException
    {
        getTerminal().clear();
    }

    @LuaFunction
    public final void clearLine() throws LuaException
    {
        getTerminal().clearLine();
    }

    @LuaFunction( { "getTextColour", "getTextColor" } )
    public final int getTextColour() throws LuaException
    {
        return encodeColour( getTerminal().getTextColour() );
    }

    @LuaFunction( { "setTextColour", "setTextColor" } )
    public final void setTextColour( int colourArg ) throws LuaException
    {
        int colour = parseColour( colourArg );
        Terminal terminal = getTerminal();
        synchronized( terminal )
        {
            terminal.setTextColour( colour );
        }
    }

    @LuaFunction( { "getBackgroundColour", "getBackgroundColor" } )
    public final int getBackgroundColour() throws LuaException
    {
        return encodeColour( getTerminal().getBackgroundColour() );
    }

    @LuaFunction( { "setBackgroundColour", "setBackgroundColor" } )
    public final void setBackgroundColour( int colourArg ) throws LuaException
    {
        int colour = parseColour( colourArg );
        Terminal terminal = getTerminal();
        synchronized( terminal )
        {
            terminal.setBackgroundColour( colour );
        }
    }

    @LuaFunction( { "isColour", "isColor" } )
    public final boolean getIsColour() throws LuaException
    {
        return isColour();
    }

    @LuaFunction
    public final void blit( String text, String textColour, String backgroundColour ) throws LuaException
    {
        if( textColour.length() != text.length() || backgroundColour.length() != text.length() )
        {
            throw new LuaException( "Arguments must be the same length" );
        }

        Terminal terminal = getTerminal();
        synchronized( terminal )
        {
            terminal.blit( text, textColour, backgroundColour );
            terminal.setCursorPos( terminal.getCursorX() + text.length(), terminal.getCursorY() );
        }
    }

    @LuaFunction( { "setPaletteColour", "setPaletteColor" } )
    public final void setPaletteColour( IArguments args ) throws LuaException
    {
        int colour = 15 - parseColour( args.getInt( 0 ) );
        if( args.count() == 2 )
        {
            int hex = args.getInt( 1 );
            double[] rgb = Palette.decodeRGB8( hex );
            setColour( getTerminal(), colour, rgb[0], rgb[1], rgb[2] );
        }
        else
        {
            double r = args.getFiniteDouble( 1 );
            double g = args.getFiniteDouble( 2 );
            double b = args.getFiniteDouble( 3 );
            setColour( getTerminal(), colour, r, g, b );
        }
    }

    @LuaFunction( { "getPaletteColour", "getPaletteColor" } )
    public final Object[] getPaletteColour( int colourArg ) throws LuaException
    {
        int colour = 15 - parseColour( colourArg );
        Terminal terminal = getTerminal();
        synchronized( terminal )
        {
            if( terminal.getPalette() != null )
            {
                return ArrayUtils.toObject( terminal.getPalette().getColour( colour ) );
            }
        }
        return null;
    }

    public static int parseColour( int colour ) throws LuaException
    {
        if( colour <= 0 ) throw new LuaException( "Colour out of range" );
        colour = getHighestBit( colour ) - 1;
        if( colour < 0 || colour > 15 ) throw new LuaException( "Colour out of range" );
        return colour;
    }


    public static int encodeColour( int colour )
    {
        return 1 << colour;
    }

    public static void setColour( Terminal terminal, int colour, double r, double g, double b )
    {
        if( terminal.getPalette() != null )
        {
            terminal.getPalette().setColour( colour, r, g, b );
            terminal.setChanged();
        }
    }
}
