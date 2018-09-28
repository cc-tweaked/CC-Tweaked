/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.TermAPI;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.Palette;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;

import static dan200.computercraft.core.apis.ArgumentHelper.*;

public class MonitorPeripheral implements IPeripheral
{
    private final TileMonitor m_monitor;

    public MonitorPeripheral( TileMonitor monitor )
    {
        m_monitor = monitor;
    }

    // IPeripheral implementation

    @Nonnull
    @Override
    public String getType()
    {
        return "monitor";
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
            "setTextScale",
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
            "getTextScale",
            "getCursorBlink",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object args[] ) throws LuaException
    {
        ServerMonitor monitor = m_monitor.getCachedServerMonitor();
        if( monitor == null ) throw new LuaException( "Monitor has been detatched" );

        Terminal terminal = monitor.getTerminal();
        if( terminal == null ) throw new LuaException( "Monitor has been detatched" );

        switch( method )
        {
            case 0:
            {
                // write
                String text;
                if( args.length > 0 && args[0] != null ) {
                    text = args[0].toString();
                } else {
                    text = "";
                }
                terminal.write( text );
                terminal.setCursorPos( terminal.getCursorX() + text.length(), terminal.getCursorY() );
                return null;
            }
            case 1:
            {
                // scroll
                int value = getInt( args, 0 );
                terminal.scroll( value );
                return null;
            }
            case 2:
            {
                // setCursorPos
                int x = getInt( args, 0 ) - 1;
                int y = getInt( args, 1 ) - 1;
                terminal.setCursorPos( x, y );
                return null;
            }
            case 3:
            {
                // setCursorBlink
                boolean blink = getBoolean( args, 0 );
                terminal.setCursorBlink( blink );
                return null;
            }
            case 4:
            {
                // getCursorPos
                return new Object[] {
                    terminal.getCursorX() + 1,
                    terminal.getCursorY() + 1
                };
            }
            case 5:
            {
                // getSize
                return new Object[] {
                    terminal.getWidth(),
                    terminal.getHeight()
                };
            }
            case 6:
            {
                // clear
                terminal.clear();
                return null;
            }
            case 7:
            {
                // clearLine
                terminal.clearLine();
                return null;
            }
            case 8:
            {
                // setTextScale
                int scale = (int) (getReal( args, 0 ) * 2.0);
                if( scale < 1 || scale > 10 )
                {
                    throw new LuaException( "Expected number in range 0.5-5" );
                }
                monitor.setTextScale( scale );
                return null;
            }
            case 9:
            case 10:
            {
                // setTextColour/setTextColor
                int colour = TermAPI.parseColour( args );
                terminal.setTextColour( colour );
                return null;
            }
            case 11:
            case 12:
            {
                // setBackgroundColour/setBackgroundColor
                int colour = TermAPI.parseColour( args );
                terminal.setBackgroundColour( colour );
                return null;
            }
            case 13:
            case 14:
            {
                // isColour/isColor
                return new Object[] {
                    monitor.isColour()
                };
            }
            case 15:
            case 16:
            {
                // getTextColour/getTextColor
                return TermAPI.encodeColour( terminal.getTextColour() );
            }
            case 17:
            case 18:
            {
                // getBackgroundColour/getBackgroundColor
                return TermAPI.encodeColour( terminal.getBackgroundColour() );
            }
            case 19:
            {
                // blit
                String text = getString( args, 0 );
                String textColour = getString( args, 1 );
                String backgroundColour = getString( args, 2 );
                if( textColour.length() != text.length() || backgroundColour.length() != text.length() )
                {
                    throw new LuaException( "Arguments must be the same length" );
                }

                terminal.blit( text, textColour, backgroundColour );
                terminal.setCursorPos( terminal.getCursorX() + text.length(), terminal.getCursorY() );
                return null;
            }
            case 20:
            case 21:
            {
                // setPaletteColour/setPaletteColor
                int colour = 15 - TermAPI.parseColour( args );
                if( args.length == 2 )
                {
                    int hex = getInt( args, 1 );
                    double[] rgb = Palette.decodeRGB8( hex );
                    TermAPI.setColour( terminal, colour, rgb[ 0 ], rgb[ 1 ], rgb[ 2 ] );
                }
                else
                {
                    double r = getReal( args, 1 );
                    double g = getReal( args, 2 );
                    double b = getReal( args, 3 );
                    TermAPI.setColour( terminal, colour, r, g, b );
                }
                return null;
            }
            case 22:
            case 23:
            {
                // getPaletteColour/getPaletteColor
                Palette palette = terminal.getPalette();

                int colour = 15 - TermAPI.parseColour( args );

                if( palette != null )
                {
                    return ArrayUtils.toObject( palette.getColour( colour ) );
                }
                return null;
            }
            case 24:
            {
                // getTextScale
                return new Object[] { monitor.getTextScale() / 2.0 };
            }
            case 25:
                // getCursorBlink
                return new Object[] { terminal.getCursorBlink() };
            default:
                return null;
        }
    }

    @Override
    public void attach( @Nonnull IComputerAccess computer )
    {
        m_monitor.addComputer( computer );
    }

    @Override
    public void detach( @Nonnull IComputerAccess computer )
    {
        m_monitor.removeComputer( computer );
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        if( other != null && other instanceof MonitorPeripheral )
        {
            MonitorPeripheral otherMonitor = (MonitorPeripheral)other;
            if( otherMonitor.m_monitor == this.m_monitor )
            {
                return true;
            }
        }
        return false;
    }
}
