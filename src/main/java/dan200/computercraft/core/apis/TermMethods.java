/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
 *
 * @cc.module term.Redirect
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

    /**
     * Write {@code text} at the current cursor position, moving the cursor to the end of the text.
     *
     * Unlike functions like {@code write} and {@code print}, this does not wrap the text - it simply copies the
     * text to the current terminal line.
     *
     * @param arguments The text to write.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.param text The text to write.
     */
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

    /**
     * Move all positions up (or down) by {@code y} pixels.
     *
     * Every pixel in the terminal will be replaced by the line {@code y} pixels below it. If {@code y} is negative, it
     * will copy pixels from above instead.
     *
     * @param y The number of lines to move up by. This may be a negative number.
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void scroll( int y ) throws LuaException
    {
        getTerminal().scroll( y );
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
    public final Object[] getCursorPos() throws LuaException
    {
        Terminal terminal = getTerminal();
        return new Object[] { terminal.getCursorX() + 1, terminal.getCursorY() + 1 };
    }

    /**
     * Set the position of the cursor. {@link #write(IArguments) terminal writes} will begin from this position.
     *
     * @param x The new x position of the cursor.
     * @param y The new y position of the cursor.
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void setCursorPos( int x, int y ) throws LuaException
    {
        Terminal terminal = getTerminal();
        synchronized( terminal )
        {
            terminal.setCursorPos( x - 1, y - 1 );
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
    public final boolean getCursorBlink() throws LuaException
    {
        return getTerminal().getCursorBlink();
    }

    /**
     * Sets whether the cursor should be visible (and blinking) at the current {@link #getCursorPos() cursor position}.
     *
     * @param blink Whether the cursor should blink.
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void setCursorBlink( boolean blink ) throws LuaException
    {
        Terminal terminal = getTerminal();
        synchronized( terminal )
        {
            terminal.setCursorBlink( blink );
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
    public final Object[] getSize() throws LuaException
    {
        Terminal terminal = getTerminal();
        return new Object[] { terminal.getWidth(), terminal.getHeight() };
    }

    /**
     * Clears the terminal, filling it with the {@link #getBackgroundColour() current background colour}.
     *
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void clear() throws LuaException
    {
        getTerminal().clear();
    }

    /**
     * Clears the line the cursor is currently on, filling it with the {@link #getBackgroundColour() current background
     * colour}.
     *
     * @throws LuaException (hidden) If the terminal cannot be found.
     */
    @LuaFunction
    public final void clearLine() throws LuaException
    {
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
    @LuaFunction( { "getTextColour", "getTextColor" } )
    public final int getTextColour() throws LuaException
    {
        return encodeColour( getTerminal().getTextColour() );
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

    /**
     * Return the current background colour. This is used when {@link #write writing text} and {@link #clear clearing}
     * the terminal.
     *
     * @return The current background colour.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.see colors For a list of colour constants, returned by this function.
     * @cc.since 1.74
     */
    @LuaFunction( { "getBackgroundColour", "getBackgroundColor" } )
    public final int getBackgroundColour() throws LuaException
    {
        return encodeColour( getTerminal().getBackgroundColour() );
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

    /**
     * Determine if this terminal supports colour.
     *
     * Terminals which do not support colour will still allow writing coloured text/backgrounds, but it will be
     * displayed in greyscale.
     *
     * @return Whether this terminal supports colour.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.since 1.45
     */
    @LuaFunction( { "isColour", "isColor" } )
    public final boolean getIsColour() throws LuaException
    {
        return isColour();
    }

    /**
     * Writes {@code text} to the terminal with the specific foreground and background characters.
     *
     * As with {@link #write(IArguments)}, the text will be written at the current cursor location, with the cursor
     * moving to the end of the text.
     *
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

    /**
     * Set the palette for a specific colour.
     *
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

    /**
     * Get the current palette for a specific colour.
     *
     * @param colourArg The colour whose palette should be fetched.
     * @return The resulting colour.
     * @throws LuaException (hidden) If the terminal cannot be found.
     * @cc.treturn number The red channel, will be between 0 and 1.
     * @cc.treturn number The green channel, will be between 0 and 1.
     * @cc.treturn number The blue channel, will be between 0 and 1.
     * @cc.since 1.80pr1
     */
    @LuaFunction( { "getPaletteColour", "getPaletteColor" } )
    public final Object[] getPaletteColour( int colourArg ) throws LuaException
    {
        int colour = 15 - parseColour( colourArg );
        Terminal terminal = getTerminal();
        synchronized( terminal )
        {
            return ArrayUtils.toObject( terminal.getPalette().getColour( colour ) );
        }
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
        terminal.getPalette().setColour( colour, r, g, b );
        terminal.setChanged();
    }
}
