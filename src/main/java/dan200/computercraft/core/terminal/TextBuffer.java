/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.terminal;

public class TextBuffer
{
    private final char[] text;

    public TextBuffer( char c, int length )
    {
        text = new char[length];
        fill( c );
    }

    public TextBuffer( String text )
    {
        this.text = text.toCharArray();
    }

    public int length()
    {
        return text.length;
    }

    public void write( String text )
    {
        write( text, 0 );
    }

    public void write( String text, int start )
    {
        int pos = start;
        start = Math.max( start, 0 );
        int end = Math.min( start + text.length(), pos + text.length() );
        end = Math.min( end, this.text.length );
        for( int i = start; i < end; i++ )
        {
            this.text[i] = text.charAt( i - pos );
        }
    }

    public void write( TextBuffer text )
    {
        int end = Math.min( text.length(), this.text.length );
        for( int i = 0; i < end; i++ )
        {
            this.text[i] = text.charAt( i );
        }
    }

    public void fill( char c )
    {
        fill( c, 0, text.length );
    }

    public void fill( char c, int start, int end )
    {
        start = Math.max( start, 0 );
        end = Math.min( end, text.length );
        for( int i = start; i < end; i++ )
        {
            text[i] = c;
        }
    }

    public char charAt( int i )
    {
        return text[i];
    }

    public void setChar( int i, char c )
    {
        if( i >= 0 && i < text.length )
        {
            text[i] = c;
        }
    }

    @Override
    public String toString()
    {
        return new String( text );
    }
}
