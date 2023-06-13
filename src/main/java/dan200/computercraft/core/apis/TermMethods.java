// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.apis;

import dan200.computer.core.Terminal;
import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.util.Colour;

/**
 * A base class for all objects which interact with a terminal. Namely the {@link TermAPI} and monitors.
 *
 * @cc.module term.Redirect
 */
public abstract class TermMethods {
    private static int getHighestBit(int group) {
        // Equivalent to log2(group) - 1.
        return 32 - Integer.numberOfLeadingZeros(group);
    }

    protected abstract boolean isColour();

    protected abstract Terminal getTerminal() throws LuaException;

    /**
     * Write {@code text} at the current cursor position, moving the cursor to the end of the text.
     * <p>
     * Unlike functions like {@code write} and {@code print}, this does not wrap the text - it simply copies the
     * text to the current terminal line.
     *
     * @param textA The text to write.
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void write(Coerced<String> textA) throws LuaException {
        String text = textA.value();
        Terminal terminal = getTerminal();
        synchronized (terminal) {
            terminal.write(text);
            terminal.setCursorPos(terminal.getCursorX() + text.length(), terminal.getCursorY());
        }
    }

    /**
     * Move all positions up (or down) by {@code y} pixels.
     * <p>
     * Every pixel in the terminal will be replaced by the line {@code y} pixels below it. If {@code y} is negative, it
     * will copy pixels from above instead.
     *
     * @param y The number of lines to move up by. This may be a negative number.
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void scroll(int y) throws LuaException {
        getTerminal().scroll(y);
    }

    /**
     * Get the position of the cursor.
     *
     * @return The cursor's position.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.treturn number The x position of the cursor.
     * @cc.treturn number The y position of the cursor.
     */
    @LuaFunction
    public final Object[] getCursorPos() throws LuaException {
        Terminal terminal = getTerminal();
        return new Object[]{ terminal.getCursorX() + 1, terminal.getCursorY() + 1 };
    }

    /**
     * Set the position of the cursor. {@link #write(Coerced) terminal writes} will begin from this position.
     *
     * @param x The new x position of the cursor.
     * @param y The new y position of the cursor.
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void setCursorPos(int x, int y) throws LuaException {
        Terminal terminal = getTerminal();
        synchronized (terminal) {
            terminal.setCursorPos(x - 1, y - 1);
        }
    }

    /**
     * Checks if the cursor is currently blinking.
     *
     * @return If the cursor is blinking.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.since 1.80pr1.9
     */
    @LuaFunction
    public final boolean getCursorBlink() throws LuaException {
        return getTerminal().getCursorBlink();
    }

    /**
     * Sets whether the cursor should be visible (and blinking) at the current {@link #getCursorPos() cursor position}.
     *
     * @param blink Whether the cursor should blink.
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void setCursorBlink(boolean blink) throws LuaException {
        Terminal terminal = getTerminal();
        synchronized (terminal) {
            terminal.setCursorBlink(blink);
        }
    }

    /**
     * Get the size of the terminal.
     *
     * @return The terminal's size.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.treturn number The terminal's width.
     * @cc.treturn number The terminal's height.
     */
    @LuaFunction
    public final Object[] getSize() throws LuaException {
        Terminal terminal = getTerminal();
        return new Object[]{ terminal.getWidth(), terminal.getHeight() };
    }

    /**
     * Clears the terminal, filling it with the {@link #getBackgroundColour() current background colour}.
     *
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void clear() throws LuaException {
        getTerminal().clear();
    }

    /**
     * Clears the line the cursor is currently on, filling it with the {@link #getBackgroundColour() current background
     * colour}.
     *
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void clearLine() throws LuaException {
        getTerminal().clearLine();
    }

    /**
     * Return the colour that new text will be written as.
     *
     * @return The current text colour.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.see colors For a list of colour constants, returned by this function.
     * @cc.since 1.74
     */
    @LuaFunction({ "getTextColour", "getTextColor" })
    public final int getTextColour() throws LuaException {
        return encodeColour(getTerminal().getTextColour());
    }

    /**
     * Set the colour that new text will be written as.
     *
     * @param colourArg The new text colour.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.see colors For a list of colour constants.
     * @cc.since 1.45
     * @cc.changed 1.80pr1 Standard computers can now use all 16 colors, being changed to grayscale on screen.
     */
    @LuaFunction({ "setTextColour", "setTextColor" })
    public final void setTextColour(int colourArg) throws LuaException {
        int colour = parseColour(colourArg);
        Terminal terminal = getTerminal();
        synchronized (terminal) {
            terminal.setTextColour(colour);
        }
    }

