/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.StringUtil;

import javax.annotation.Nonnull;

import static dan200.computercraft.api.lua.ArgumentHelper.getInt;
import static dan200.computercraft.api.lua.ArgumentHelper.optString;

public class PrinterPeripheral implements IPeripheral
{
    private final TilePrinter printer;

    public PrinterPeripheral( TilePrinter printer )
    {
        this.printer = printer;
    }

    @Nonnull
    @Override
    public String getType()
    {
        return "printer";
    }

    // FIXME: There's a theoretical race condition here between getCurrentPage and then using the page. Ideally
    //  we'd lock on the page, consume it, and unlock.

    // FIXME: None of our page modification functions actually mark the tile as dirty, so the page may not be
    //  persisted correctly.

    public final void write( Object[] args ) throws LuaException
    {
        String text = args.length > 0 && args[0] != null ? args[0].toString() : "";
        Terminal page = getCurrentPage();
        page.write( text );
        page.setCursorPos( page.getCursorX() + text.length(), page.getCursorY() );
    }

    @LuaFunction
    public final Object[] getCursorPos( Object[] args ) throws LuaException
    {
        Terminal page = getCurrentPage();
        int x = page.getCursorX();
        int y = page.getCursorY();
        return new Object[] { x + 1, y + 1 };
    }

    @LuaFunction
    public final void setCursorPos( Object[] args ) throws LuaException
    {
        int x = getInt( args, 0 ) - 1;
        int y = getInt( args, 1 ) - 1;
        Terminal page = getCurrentPage();
        page.setCursorPos( x, y );
    }

    @LuaFunction
    public final Object[] getPageSize( Object[] args ) throws LuaException
    {
        Terminal page = getCurrentPage();
        int width = page.getWidth();
        int height = page.getHeight();
        return new Object[] { width, height };
    }

    @LuaFunction( mainThread = true )
    public final boolean newPage()
    {
        return printer.startNewPage();
    }

    @LuaFunction( mainThread = true )
    public final boolean endPage() throws LuaException
    {
        getCurrentPage();
        return printer.endCurrentPage();
    }

    @LuaFunction
    public final void setPageTitle( Object[] args ) throws LuaException
    {
        String title = optString( args, 0, "" );
        getCurrentPage();
        printer.setPageTitle( StringUtil.normaliseLabel( title ) );
    }

    @LuaFunction
    public final int getInkLevel()
    {
        return printer.getInkLevel();
    }

    @LuaFunction
    public final int getPaperLevel()
    {
        return printer.getPaperLevel();
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return other instanceof PrinterPeripheral && ((PrinterPeripheral) other).printer == printer;
    }

    @Nonnull
    @Override
    public Object getTarget()
    {
        return printer;
    }

    @Nonnull
    private Terminal getCurrentPage() throws LuaException
    {
        Terminal currentPage = printer.getCurrentPage();
        if( currentPage == null ) throw new LuaException( "Page not started" );
        return currentPage;
    }
}
