/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.terminal;

import dan200.computercraft.shared.util.Palette;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Arrays;

public class Terminal
{
    private static final String base16 = "0123456789abcdef";
    private static final byte BLANK_CHAR = (byte) ' ';

    private int m_cursorX;
    private int m_cursorY;
    private boolean m_cursorBlink;
    private byte m_cursorColour;
    private byte m_cursorBackgroundColour;

    private int m_width;
    private int m_height;

    private byte[] m_text;
    private byte[] m_textColour;
    private byte[] m_backgroundColour;

    private final Palette m_palette;

    private boolean m_changed;
    private final Runnable onChanged;

    public Terminal( int width, int height )
    {
        this( width, height, null );
    }

    public Terminal( int width, int height, Runnable changedCallback )
    {
        m_width = width;
        m_height = height;
        onChanged = changedCallback;

        m_cursorColour = 0;
        m_cursorBackgroundColour = 15;

        m_text = new byte[m_width * m_height];
        m_textColour = new byte[m_width * m_height];
        m_backgroundColour = new byte[m_width * m_height];

        Arrays.fill(m_text, BLANK_CHAR);
        Arrays.fill(m_textColour, m_cursorColour);
        Arrays.fill(m_backgroundColour, m_cursorBackgroundColour);

        m_cursorX = 0;
        m_cursorY = 0;
        m_cursorBlink = false;

        m_changed = false;

        m_palette = new Palette();
    }

    public synchronized void reset()
    {
        m_cursorColour = 0;
        m_cursorBackgroundColour = 15;
        m_cursorX = 0;
        m_cursorY = 0;
        m_cursorBlink = false;
        clear();
        setChanged();
        m_palette.resetColours();
    }

    public int getWidth()
    {
        return m_width;
    }

    public int getHeight()
    {
        return m_height;
    }

    public synchronized void resize( int width, int height )
    {
        if( width == m_width && height == m_height )
        {
            return;
        }

        int oldHeight = m_height;
        int oldWidth = m_width;
        byte[] oldText = m_text;
        byte[] oldTextColour = m_textColour;
        byte[] oldBackgroundColour = m_backgroundColour;

        m_width = width;
        m_height = height;

        m_text = new byte[m_width * m_height];
        m_textColour = new byte[m_width * m_height];
        m_backgroundColour = new byte[m_width * m_height];
        for( int y = 0; y < m_height; y++ )
        {
            int lineStart = y * m_width;
            int lineEnd = (y + 1) * m_width;

            if( y >= oldHeight ) // fill new height of the screen with empty lines
            {
                Arrays.fill(m_text, lineStart, lineEnd, BLANK_CHAR);
                Arrays.fill(m_textColour, lineStart, lineEnd, m_cursorColour);
                Arrays.fill(m_backgroundColour, lineStart, lineEnd, m_cursorBackgroundColour);
            }
            else // copy old content onto new terminal
            {
                int copySize = Math.min(oldWidth, m_width);
                int srcFrom = y * oldWidth;
                System.arraycopy(oldText, srcFrom, m_text, lineStart, copySize);
                System.arraycopy(oldTextColour, srcFrom, m_textColour, lineStart, copySize);
                System.arraycopy(oldBackgroundColour, srcFrom, m_backgroundColour, lineStart, copySize);

                if( m_width > oldWidth ) // fill extra space on each line if needed
                {
                    int fillFrom = srcFrom + copySize;
                    int fillTo = fillFrom + (m_width - oldWidth);
                    Arrays.fill(m_text, fillFrom, fillTo, BLANK_CHAR);
                    Arrays.fill(m_textColour, fillFrom, fillTo, m_cursorColour);
                    Arrays.fill(m_backgroundColour, fillFrom, fillTo, m_cursorBackgroundColour);
                }
            }
        }
        setChanged();
    }

    public void setCursorPos( int x, int y )
    {
        if( m_cursorX != x || m_cursorY != y )
        {
            m_cursorX = x;
            m_cursorY = y;
            setChanged();
        }
    }

    public void setCursorBlink( boolean blink )
    {
        if( m_cursorBlink != blink )
        {
            m_cursorBlink = blink;
            setChanged();
        }
    }

    public void setTextColour( byte colour )
    {
        if( m_cursorColour != colour )
        {
            m_cursorColour = colour;
            setChanged();
        }
    }

    public void setBackgroundColour( byte colour )
    {
        if( m_cursorBackgroundColour != colour )
        {
            m_cursorBackgroundColour = colour;
            setChanged();
        }
    }

    public int getCursorX()
    {
        return m_cursorX;
    }

    public int getCursorY()
    {
        return m_cursorY;
    }

    public boolean getCursorBlink()
    {
        return m_cursorBlink;
    }

    public int getTextColour()
    {
        return m_cursorColour;
    }

    public int getBackgroundColour()
    {
        return m_cursorBackgroundColour;
    }

    public Palette getPalette()
    {
        return m_palette;
    }

    public synchronized void blit( byte[] text, byte[] textColour, byte[] backgroundColour )
    {
        int x = m_cursorX;
        int y = m_cursorY;
        if( y >= 0 && y < m_height )
        {
            int copySize = Math.min(text.length, m_width);
            int dstFrom = (y * m_width) + x;

            System.arraycopy(text, 0, m_text, dstFrom, copySize);
            System.arraycopy(textColour, 0, m_textColour, dstFrom, copySize);
            System.arraycopy(backgroundColour, 0, m_backgroundColour, dstFrom, copySize);

            setChanged();
        }
    }

