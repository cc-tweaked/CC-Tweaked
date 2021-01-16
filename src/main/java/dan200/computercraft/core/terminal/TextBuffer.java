/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.terminal;

public class TextBuffer
{
    private final char[] text;

    public TextBuffer( char c, int length )
    {
        text = new char[length];
        for( int i = 0; i < length; i++ )
        {
            text[i] = c;
        }
    }

    public TextBuffer( String text )
    {
        this( text, 1 );
    }

    public TextBuffer( String text, int repetitions )
    {
        int textLength = text.length();
        this.text = new char[textLength * repetitions];
        for( int i = 0; i < repetitions; i++ )
        {
            for( int j = 0; j < textLength; j++ )
            {
                this.text[j + i * textLength] = text.charAt( j );
            }
        }
    }

    public TextBuffer( TextBuffer text )
    {
        this( text, 1 );
    }

    public TextBuffer( TextBuffer text, int repetitions )
    {
        int textLength = text.length();
        this.text = new char[textLength * repetitions];
        for( int i = 0; i < repetitions; i++ )
        {
            for( int j = 0; j < textLength; j++ )
            {
                this.text[j + i * textLength] = text.charAt( j );
            }
        }
    }

    public int length()
    {
        return text.length;
    }

    public String read()
    {
        return read( 0, text.length );
    }

    public String read( int start )
    {
        return read( start, text.length );
    }

    public String read( int start, int end )
    {
        start = Math.max( start, 0 );
        end = Math.min( end, text.length );
        int textLength = Math.max( end - start, 0 );
        return new String( text, start, textLength );
    }

    public void write( String text )
    {
        write( text, 0, text.length() );
    }

    public void write( String text, int start )
    {
        write( text, start, start + text.length() );
    }

    public void write( String text, int start, int end )
    {
        int pos = start;
        start = Math.max( start, 0 );
        end = Math.min( end, pos + text.length() );
        end = Math.min( end, this.text.length );
        for( int i = start; i < end; i++ )
        {
            this.text[i] = text.charAt( i - pos );
        }
    }

    public void write( TextBuffer text )
    {
        write( text, 0, text.length() );
    }

    public void write( TextBuffer text, int start )
    {
        write( text, start, start + text.length() );
    }

    public void write( TextBuffer text, int start, int end )
    {
        int pos = start;
        start = Math.max( start, 0 );
        end = Math.min( end, pos + text.length() );
        end = Math.min( end, this.text.length );
        for( int i = start; i < end; i++ )
        {
            this.text[i] = text.charAt( i - pos );
        }
    }

    public void fill( char c )
    {
        fill( c, 0, text.length );
    }

    public void fill( char c, int start )
    {
        fill( c, start, text.length );
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

    public void fill( String text )
    {
        fill( text, 0, this.text.length );
    }

    public void fill( String text, int start )
    {
        fill( text, start, this.text.length );
    }

    public void fill( String text, int start, int end )
    {
        int pos = start;
        start = Math.max( start, 0 );
        end = Math.min( end, this.text.length );

        int textLength = text.length();
        for( int i = start; i < end; i++ )
        {
            this.text[i] = text.charAt( (i - pos) % textLength );
        }
    }

    public void fill( TextBuffer text )
    {
        fill( text, 0, this.text.length );
    }

    public void fill( TextBuffer text, int start )
    {
        fill( text, start, this.text.length );
    }

    public void fill( TextBuffer text, int start, int end )
    {
        int pos = start;
        start = Math.max( start, 0 );
        end = Math.min( end, this.text.length );

        int textLength = text.length();
        for( int i = start; i < end; i++ )
        {
            this.text[i] = text.charAt( (i - pos) % textLength );
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

    public String toString()
    {
        return new String( text );
    }
}
