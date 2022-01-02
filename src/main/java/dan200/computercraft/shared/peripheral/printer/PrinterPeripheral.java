/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.util.StringUtil;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * The printer peripheral allows pages and books to be printed.
 *
 * @cc.module printer
 */
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

    /**
     * Writes text to the current page.
     *
     * @param arguments The values to write to the page.
     * @throws LuaException If any values couldn't be converted to a string, or if no page is started.
     * @cc.tparam string|number ... The values to write to the page.
     */
    @LuaFunction
    public final void write( IArguments arguments ) throws LuaException
    {
        String text = StringUtil.toString( arguments.get( 0 ) );
        Terminal page = getCurrentPage();
        page.write( text );
        page.setCursorPos( page.getCursorX() + text.length(), page.getCursorY() );
    }

    /**
     * Returns the current position of the cursor on the page.
     *
     * @return The position of the cursor.
     * @throws LuaException If a page isn't being printed.
     * @cc.treturn number The X position of the cursor.
     * @cc.treturn number The Y position of the cursor.
     */
    @LuaFunction
    public final Object[] getCursorPos() throws LuaException
    {
        Terminal page = getCurrentPage();
        int x = page.getCursorX();
        int y = page.getCursorY();
        return new Object[] { x + 1, y + 1 };
    }

    /**
     * Sets the position of the cursor on the page.
     *
     * @param x The X coordinate to set the cursor at.
     * @param y The Y coordinate to set the cursor at.
     * @throws LuaException If a page isn't being printed.
     */
    @LuaFunction
    public final void setCursorPos( int x, int y ) throws LuaException
    {
        Terminal page = getCurrentPage();
        page.setCursorPos( x - 1, y - 1 );
    }

    /**
     * Returns the size of the current page.
     *
     * @return The size of the page.
     * @throws LuaException If a page isn't being printed.
     * @cc.treturn number The width of the page.
     * @cc.treturn number The height of the page.
     */
    @LuaFunction
    public final Object[] getPageSize() throws LuaException
    {
        Terminal page = getCurrentPage();
        int width = page.getWidth();
        int height = page.getHeight();
        return new Object[] { width, height };
    }

    /**
     * Starts printing a new page.
     *
     * @return Whether a new page could be started.
     */
    @LuaFunction( mainThread = true )
    public final boolean newPage()
    {
        return printer.startNewPage();
    }

    /**
     * Finalizes printing of the current page and outputs it to the tray.
     *
     * @return Whether the page could be successfully finished.
     * @throws LuaException If a page isn't being printed.
     */
    @LuaFunction( mainThread = true )
    public final boolean endPage() throws LuaException
    {
        getCurrentPage();
        return printer.endCurrentPage();
    }

    /**
     * Sets the title of the current page.
     *
     * @param title The title to set for the page.
     * @throws LuaException If a page isn't being printed.
     */
    @LuaFunction
    public final void setPageTitle( Optional<String> title ) throws LuaException
    {
        getCurrentPage();
        printer.setPageTitle( StringUtil.normaliseLabel( title.orElse( "" ) ) );
    }

    /**
     * Returns the amount of ink left in the printer.
     *
     * @return The amount of ink available to print with.
     */
    @LuaFunction
    public final int getInkLevel()
    {
        return printer.getInkLevel();
    }

    /**
     * Returns the amount of paper left in the printer.
     *
     * @return The amount of paper available to print with.
     */
    @LuaFunction
    public final int getPaperLevel()
    {
        return printer.getPaperLevel();
    }

    @Override
    public boolean equals( IPeripheral other )
    {
        return this == other || (other instanceof PrinterPeripheral otherPrinter && otherPrinter.printer == printer);
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
