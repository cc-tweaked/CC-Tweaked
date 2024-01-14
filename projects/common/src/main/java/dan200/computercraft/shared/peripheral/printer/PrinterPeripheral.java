// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.peripheral.printer;

import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.StringUtil;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The printer peripheral allows printing text onto pages. These pages can then be crafted together into printed pages
 * or books.
 * <p>
 * Printers require ink (one of the coloured dyes) and paper in order to function. Once loaded, a new page can be
 * started with {@link #newPage()}. Then the printer can be used similarly to a normal terminal; {@linkplain
 * #write(Coerced) text can be written}, and {@linkplain #setCursorPos(int, int) the cursor moved}. Once all text has
 * been printed, {@link #endPage()} should be called to finally print the page.
 * <p>
 * ## Recipes
 * <div class="recipe-container">
 *     <mc-recipe recipe="computercraft:printer"></mc-recipe>
 *     <mc-recipe recipe="computercraft:printed_pages"></mc-recipe>
 *     <mc-recipe recipe="computercraft:printed_book"></mc-recipe>
 * </div>
 *
 * @cc.usage Print a page titled "Hello" with a small message on it.
 *
 * <pre>{@code
 * local printer = peripheral.find("printer")
 *
 * -- Start a new page, or print an error.
 * if not printer.newPage() then
 *   error("Cannot start a new page. Do you have ink and paper?")
 * end
 *
 * -- Write to the page
 * printer.setPageTitle("Hello")
 * printer.write("This is my first page")
 * printer.setCursorPos(1, 3)
 * printer.write("This is two lines below.")
 *
 * -- And finally print the page!
 * if not printer.endPage() then
 *   error("Cannot end the page. Is there enough space?")
 * end
 * }</pre>
 * @cc.module printer
 * @cc.see cc.strings.wrap To wrap text before printing it.
 */
public class PrinterPeripheral implements IPeripheral {
    private final PrinterBlockEntity printer;

    public PrinterPeripheral(PrinterBlockEntity printer) {
        this.printer = printer;
    }

    @Override
    public String getType() {
        return "printer";
    }

    // FIXME: There's a theoretical race condition here between getCurrentPage and then using the page. Ideally
    //  we'd lock on the page, consume it, and unlock.

    // FIXME: None of our page modification functions actually mark the tile as dirty, so the page may not be
    //  persisted correctly.

    /**
     * Writes text to the current page.
     *
     * @param textA The value to write to the page.
     * @throws LuaException If any values couldn't be converted to a string, or if no page is started.
     */
    @LuaFunction
    public final void write(Coerced<String> textA) throws LuaException {
        var text = textA.value();
        var page = getCurrentPage();
        page.write(text);
        page.setCursorPos(page.getCursorX() + text.length(), page.getCursorY());
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
    public final Object[] getCursorPos() throws LuaException {
        var page = getCurrentPage();
        var x = page.getCursorX();
        var y = page.getCursorY();
        return new Object[]{ x + 1, y + 1 };
    }

    /**
     * Sets the position of the cursor on the page.
     *
     * @param x The X coordinate to set the cursor at.
     * @param y The Y coordinate to set the cursor at.
     * @throws LuaException If a page isn't being printed.
     */
    @LuaFunction
    public final void setCursorPos(int x, int y) throws LuaException {
        var page = getCurrentPage();
        page.setCursorPos(x - 1, y - 1);
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
    public final Object[] getPageSize() throws LuaException {
        var page = getCurrentPage();
        var width = page.getWidth();
        var height = page.getHeight();
        return new Object[]{ width, height };
    }

    /**
     * Starts printing a new page.
     *
     * @return Whether a new page could be started.
     */
    @LuaFunction(mainThread = true)
    public final boolean newPage() {
        return printer.startNewPage();
    }

    /**
     * Finalizes printing of the current page and outputs it to the tray.
     *
     * @return Whether the page could be successfully finished.
     * @throws LuaException If a page isn't being printed.
     */
    @LuaFunction(mainThread = true)
    public final boolean endPage() throws LuaException {
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
    public final void setPageTitle(Optional<String> title) throws LuaException {
        getCurrentPage();
        printer.setPageTitle(title.map(StringUtil::normaliseLabel).orElse(""));
    }

    /**
     * Returns the amount of ink left in the printer.
     *
     * @return The amount of ink available to print with.
     */
    @LuaFunction
    public final int getInkLevel() {
        return printer.getInkLevel();
    }

    /**
     * Returns the amount of paper left in the printer.
     *
     * @return The amount of paper available to print with.
     */
    @LuaFunction
    public final int getPaperLevel() {
        return printer.getPaperLevel();
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other || (other instanceof PrinterPeripheral otherPrinter && otherPrinter.printer == printer);
    }

    @Override
    public Object getTarget() {
        return printer;
    }

    private Terminal getCurrentPage() throws LuaException {
        var currentPage = printer.getCurrentPage();
        if (currentPage == null) throw new LuaException("Page not started");
        return currentPage;
    }
}
