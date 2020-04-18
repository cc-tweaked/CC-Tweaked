/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;

import static dan200.computercraft.api.lua.ArgumentHelper.*;

public class TermAPI implements ILuaAPI
{
    private final Terminal m_terminal;
    private final IComputerEnvironment m_environment;

    public TermAPI( IAPIEnvironment environment )
    {
        m_terminal = environment.getTerminal();
        m_environment = environment.getComputerEnvironment();
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "term" };
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "write",
            "scroll",
            "setCursorPos",
            "setCursorBlink",
            "getCursorPos",
            "getSize",
            "clear",
            "clearLine",
            "setTextColour",
            "setTextColor",
            "setBackgroundColour",
            "setBackgroundColor",
            "isColour",
            "isColor",
            "getTextColour",
            "getTextColor",
            "getBackgroundColour",
            "getBackgroundColor",
            "blit",
            "setPaletteColour",
            "setPaletteColor",
            "getPaletteColour",
            "getPaletteColor",
            "nativePaletteColour",
            "nativePaletteColor",
            "getCursorBlink",
        };
    }

    public static byte parseColour( Object[] args ) throws LuaException
    {
        int colour = getInt( args, 0 );
        if( colour <= 0 )
        {
            throw new LuaException( "Colour out of range" );
        }
        byte bit = (byte) (getHighestBit( colour ) - 1);
        if( bit < 0 || bit > 15 )
        {
            throw new LuaException( "Colour out of range" );
        }
        return bit;
    }

    public static Object[] encodeColour( int colour ) throws LuaException
    {
        return new Object[] { 1 << colour };
    }

    public static void setColour( Terminal terminal, int colour, double r, double g, double b )
    {
        if( terminal.getPalette() != null )
        {
            terminal.getPalette().setColour( colour, r, g, b );
            terminal.setChanged();
        }
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0:
            {
                // write
                String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
                synchronized( m_terminal )
                {
                    m_terminal.write( text );
                    m_terminal.setCursorPos( m_terminal.getCursorX() + text.length(), m_terminal.getCursorY() );
                }
                return null;
            }
            case 1:
            {
                // scroll
                int y = getInt( args, 0 );
                synchronized( m_terminal )
                {
                    m_terminal.scroll( y );
                }
                return null;
            }
            case 2:
            {
                // setCursorPos
                int x = getInt( args, 0 ) - 1;
                int y = getInt( args, 1 ) - 1;
                synchronized( m_terminal )
                {
                    m_terminal.setCursorPos( x, y );
                }
                return null;
            }
            case 3:
            {
                // setCursorBlink
                boolean b = getBoolean( args, 0 );
                synchronized( m_terminal )
                {
                    m_terminal.setCursorBlink( b );
                }
                return null;
            }
            case 4:
            {
                // getCursorPos
                int x, y;
                synchronized( m_terminal )
                {
                    x = m_terminal.getCursorX();
                    y = m_terminal.getCursorY();
                }
                return new Object[] { x + 1, y + 1 };
            }
            case 5:
            {
                // getSize
                int width, height;
                synchronized( m_terminal )
                {
                    width = m_terminal.getWidth();
                    height = m_terminal.getHeight();
                }
                return new Object[] { width, height };
            }
            case 6: // clear
                synchronized( m_terminal )
                {
                    m_terminal.clear();
                }
                return null;
            case 7: // clearLine
                synchronized( m_terminal )
                {
                    m_terminal.clearLine();
                }
                return null;
            case 8:
            case 9:
            {
                // setTextColour/setTextColor
                byte colour = parseColour( args );
                synchronized( m_terminal )
                {
                    m_terminal.setTextColour( colour );
                }
                return null;
            }
            case 10:
            case 11:
            {
                // setBackgroundColour/setBackgroundColor
                byte colour = parseColour( args );
                synchronized( m_terminal )
                {
                    m_terminal.setBackgroundColour( colour );
                }
                return null;
            }
            case 12:
            case 13: // isColour/isColor
                return new Object[] { m_environment.isColour() };
            case 14:
            case 15: // getTextColour/getTextColor
                return encodeColour( m_terminal.getTextColour() );
            case 16:
            case 17: // getBackgroundColour/getBackgroundColor
                return encodeColour( m_terminal.getBackgroundColour() );
            case 18:
            {
                // blit
                String text = getString( args, 0 );
                String textColour = getString( args, 1 );
                String backgroundColour = getString( args, 2 );
                if( textColour.length() != text.length() || backgroundColour.length() != text.length() )
                {
                    throw new LuaException( "Arguments must be the same length" );
                }

                synchronized( m_terminal )
                {
                    m_terminal.blit( text, textColour, backgroundColour );
                    m_terminal.setCursorPos( m_terminal.getCursorX() + text.length(), m_terminal.getCursorY() );
                }
                return null;
            }
            case 19:
            case 20:
            {
                // setPaletteColour/setPaletteColor
                int colour = 15 - parseColour( args );
                if( args.length == 2 )
                {
                    int hex = getInt( args, 1 );
                    double[] rgb = Palette.decodeRGB8( hex );
                    setColour( m_terminal, colour, rgb[0], rgb[1], rgb[2] );
                }
                else
                {
                    double r = getFiniteDouble( args, 1 );
                    double g = getFiniteDouble( args, 2 );
                    double b = getFiniteDouble( args, 3 );
                    setColour( m_terminal, colour, r, g, b );
                }
                return null;
            }
            case 21:
            case 22:
            {
                // getPaletteColour/getPaletteColor
                int colour = 15 - parseColour( args );
                synchronized( m_terminal )
                {
                    if( m_terminal.getPalette() != null )
                    {
                        return ArrayUtils.toObject( m_terminal.getPalette().getColour( colour ) );
                    }
                }
                return null;
            }
            case 23:
            case 24:
            {
                // nativePaletteColour/nativePaletteColor
                int colour = 15 - parseColour( args );
                Colour c = Colour.fromInt( colour );

                float[] rgb = c.getRGB();

                Object[] rgbObj = new Object[rgb.length];
                for( int i = 0; i < rgbObj.length; ++i ) rgbObj[i] = rgb[i];
                return rgbObj;
            }
            case 25:
                // getCursorBlink
                return new Object[] { m_terminal.getCursorBlink() };
            default:
                return null;
        }
    }

    private static byte getHighestBit( int group )
    {
        byte bit = 0;
        while( group > 0 )
        {
            group >>= 1;
            bit++;
        }
        return bit;
    }
}
