/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.terminal;

import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public class Terminal
{
    private static final String base16 = "0123456789abcdef";

    private int m_cursorX = 0;
    private int m_cursorY = 0;
    private boolean m_cursorBlink = false;
    private int m_cursorColour = 0;
    private int m_cursorBackgroundColour = 15;

    private int m_width;
    private int m_height;

    private TextBuffer[] m_text;
    private TextBuffer[] m_textColour;
    private TextBuffer[] m_backgroundColour;

    private final Palette m_palette = new Palette();

    private boolean m_changed = false;
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

        m_text = new TextBuffer[m_height];
        m_textColour = new TextBuffer[m_height];
        m_backgroundColour = new TextBuffer[m_height];
        for( int i = 0; i < m_height; i++ )
        {
            m_text[i] = new TextBuffer( ' ', m_width );
            m_textColour[i] = new TextBuffer( base16.charAt( m_cursorColour ), m_width );
            m_backgroundColour[i] = new TextBuffer( base16.charAt( m_cursorBackgroundColour ), m_width );
        }
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
        TextBuffer[] oldText = m_text;
        TextBuffer[] oldTextColour = m_textColour;
        TextBuffer[] oldBackgroundColour = m_backgroundColour;

        m_width = width;
        m_height = height;

        m_text = new TextBuffer[m_height];
        m_textColour = new TextBuffer[m_height];
        m_backgroundColour = new TextBuffer[m_height];
        for( int i = 0; i < m_height; i++ )
        {
            if( i >= oldHeight )
            {
                m_text[i] = new TextBuffer( ' ', m_width );
                m_textColour[i] = new TextBuffer( base16.charAt( m_cursorColour ), m_width );
                m_backgroundColour[i] = new TextBuffer( base16.charAt( m_cursorBackgroundColour ), m_width );
            }
            else if( m_width == oldWidth )
            {
                m_text[i] = oldText[i];
                m_textColour[i] = oldTextColour[i];
                m_backgroundColour[i] = oldBackgroundColour[i];
            }
            else
            {
                m_text[i] = new TextBuffer( ' ', m_width );
                m_textColour[i] = new TextBuffer( base16.charAt( m_cursorColour ), m_width );
                m_backgroundColour[i] = new TextBuffer( base16.charAt( m_cursorBackgroundColour ), m_width );
                m_text[i].write( oldText[i] );
                m_textColour[i].write( oldTextColour[i] );
                m_backgroundColour[i].write( oldBackgroundColour[i] );
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

    public void setTextColour( int colour )
    {
        if( m_cursorColour != colour )
        {
            m_cursorColour = colour;
            setChanged();
        }
    }

    public void setBackgroundColour( int colour )
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

    public synchronized void blit( String text, String textColour, String backgroundColour )
    {
        int x = m_cursorX;
        int y = m_cursorY;
        if( y >= 0 && y < m_height )
        {
            m_text[y].write( text, x );
            m_textColour[y].write( textColour, x );
            m_backgroundColour[y].write( backgroundColour, x );
            setChanged();
        }
    }

    public synchronized void write( String text )
    {
        int x = m_cursorX;
        int y = m_cursorY;
        if( y >= 0 && y < m_height )
        {
            m_text[y].write( text, x );
            m_textColour[y].fill( base16.charAt( m_cursorColour ), x, x + text.length() );
            m_backgroundColour[y].fill( base16.charAt( m_cursorBackgroundColour ), x, x + text.length() );
            setChanged();
        }
    }

    public synchronized void scroll( int yDiff )
    {
        if( yDiff != 0 )
        {
            TextBuffer[] newText = new TextBuffer[m_height];
            TextBuffer[] newTextColour = new TextBuffer[m_height];
            TextBuffer[] newBackgroundColour = new TextBuffer[m_height];
            for( int y = 0; y < m_height; y++ )
            {
                int oldY = y + yDiff;
                if( oldY >= 0 && oldY < m_height )
                {
                    newText[y] = m_text[oldY];
                    newTextColour[y] = m_textColour[oldY];
                    newBackgroundColour[y] = m_backgroundColour[oldY];
                }
                else
                {
                    newText[y] = new TextBuffer( ' ', m_width );
                    newTextColour[y] = new TextBuffer( base16.charAt( m_cursorColour ), m_width );
                    newBackgroundColour[y] = new TextBuffer( base16.charAt( m_cursorBackgroundColour ), m_width );
                }
            }
            m_text = newText;
            m_textColour = newTextColour;
            m_backgroundColour = newBackgroundColour;
            setChanged();
        }
    }

    public synchronized void clear()
    {
        for( int y = 0; y < m_height; y++ )
        {
            m_text[y].fill( ' ' );
            m_textColour[y].fill( base16.charAt( m_cursorColour ) );
            m_backgroundColour[y].fill( base16.charAt( m_cursorBackgroundColour ) );
        }
        setChanged();
    }

    public synchronized void clearLine()
    {
        int y = m_cursorY;
        if( y >= 0 && y < m_height )
        {
            m_text[y].fill( ' ' );
            m_textColour[y].fill( base16.charAt( m_cursorColour ) );
            m_backgroundColour[y].fill( base16.charAt( m_cursorBackgroundColour ) );
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

    public synchronized void write( PacketBuffer buffer )
    {
        buffer.writeInt( m_cursorX );
        buffer.writeInt( m_cursorY );
        buffer.writeBoolean( m_cursorBlink );
        buffer.writeByte( m_cursorBackgroundColour << 4 | m_cursorColour );

        for( int y = 0; y < m_height; y++ )
        {
            TextBuffer text = m_text[y];
            TextBuffer textColour = m_textColour[y];
            TextBuffer backColour = m_backgroundColour[y];

            for( int x = 0; x < m_width; x++ )
            {
                buffer.writeByte( text.charAt( x ) & 0xFF );
                buffer.writeByte( getColour(
                    backColour.charAt( x ), Colour.Black ) << 4 |
                    getColour( textColour.charAt( x ), Colour.White )
                );
            }
        }

        m_palette.write( buffer );
    }

    public synchronized void read( PacketBuffer buffer )
    {
        m_cursorX = buffer.readInt();
        m_cursorY = buffer.readInt();
        m_cursorBlink = buffer.readBoolean();

        byte cursorColour = buffer.readByte();
        m_cursorBackgroundColour = (cursorColour >> 4) & 0xF;
        m_cursorColour = cursorColour & 0xF;

        for( int y = 0; y < m_height; y++ )
        {
            TextBuffer text = m_text[y];
            TextBuffer textColour = m_textColour[y];
            TextBuffer backColour = m_backgroundColour[y];

            for( int x = 0; x < m_width; x++ )
            {
                text.setChar( x, (char) (buffer.readByte() & 0xFF) );

                byte colour = buffer.readByte();
                backColour.setChar( x, base16.charAt( (colour >> 4) & 0xF ) );
                textColour.setChar( x, base16.charAt( colour & 0xF ) );
            }
        }

        m_palette.read( buffer );
        setChanged();
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

        m_palette.writeToNBT( nbt );
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

        m_palette.readFromNBT( nbt );
        setChanged();
    }

    public static int getColour( char c, Colour def )
    {
        if( c >= '0' && c <= '9' ) return c - '0';
        if( c >= 'a' && c <= 'f' ) return c - 'a' + 10;
        return 15 - def.ordinal();
    }
}