    /**
     * Return the current background colour. This is used when {@link #write writing text} and {@link #clear clearing}
     * the terminal.
     *
     * @return The current background colour.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.see colors For a list of colour constants, returned by this function.
     * @cc.since 1.74
     */
    @LuaFunction({ "getBackgroundColour", "getBackgroundColor" })
    public final int getBackgroundColour() throws LuaException {
        return encodeColour(getTerminal().getBackgroundColour());
    }

    /**
     * Set the current background colour. This is used when {@link #write writing text} and {@link #clear clearing} the
     * terminal.
     *
     * @param colourArg The new background colour.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.see colors For a list of colour constants.
     * @cc.since 1.45
     * @cc.changed 1.80pr1 Standard computers can now use all 16 colors, being changed to grayscale on screen.
     */
    @LuaFunction({ "setBackgroundColour", "setBackgroundColor" })
    public final void setBackgroundColour(int colourArg) throws LuaException {
        int colour = parseColour(colourArg);
        Terminal terminal = getTerminal();
        synchronized (terminal) {
            terminal.setBackgroundColour(colour);
        }
    }

    /**
     * Determine if this terminal supports colour.
     * <p>
     * Terminals which do not support colour will still allow writing coloured text/backgrounds, but it will be
     * displayed in greyscale.
     *
     * @return Whether this terminal supports colour.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.since 1.45
     */
    @LuaFunction({ "isColour", "isColor" })
    public final boolean getIsColour() throws LuaException {
        return isColour();
    }

    /**
     * Writes {@code text} to the terminal with the specific foreground and background colours.
     * <p>
     * As with {@link #write(Coerced)}, the text will be written at the current cursor location, with the cursor
     * moving to the end of the text.
     * <p>
     * {@code textColour} and {@code backgroundColour} must both be strings the same length as {@code text}. All
     * characters represent a single hexadecimal digit, which is converted to one of CC's colours. For instance,
     * {@code "a"} corresponds to purple.
     *
     * @param text             The text to write.
     * @param textColour       The corresponding text colours.
     * @param backgroundColour The corresponding background colours.
     * @throws LuaException If the three inputs are not the same length.
     * @cc.see colors For a list of colour constants, and their hexadecimal values.
     * @cc.since 1.74
     * @cc.changed 1.80pr1 Standard computers can now use all 16 colors, being changed to grayscale on screen.
     * @cc.usage Prints "Hello, world!" in rainbow text.
     * <pre>{@code
     * term.blit("Hello, world!","01234456789ab","0000000000000")
     * }</pre>
     */
    @LuaFunction
    public final void blit(Coerced<String> text, Coerced<String> textColour, Coerced<String> backgroundColour) throws LuaException {
        if (textColour.value().length() != text.value().length() || backgroundColour.value().length() != text.value().length()) {
            throw new LuaException("Arguments must be the same length");
        }

        Terminal terminal = getTerminal();
        synchronized (terminal) {
            blit(terminal, text.value(), textColour.value(), backgroundColour.value());
            terminal.setCursorPos(terminal.getCursorX() + text.value().length(), terminal.getCursorY());
        }
    }

    /**
     * Set the palette for a specific colour.
     * <p>
     * ComputerCraft's palette system allows you to change how a specific colour should be displayed. For instance, you
     * can make @{colors.red} <em>more red</em> by setting its palette to #FF0000. This does now allow you to draw more
     * colours - you are still limited to 16 on the screen at one time - but you can change <em>which</em> colours are
     * used.
     *
     * @param args The new palette values.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.tparam [1] number index The colour whose palette should be changed.
     * @cc.tparam number colour A 24-bit integer representing the RGB value of the colour. For instance the integer
     * `0xFF0000` corresponds to the colour #FF0000.
     * @cc.tparam [2] number index The colour whose palette should be changed.
     * @cc.tparam number r The intensity of the red channel, between 0 and 1.
     * @cc.tparam number g The intensity of the green channel, between 0 and 1.
     * @cc.tparam number b The intensity of the blue channel, between 0 and 1.
     * @cc.usage Change the @{colors.red|red colour} from the default #CC4C4C to #FF0000.
     * <pre>{@code
     * term.setPaletteColour(colors.red, 0xFF0000)
     * term.setTextColour(colors.red)
     * print("Hello, world!")
     * }</pre>
     * @cc.usage As above, but specifying each colour channel separately.
     * <pre>{@code
     * term.setPaletteColour(colors.red, 1, 0, 0)
     * term.setTextColour(colors.red)
     * print("Hello, world!")
     * }</pre>
     * @cc.see colors.unpackRGB To convert from the 24-bit format to three separate channels.
     * @cc.see colors.packRGB To convert from three separate channels to the 24-bit format.
     * @cc.since 1.80pr1
     */
    @LuaFunction({ "setPaletteColour", "setPaletteColor" })
    public final void setPaletteColour(IArguments args) throws LuaException {
        // No-op
        parseColour(args.getInt(0));
        if (args.count() == 2) {
            args.getInt(1);
        } else {
            args.getFiniteDouble(1);
            args.getFiniteDouble(2);
            args.getFiniteDouble(3);
        }
    }

