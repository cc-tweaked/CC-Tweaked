/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.terminal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextBufferTest
{
    @Test
    void testStringConstructor()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        assertEquals( "test", textBuffer.toString() );
    }

    @Test
    void testCharRepetitionConstructor()
    {
        TextBuffer textBuffer = new TextBuffer( 'a', 5 );
        assertEquals( "aaaaa", textBuffer.toString() );
    }

    @Test
    void testLength()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        assertEquals( 4, textBuffer.length() );
    }

    @Test
    void testWrite()
    {
        TextBuffer textBuffer = new TextBuffer( ' ', 4 );
        textBuffer.write( "test" );
        assertEquals( "test", textBuffer.toString() );
    }

    @Test
    void testWriteTextBuffer()
    {
        TextBuffer source = new TextBuffer( "test" );
        TextBuffer target = new TextBuffer( "    " );
        target.write( source );
        assertEquals( "test", target.toString() );
    }

    @Test
    void testWriteFromPos()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.write( "il", 1 );
        assertEquals( "tilt", textBuffer.toString() );
    }

    @Test
    void testWriteOutOfBounds()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.write( "abcdefghijklmnop", -5 );
        assertEquals( "fghi", textBuffer.toString() );
    }

    @Test
    void testWriteOutOfBounds2()
    {
        TextBuffer textBuffer = new TextBuffer( "             " );
        textBuffer.write( "Hello, world!", -3 );
        assertEquals( "lo, world!   ", textBuffer.toString() );
    }

    @Test
    void testFill()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.fill( 'c' );
        assertEquals( "cccc", textBuffer.toString() );
    }

    @Test
    void testFillSubstring()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.fill( 'c', 1, 3 );
        assertEquals( "tcct", textBuffer.toString() );
    }

    @Test
    void testFillOutOfBounds()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.fill( 'c', -5, 5 );
        assertEquals( "cccc", textBuffer.toString() );
    }

    @Test
    void testCharAt()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        assertEquals( 'e', textBuffer.charAt( 1 ) );
    }

    @Test
    void testSetChar()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.setChar( 2, 'n' );
        assertEquals( "tent", textBuffer.toString() );
    }

    @Test
    void testSetCharWithNegativeIndex()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.setChar( -5, 'n' );
        assertEquals( "test", textBuffer.toString(), "Buffer should not change after setting char with negative index." );
    }

    @Test
    void testSetCharWithIndexBeyondBufferEnd()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.setChar( 10, 'n' );
        assertEquals( "test", textBuffer.toString(), "Buffer should not change after setting char beyond buffer end." );
    }

    @Test
    void testMultipleOperations()
    {
        TextBuffer textBuffer = new TextBuffer( ' ', 5 );
        textBuffer.setChar( 0, 'H' );
        textBuffer.setChar( 1, 'e' );
        textBuffer.setChar( 2, 'l' );
        textBuffer.write( "lo", 3 );
        assertEquals( "Hello", textBuffer.toString(), "TextBuffer failed to persist over multiple operations." );
    }

    @Test
    void testEmptyBuffer()
    {
        TextBuffer textBuffer = new TextBuffer( "" );
        // exception on writing to empty buffer would fail the test
        textBuffer.write( "test" );
        assertEquals( "", textBuffer.toString() );
    }
}
