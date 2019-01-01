/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.terminal.Terminal;

import javax.annotation.Nonnull;

import static dan200.computercraft.core.apis.ArgumentHelper.getInt;
import static dan200.computercraft.core.apis.ArgumentHelper.optString;

public class PrinterPeripheral implements IPeripheral
{
    private final TilePrinter m_printer;

    public PrinterPeripheral( TilePrinter printer )
    {
        m_printer = printer;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "printer";
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "write",
            "setCursorPos",
            "getCursorPos",
            "getPageSize",
            "newPage",
            "endPage",
            "getInkLevel",
            "setPageTitle",
            "getPaperLevel",
        };
    }

    @Override
    public Object[] callMethod( @Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException
    {
        switch( method )
        {
            case 0:
            {
                // write
                String text;
                if( args.length > 0 && args[0] != null )
                {
                    text = args[0].toString();
                }
                else
                {
                    text = "";
                }

                Terminal page = getCurrentPage();
                page.write( text );
                page.setCursorPos( page.getCursorX() + text.length(), page.getCursorY() );
                return null;
            }
            case 1:
            {
                // setCursorPos
                int x = getInt( args, 0 ) - 1;
                int y = getInt( args, 1 ) - 1;
                Terminal page = getCurrentPage();
                page.setCursorPos( x, y );
                return null;
            }
            case 2:
            {
                // getCursorPos
                Terminal page = getCurrentPage();
                int x = page.getCursorX();
                int y = page.getCursorY();
                return new Object[] { x + 1, y + 1 };
            }
            case 3:
            {
                // getPageSize
                Terminal page = getCurrentPage();
                int width = page.getWidth();
                int height = page.getHeight();
                return new Object[] { width, height };
            }
            case 4:
            {
                // newPage
                return new Object[] { m_printer.startNewPage() };
            }
            case 5:
            {
                // endPage
                getCurrentPage();
                return new Object[] { m_printer.endCurrentPage() };
            }
            case 6:
            {
                // getInkLevel
                return new Object[] { m_printer.getInkLevel() };
            }
            case 7:
            {
                // setPageTitle
                String title = optString( args, 0, "" );
                getCurrentPage();
                m_printer.setPageTitle( title );
                return null;
            }
            case 8:
            {
                // getPaperLevel
                return new Object[] { m_printer.getPaperLevel() };
            }
            default:
            {
                return null;
            }
        }
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        if( other instanceof PrinterPeripheral )
        {
            PrinterPeripheral otherPrinter = (PrinterPeripheral) other;
            if( otherPrinter.m_printer == this.m_printer )
            {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return m_printer;
    }

    private Terminal getCurrentPage() throws LuaException
    {
        Terminal currentPage = m_printer.getCurrentPage();
        if( currentPage == null )
        {
            throw new LuaException( "Page not started" );
        }
        return currentPage;
    }
}