    /**
     * Get the current palette for a specific colour.
     *
     * @param colour The colour whose palette should be fetched.
     * @return The resulting colour.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.treturn number The red channel, will be between 0 and 1.
     * @cc.treturn number The green channel, will be between 0 and 1.
     * @cc.treturn number The blue channel, will be between 0 and 1.
     * @cc.since 1.80pr1
     */
    @LuaFunction({ "getPaletteColour", "getPaletteColor" })
    public final Object[] getPaletteColour(int colour) throws LuaException {
        Colour c = Colour.fromInt(parseColour(colour));
        return new Object[]{ c.getR(), c.getG(), c.getB() };
    }

    public static int parseColour(int colour) throws LuaException {
        if (colour <= 0) throw new LuaException("Colour out of range");
        colour = 16 - getHighestBit(colour);
        if (colour < 0 || colour > 15) throw new LuaException("Colour out of range");
        return colour;
    }

    public static int encodeColour(int colour) {
        return 1 << (15 - colour);
    }

    private static void blit(Terminal term, String text, String foreground, String background) {
        if (term.getCursorY() < 0 || term.getCursorY() >= term.getHeight()) return;
        int writeX = term.getCursorX();
        int spaceLeft = term.getWidth() - term.getCursorX();
        if (spaceLeft > term.getWidth() + text.length()) {
            return;
        }

        if (spaceLeft > term.getWidth()) {
            writeX = 0;
            text = text.substring(spaceLeft - term.getWidth());
            spaceLeft = term.getWidth();
        }

        text = text.replace('\t', ' ');
        if (spaceLeft > 0) {
            String oldLine = term.getLine(term.getCursorY());
            String oldColourLine = term.getColourLine(term.getCursorY());
            String oldTextLine = oldColourLine.substring(0, oldLine.length());
            String oldBackgroundLine = oldColourLine.substring(oldLine.length(), 2 * oldLine.length());
            StringBuilder newLine = new StringBuilder();
            StringBuilder newTextLine = new StringBuilder();
            StringBuilder newBackgroundLine = new StringBuilder();
            newLine.append(oldLine, 0, writeX);
            newTextLine.append(oldTextLine, 0, writeX);
            newBackgroundLine.append(oldBackgroundLine, 0, writeX);
            if (text.length() < spaceLeft) {
                newLine.append(text);
                for (int i = 0; i < text.length(); i++) newTextLine.append(foreground.charAt(i));
                for (int i = 0; i < text.length(); i++) newBackgroundLine.append(remapColour(background.charAt(i)));

                newLine.append(oldLine.substring(writeX + text.length()));
                newTextLine.append(oldTextLine.substring(writeX + text.length()));
                newBackgroundLine.append(oldBackgroundLine.substring(writeX + text.length()));
            } else {
                newLine.append(text, 0, spaceLeft);
                for (int i = 0; i < spaceLeft; i++) newTextLine.append(remapColour(foreground.charAt(i)));
                for (int i = 0; i < spaceLeft; i++) newBackgroundLine.append(remapColour(background.charAt(i)));
            }

            term.setLine(term.getCursorY(), newLine.toString(), newTextLine.append(newBackgroundLine).toString());
        }
    }

    private static final String COLOURS = "0123456789abcdef";

    /**
     * Remap a blit character to use the older format.
     *
     * @param c The colour to remap
     * @return The new character.
     */
    private static char remapColour(char c) {
        if (c >= '0' && c <= '9') return COLOURS.charAt(15 - (c - '0'));
        if (c >= 'a' && c <= 'f') return COLOURS.charAt(15 - (c - 'a' + 10));
        if (c >= 'A' && c <= 'F') return COLOURS.charAt(15 - (c - 'A' + 10));
        return ' ';
    }
}
