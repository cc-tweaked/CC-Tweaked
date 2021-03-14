/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.terminal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextBufferTest
{
    @Test
    void testCreation()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        assertEquals( "test", textBuffer.toString() );
    }

    @Test
    void testLength()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        assertEquals( 4, textBuffer.length() );
    }

    @Test
    void testCharRepetitionConstructor()
    {
        TextBuffer textBuffer = new TextBuffer( 'a', 5 );
        assertEquals( "aaaaa", textBuffer.toString() );
    }

    @Test
    void testStringRepetitionConstructor()
    {
        TextBuffer textBuffer = new TextBuffer( "test", 3 );
        assertEquals( "testtesttest", textBuffer.toString() );
    }

    @Test
    void testReadNoArgs()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        assertEquals( "test", textBuffer.read() );
    }

    @Test
    void testReadFromPos()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        assertEquals( "st", textBuffer.read( 2 ) );
    }

    @Test
    void testReadSubstring()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        assertEquals( "es", textBuffer.read( 1, 3 ) );
    }

    @Test
    void testSubsequentRead()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.read();
        assertEquals( "test", textBuffer.read() );
    }

    @Test
    void testReadOutOfBounds()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.read( -5, 5 );
        assertEquals( "test", textBuffer.read() );
    }

    @Test
    void testWriteNoArgs()
    {
        TextBuffer textBuffer = new TextBuffer( ' ', 4 );
        textBuffer.write( "test" );
        assertEquals( "test", textBuffer.toString() );
    }

    @Test
    void testWriteFromPos()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.write( "il", 1 );
        assertEquals( "tilt", textBuffer.toString() );
    }

    @Test
    void testWriteSubstring()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.write( "il", 1, 2 );
        assertEquals( "tist", textBuffer.toString() );
    }

    @Test
    void testWriteOutOfBounds()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.write( "abcdefghijklmnop", -5, 5 );
        assertEquals( "fghi", textBuffer.read() );
    }

    @Test
    void testFill()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.fill( 'c' );
        assertEquals( "cccc", textBuffer.toString() );
    }

    @Test
    void testFillFromPos()
    {
        TextBuffer textBuffer = new TextBuffer( "test" );
        textBuffer.fill( 'c', 2 );
        assertEquals( "tecc", textBuffer.toString() );
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
        assertEquals( "cccc", textBuffer.read() );
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
}