    public synchronized void write( byte[] text )
    {
        int x = m_cursorX;
        int y = m_cursorY;
        if( y >= 0 && y < m_height )
        {
            int copySize = Math.min(text.length, m_width);
            int dstFrom = (y * m_width) + x;

            System.arraycopy(text, 0, m_text, dstFrom, copySize);
            Arrays.fill(m_textColour, dstFrom, dstFrom + copySize, m_cursorColour);
            Arrays.fill(m_backgroundColour, dstFrom, dstFrom + copySize, m_cursorBackgroundColour);

            setChanged();
        }
    }

    public synchronized void scroll( int yDiff )
    {
        int absDiff = Math.abs(yDiff);
        if (absDiff >= m_height) { // just reset the entire terminal
            clear();
        } else if (yDiff != 0) {
            int charDiff = yDiff * m_width;

            int srcPos = Math.max(0, charDiff);
            int dstPos = Math.max(0, -charDiff);
            int copySize = m_width * (m_height - absDiff);

            int fillStart = yDiff > 0 ? m_width * (m_height - yDiff) : 0;
            int fillEnd = fillStart + Math.abs(charDiff);

            // 'move' the characters around (it's safe to arraycopy into itself)
            System.arraycopy(m_text, srcPos, m_text, dstPos, copySize);
            System.arraycopy(m_textColour, srcPos, m_textColour, dstPos, copySize);
            System.arraycopy(m_backgroundColour, srcPos, m_backgroundColour, dstPos, copySize);

            // fill the remaining space
            Arrays.fill(m_text, fillStart, fillEnd, BLANK_CHAR);
            Arrays.fill(m_textColour, fillStart, fillEnd, BLANK_CHAR);
            Arrays.fill(m_backgroundColour, fillStart, fillEnd, BLANK_CHAR);

            setChanged();
        }
    }

    public synchronized void clear()
    {
        Arrays.fill(m_text, BLANK_CHAR);
        Arrays.fill(m_textColour, m_cursorColour);
        Arrays.fill(m_backgroundColour, m_cursorBackgroundColour);

        setChanged();
    }

    public synchronized void clearLine()
    {
        int y = m_cursorY;
        if( y >= 0 && y < m_height )
        {
            int fillStart = y * m_width;
            int fillEnd = (y + 1) * m_height;

            Arrays.fill(m_text, fillStart, fillEnd, BLANK_CHAR);
            Arrays.fill(m_textColour, fillStart, fillEnd, m_cursorColour);
            Arrays.fill(m_backgroundColour, fillStart, fillEnd, m_cursorBackgroundColour);

            setChanged();
        }
    }

    public synchronized TextBuffer getLine( int y )
    {
        if( y >= 0 && y < m_height )
        {
            return m_text[y];
        }
        return null;
    }

    public synchronized void setLine( int y, String text, String textColour, String backgroundColour )
    {
        m_text[y].write( text );
        m_textColour[y].write( textColour );
        m_backgroundColour[y].write( backgroundColour );
        setChanged();
    }

    public synchronized TextBuffer getTextColourLine( int y )
    {
        if( y >= 0 && y < m_height )
        {
            return m_textColour[y];
        }
        return null;
    }

    public synchronized TextBuffer getBackgroundColourLine( int y )
    {
        if( y >= 0 && y < m_height )
        {
            return m_backgroundColour[y];
        }
        return null;
    }

    /**
     * Determine whether this terminal has changed.
     *
     * @return If this terminal is dirty.
     * @deprecated All {@code *Changed()} methods are deprecated: one should pass in a callback
     * instead.
     */
    @Deprecated
    public final boolean getChanged()
    {
        return m_changed;
    }

    public final void setChanged()
    {
        m_changed = true;
        if( onChanged != null ) onChanged.run();
    }

    public final void clearChanged()
    {
        m_changed = false;
    }

    public synchronized NBTTagCompound writeToNBT( NBTTagCompound nbt )
    {
        nbt.setInteger( "term_cursorX", m_cursorX );
        nbt.setInteger( "term_cursorY", m_cursorY );
        nbt.setBoolean( "term_cursorBlink", m_cursorBlink );
        nbt.setInteger( "term_textColour", m_cursorColour );
        nbt.setInteger( "term_bgColour", m_cursorBackgroundColour );
        for( int n = 0; n < m_height; n++ )
        {
            nbt.setString( "term_text_" + n, m_text[n].toString() );
            nbt.setString( "term_textColour_" + n, m_textColour[n].toString() );
            nbt.setString( "term_textBgColour_" + n, m_backgroundColour[n].toString() );
        }
        if( m_palette != null )
        {
            m_palette.writeToNBT( nbt );
        }
        return nbt;
    }

    public synchronized void readFromNBT( NBTTagCompound nbt )
    {
        m_cursorX = nbt.getInteger( "term_cursorX" );
        m_cursorY = nbt.getInteger( "term_cursorY" );
        m_cursorBlink = nbt.getBoolean( "term_cursorBlink" );
        m_cursorColour = nbt.getInteger( "term_textColour" );
        m_cursorBackgroundColour = nbt.getInteger( "term_bgColour" );

        for( int n = 0; n < m_height; n++ )
        {
            m_text[n].fill( ' ' );
            if( nbt.hasKey( "term_text_" + n ) )
            {
                m_text[n].write( nbt.getString( "term_text_" + n ) );
            }
            m_textColour[n].fill( base16.charAt( m_cursorColour ) );
            if( nbt.hasKey( "term_textColour_" + n ) )
            {
                m_textColour[n].write( nbt.getString( "term_textColour_" + n ) );
            }
            m_backgroundColour[n].fill( base16.charAt( m_cursorBackgroundColour ) );
            if( nbt.hasKey( "term_textBgColour_" + n ) )
            {
                m_backgroundColour[n].write( nbt.getString( "term_textBgColour_" + n ) );
            }
        }
        if( m_palette != null )
        {
            m_palette.readFromNBT( nbt );
        }
        setChanged();
    }
}
