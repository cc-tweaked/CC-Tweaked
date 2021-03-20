/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.terminal;

import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TerminalTest
{
    // for PacketBuffer read/write tests
    private static final byte[] packetBufferSnapshot = {
        0, 0, 0, 2, 0, 0, 0, 5, 0, 83, 104, -31, 105, -31, 17, 17, 17, -52, 76, 76, 87, -90, 78, 127,
        102, 76, 51, 102, -52, -78, 102, -27, 76, -103, -78, -103, -103, -103, 76, 76, 76, -14, -78,
        -52, 127, -52, 25, -34, -34, 108, -103, -78, -14, -27, 127, -40, -14, -78, 51, -16, -16, -16,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0,
    };

    // for NBT read/write tests
    private static final String nbtSnapshot =
        "{term_cursorX:2,term_cursorBlink:0b,term_palette:" +
        "[I;1118481,13388876,5744206,8349260,3368652,11691749,5020082,10066329," +
        "5000268,15905484,8375321,14605932,10072818,15040472,15905331,15790320]," +
        "term_bgColour:5,term_text_0:\"hi\",term_cursorY:5,term_textColour:3," +
        "term_textBgColour_0:\"ee\",term_textColour_0:\"11\"}";

    private static class MockOnChangedCallback implements Runnable
    {
        private int timesCalled = 0;

        @Override
        public void run()
        {
            timesCalled++;
        }

        public void assertCalledTimes( int expectedTimesCalled )
        {
            assertEquals( expectedTimesCalled, timesCalled, "onChanged callback was not called the correct number of times" );
        }

        public void assertNotCalled()
        {
            assertEquals( 0, timesCalled, "onChanged callback should not have been called" );
        }

        public void mockClear()
        {
            this.timesCalled = 0;
        }
    }

    private static final class TerminalTestSnapshot
    {
        private final List<String> textLines;
        private final List<String> textColourLines;
        private final List<String> backgroundColourLines;

        private TerminalTestSnapshot( Terminal terminal )
        {
            textLines = new ArrayList<>( terminal.getHeight() );
            textColourLines = new ArrayList<>( terminal.getHeight() );
            backgroundColourLines = new ArrayList<>( terminal.getHeight() );

            for( int i = 0; i < terminal.getHeight(); i++ )
            {
                textLines.add( terminal.getLine( i ).toString() );
                textColourLines.add( terminal.getTextColourLine( i ).toString() );
                backgroundColourLines.add( terminal.getBackgroundColourLine( i ).toString() );
            }
        }

        public TerminalTestSnapshot assertTextMatches( String[] snapshot )
        {
            List<String> snapshotLines = new ArrayList<>( Arrays.asList( snapshot ) );
            assertLinesMatch( snapshotLines, textLines );
            return this;
        }

        public TerminalTestSnapshot assertTextColourMatches( String[] snapshot )
        {
            List<String> snapshotLines = new ArrayList<>( Arrays.asList( snapshot ) );
            assertLinesMatch( snapshotLines, textColourLines );
            return this;
        }

        public TerminalTestSnapshot assertBackgroundColourMatches( String[] snapshot )
        {
            List<String> snapshotLines = new ArrayList<>( Arrays.asList( snapshot ) );
            assertLinesMatch( snapshotLines, backgroundColourLines );
            return this;
        }

        public void assertBufferUnchanged( TerminalTestSnapshot old )
        {
            this.assertTextMatches( old.textLines.toArray( new String[0] ) );
            this.assertTextColourMatches( old.textColourLines.toArray( new String[0] ) );
            this.assertBackgroundColourMatches( old.backgroundColourLines.toArray( new String[0] ) );
        }
    }

    @Test
    void testCreation()
    {
        Terminal terminal = new Terminal( 16, 9 );
        assertEquals( 16, terminal.getWidth() );
        assertEquals( 9, terminal.getHeight() );
    }

    @Test
    void testSetAndGetLine()
    {
        Terminal terminal = new Terminal( 16, 9 );
        terminal.setLine( 1, "ABCDEFGHIJKLMNOP", "0123456789abcdef", "fedcba9876543210" );
        assertEquals( "ABCDEFGHIJKLMNOP", terminal.getLine( 1 ).toString() );
        assertEquals( "0123456789abcdef", terminal.getTextColourLine( 1 ).toString() );
        assertEquals( "fedcba9876543210", terminal.getBackgroundColourLine( 1 ).toString() );
    }

    @Test
    void testDefaults()
    {
        Terminal terminal = new Terminal( 16, 9 );
        assertEquals( 0, terminal.getCursorX() );
        assertEquals( 0, terminal.getCursorY() );
        assertFalse( terminal.getCursorBlink() );
        assertEquals( 0, terminal.getTextColour() );
        assertEquals( 15, terminal.getBackgroundColour() );
    }

    @Test
    void testDefaultTextBuffer()
    {
        new TerminalTestSnapshot( new Terminal( 4, 3 ) )
            .assertTextMatches( new String[] {
                "    ",
                "    ",
                "    ",
            } );
    }

    @Test
    void testDefaultTextColourBuffer()
    {
        new TerminalTestSnapshot( new Terminal( 4, 3 ) )
            .assertTextColourMatches( new String[] {
                "0000",
                "0000",
                "0000",
            } );
    }

    @Test
    void testDefaultBackgroundColourBuffer()
    {
        new TerminalTestSnapshot( new Terminal( 4, 3 ) )
            .assertBackgroundColourMatches( new String[] {
                "ffff",
                "ffff",
                "ffff",
            } );
    }

    @Test
    void testResizeHeight()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        terminal.resize( 4, 4 );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "    ",
                "    ",
                "    ",
                "    ",
            } ).assertTextColourMatches( new String[] {
                "0000",
                "0000",
                "0000",
                "0000",
            } ).assertBackgroundColourMatches( new String[] {
                "ffff",
                "ffff",
                "ffff",
                "ffff",
            } );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testResizeWidth()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        terminal.resize( 5, 3 );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "     ",
                "     ",
                "     ",
            } ).assertTextColourMatches( new String[] {
                "00000",
                "00000",
                "00000",
            } ).assertBackgroundColourMatches( new String[] {
                "fffff",
                "fffff",
                "fffff",
            } );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testResizeWidthAndHeight()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        terminal.resize( 5, 4 );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "     ",
                "     ",
                "     ",
                "     ",
            } ).assertTextColourMatches( new String[] {
                "00000",
                "00000",
                "00000",
                "00000",
            } ).assertBackgroundColourMatches( new String[] {
                "fffff",
                "fffff",
                "fffff",
                "fffff",
            } );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testResizeSmaller()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        terminal.resize( 2, 2 );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "  ",
                "  ",
            } ).assertTextColourMatches( new String[] {
                "00",
                "00",
            } ).assertBackgroundColourMatches( new String[] {
                "ff",
                "ff",
            } );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testResizeWithSameDimensions()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        TerminalTestSnapshot old = new TerminalTestSnapshot( terminal );
        terminal.resize( 4, 3 );

        new TerminalTestSnapshot( terminal ).assertBufferUnchanged( old );

        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testSetAndGetCursorPos()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setCursorPos( 2, 1 );

        assertEquals( 2, terminal.getCursorX() );
        assertEquals( 1, terminal.getCursorY() );
        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testSetCursorPosUnchanged()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setCursorPos( 2, 1 );
        mockOnChangedCallback.mockClear();
        terminal.setCursorPos( 2, 1 );

        assertEquals( 2, terminal.getCursorX() );
        assertEquals( 1, terminal.getCursorY() );
        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testSetCursorBlink()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setCursorBlink( true );

        assertTrue( terminal.getCursorBlink() );
        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testSetCursorBlinkUnchanged()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setCursorBlink( true );
        mockOnChangedCallback.mockClear();
        terminal.setCursorBlink( true );

        assertTrue( terminal.getCursorBlink() );
        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testSetTextColour()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setTextColour( 5 );

        assertEquals( terminal.getTextColour(), 5 );
        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testSetTextColourUnchanged()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setTextColour( 5 );
        mockOnChangedCallback.mockClear();
        terminal.setTextColour( 5 );

        assertEquals( terminal.getTextColour(), 5 );
        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testSetBackgroundColour()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setBackgroundColour( 5 );

        assertEquals( terminal.getBackgroundColour(), 5 );
        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testSetBackgroundColourUnchanged()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setBackgroundColour( 5 );
        mockOnChangedCallback.mockClear();
        terminal.setBackgroundColour( 5 );

        assertEquals( terminal.getBackgroundColour(), 5 );
        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testBlitFromOrigin()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.blit( "test", "1234", "abcd" );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "test",
                "    ",
                "    ",
            } ).assertTextColourMatches( new String[] {
                "1234",
                "0000",
                "0000",
            } ).assertBackgroundColourMatches( new String[] {
                "abcd",
                "ffff",
                "ffff",
            } );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testBlitWithOffset()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setCursorPos( 2, 1 );
        mockOnChangedCallback.mockClear();
        terminal.blit( "hi", "11", "ee" );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "    ",
                "  hi",
                "    ",
            } ).assertTextColourMatches( new String[] {
                "0000",
                "0011",
                "0000",
            } ).assertBackgroundColourMatches( new String[] {
                "ffff",
                "ffee",
                "ffff",
            } );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testBlitOutOfBoundsNegativeY()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        TerminalTestSnapshot old = new TerminalTestSnapshot( terminal );

        terminal.setCursorPos( 2, -5 );
        mockOnChangedCallback.mockClear();
        terminal.blit( "hi", "11", "ee" );

        new TerminalTestSnapshot( terminal ).assertBufferUnchanged( old );

        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testBlitOutOfBoundsPositiveY()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        TerminalTestSnapshot old = new TerminalTestSnapshot( terminal );

        terminal.setCursorPos( 2, 5 );
        mockOnChangedCallback.mockClear();
        terminal.blit( "hi", "11", "ee" );

        new TerminalTestSnapshot( terminal ).assertBufferUnchanged( old );

        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testWriteFromOrigin()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.write( "test" );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "test",
                "    ",
                "    ",
            } ).assertTextColourMatches( new String[] {
                "0000",
                "0000",
                "0000",
            } ).assertBackgroundColourMatches( new String[] {
                "ffff",
                "ffff",
                "ffff",
            } );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testWriteWithOffset()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setCursorPos( 2, 1 );
        mockOnChangedCallback.mockClear();
        terminal.write( "hi" );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "    ",
                "  hi",
                "    ",
            } ).assertTextColourMatches( new String[] {
                "0000",
                "0000",
                "0000",
            } ).assertBackgroundColourMatches( new String[] {
                "ffff",
                "ffff",
                "ffff",
            } );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testWriteOutOfBoundsNegativeY()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        TerminalTestSnapshot old = new TerminalTestSnapshot( terminal );

        terminal.setCursorPos( 2, -5 );
        mockOnChangedCallback.mockClear();
        terminal.write( "hi" );

        new TerminalTestSnapshot( terminal ).assertBufferUnchanged( old );

        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testWriteOutOfBoundsPositiveY()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        TerminalTestSnapshot old = new TerminalTestSnapshot( terminal );

        terminal.setCursorPos( 2, 5 );
        mockOnChangedCallback.mockClear();
        terminal.write( "hi" );

        new TerminalTestSnapshot( terminal ).assertBufferUnchanged( old );

        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testScrollUp()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setLine( 1, "test", "1111", "eeee" );
        mockOnChangedCallback.mockClear();
        terminal.scroll( 1 );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "test",
                "    ",
                "    ",
            } ).assertTextColourMatches( new String[] {
                "1111",
                "0000",
                "0000",
            } ).assertBackgroundColourMatches( new String[] {
                "eeee",
                "ffff",
                "ffff",
            } );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testScrollDown()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setLine( 1, "test", "1111", "eeee" );
        mockOnChangedCallback.mockClear();
        terminal.scroll( -1 );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "    ",
                "    ",
                "test",
            } ).assertTextColourMatches( new String[] {
                "0000",
                "0000",
                "1111",
            } ).assertBackgroundColourMatches( new String[] {
                "ffff",
                "ffff",
                "eeee",
            } );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testScrollZeroLinesUnchanged()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setLine( 1, "test", "1111", "eeee" );
        TerminalTestSnapshot old = new TerminalTestSnapshot( terminal );
        mockOnChangedCallback.mockClear();
        terminal.scroll( 0 );

        new TerminalTestSnapshot( terminal ).assertBufferUnchanged( old );

        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testClear()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        TerminalTestSnapshot old = new TerminalTestSnapshot( terminal );

        terminal.setLine( 1, "test", "1111", "eeee" );
        mockOnChangedCallback.mockClear();
        terminal.clear();

        new TerminalTestSnapshot( terminal ).assertBufferUnchanged( old );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testClearLine()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );
        TerminalTestSnapshot old = new TerminalTestSnapshot( terminal );

        terminal.setLine( 1, "test", "1111", "eeee" );
        terminal.setCursorPos( 0, 1 );
        mockOnChangedCallback.mockClear();
        terminal.clearLine();

        new TerminalTestSnapshot( terminal ).assertBufferUnchanged( old );

        mockOnChangedCallback.assertCalledTimes( 1 );
    }

    @Test
    void testClearOutOfBoundsNegativeY()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setLine( 1, "test", "1111", "eeee" );
        TerminalTestSnapshot old = new TerminalTestSnapshot( terminal );
        terminal.setCursorPos( 0, -5 );
        mockOnChangedCallback.mockClear();
        terminal.clearLine();

        new TerminalTestSnapshot( terminal ).assertBufferUnchanged( old );

        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testClearOutOfBoundsPositiveY()
    {
        MockOnChangedCallback mockOnChangedCallback = new MockOnChangedCallback();
        Terminal terminal = new Terminal( 4, 3, mockOnChangedCallback );

        terminal.setLine( 1, "test", "1111", "eeee" );
        TerminalTestSnapshot old = new TerminalTestSnapshot( terminal );
        terminal.setCursorPos( 0, 5 );
        mockOnChangedCallback.mockClear();
        terminal.clearLine();

        new TerminalTestSnapshot( terminal ).assertBufferUnchanged( old );

        mockOnChangedCallback.assertNotCalled();
    }

    @Test
    void testWriteToPacketBuffer()
    {
        Terminal terminal = new Terminal( 2, 1 );

        terminal.blit( "hi", "11", "ee" );
        terminal.setCursorPos( 2, 5 );
        terminal.setTextColour( 3 );
        terminal.setBackgroundColour( 5 );

        PacketBuffer packetBuffer = new PacketBuffer( Unpooled.buffer() );
        terminal.write( packetBuffer );

        assertArrayEquals( packetBufferSnapshot, packetBuffer.array() );
    }

    @Test
    void testReadFromPacketBuffer()
    {
        Terminal terminal = new Terminal( 2, 1 );

        PacketBuffer packetBuffer = new PacketBuffer( Unpooled.buffer() );
        packetBuffer.writeBytes( packetBufferSnapshot );
        terminal.read( packetBuffer );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "hi",
            } ).assertTextColourMatches( new String[] {
                "11",
            } ).assertBackgroundColourMatches( new String[] {
                "ee",
            } );

        assertEquals( 2, terminal.getCursorX() );
        assertEquals( 5, terminal.getCursorY() );
        assertEquals( 3, terminal.getTextColour() );
        assertEquals( 5, terminal.getBackgroundColour() );
    }

    @Test
    void testWriteAndReadNBT()
    {
        // WRITE
        Terminal terminal = new Terminal( 2, 1 );

        terminal.blit( "hi", "11", "ee" );
        terminal.setCursorPos( 2, 5 );
        terminal.setTextColour( 3 );
        terminal.setBackgroundColour( 5 );

        PacketBuffer packetBuffer = new PacketBuffer( Unpooled.buffer() );
        terminal.write( packetBuffer );

        CompoundNBT nbt = new CompoundNBT();
        terminal.writeToNBT( nbt );
        assertEquals( nbtSnapshot, nbt.toString() );

        // READ
        terminal = new Terminal( 2, 1 );

        terminal.readFromNBT( nbt );

        new TerminalTestSnapshot( terminal )
            .assertTextMatches( new String[] {
                "hi",
            } ).assertTextColourMatches( new String[] {
                "11",
            } ).assertBackgroundColourMatches( new String[] {
                "ee",
            } );

        assertEquals( 2, terminal.getCursorX() );
        assertEquals( 5, terminal.getCursorY() );
        assertEquals( 3, terminal.getTextColour() );
        assertEquals( 5, terminal.getBackgroundColour() );
    }
}
