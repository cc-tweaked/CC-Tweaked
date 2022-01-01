/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.terminal;

import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.utils.CallCounter;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import static dan200.computercraft.core.terminal.TerminalMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.*;

class TerminalTest
{
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
    void testGetLineOutOfBounds()
    {
        Terminal terminal = new Terminal( 16, 9 );

        assertNull( terminal.getLine( -5 ) );
        assertNull( terminal.getLine( 12 ) );

        assertNull( terminal.getTextColourLine( -5 ) );
        assertNull( terminal.getTextColourLine( 12 ) );

        assertNull( terminal.getBackgroundColourLine( -5 ) );
        assertNull( terminal.getBackgroundColourLine( 12 ) );
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
        assertThat( new Terminal( 4, 3 ), textMatches( new String[] {
            "    ",
            "    ",
            "    ",
        } ) );
    }

    @Test
    void testDefaultTextColourBuffer()
    {
        assertThat( new Terminal( 4, 3 ), textColourMatches( new String[] {
            "0000",
            "0000",
            "0000",
        } ) );
    }

    @Test
    void testDefaultBackgroundColourBuffer()
    {
        assertThat( new Terminal( 4, 3 ), TerminalMatchers.backgroundColourMatches( new String[] {
            "ffff",
            "ffff",
            "ffff",
        } ) );
    }

    @Test
    void testZeroSizeBuffers()
    {
        String[] x = new String[0];
        assertThat( new Terminal( 0, 0 ), allOf(
            textMatches( new String[0] ),
            textColourMatches( x ),
            TerminalMatchers.backgroundColourMatches( x )
        ) );
    }

    @Test
    void testResizeWidthAndHeight()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );
        terminal.setLine( 0, "test", "aaaa", "eeee" );
        callCounter.reset();

        terminal.resize( 5, 4 );

        assertThat( terminal, allOf(
            textMatches( new String[] {
                "test ",
                "     ",
                "     ",
                "     ",
            } ),
            textColourMatches( new String[] {
                "aaaa0",
                "00000",
                "00000",
                "00000",
            } ), TerminalMatchers.backgroundColourMatches( new String[] {
                "eeeef",
                "fffff",
                "fffff",
                "fffff",
            } )
        ) );

        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testResizeSmaller()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );
        terminal.setLine( 0, "test", "aaaa", "eeee" );
        terminal.setLine( 1, "smol", "aaaa", "eeee" );
        terminal.setLine( 2, "term", "aaaa", "eeee" );
        callCounter.reset();

        terminal.resize( 2, 2 );

        assertThat( terminal, allOf(
            textMatches( new String[] {
                "te",
                "sm",
            } ),
            textColourMatches( new String[] {
                "aa",
                "aa",
            } ),
            TerminalMatchers.backgroundColourMatches( new String[] {
                "ee",
                "ee",
            } )
        ) );

        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testResizeWithSameDimensions()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );
        TerminalBufferSnapshot old = new TerminalBufferSnapshot( terminal );
        terminal.resize( 4, 3 );

        assertThat( "Terminal should be unchanged", terminal, old.matches() );

        callCounter.assertNotCalled();
    }

    @Test
    void testSetAndGetCursorPos()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setCursorPos( 2, 1 );

        assertEquals( 2, terminal.getCursorX() );
        assertEquals( 1, terminal.getCursorY() );
        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testSetCursorPosUnchanged()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setCursorPos( 2, 1 );
        callCounter.reset();
        terminal.setCursorPos( 2, 1 );

        assertEquals( 2, terminal.getCursorX() );
        assertEquals( 1, terminal.getCursorY() );
        callCounter.assertNotCalled();
    }

    @Test
    void testSetCursorBlink()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setCursorBlink( true );

        assertTrue( terminal.getCursorBlink() );
        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testSetCursorBlinkUnchanged()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setCursorBlink( true );
        callCounter.reset();
        terminal.setCursorBlink( true );

        assertTrue( terminal.getCursorBlink() );
        callCounter.assertNotCalled();
    }

    @Test
    void testSetTextColour()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setTextColour( 5 );

        assertEquals( terminal.getTextColour(), 5 );
        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testSetTextColourUnchanged()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setTextColour( 5 );
        callCounter.reset();
        terminal.setTextColour( 5 );

        assertEquals( terminal.getTextColour(), 5 );
        callCounter.assertNotCalled();
    }

    @Test
    void testSetBackgroundColour()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setBackgroundColour( 5 );

        assertEquals( terminal.getBackgroundColour(), 5 );
        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testSetBackgroundColourUnchanged()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setBackgroundColour( 5 );
        callCounter.reset();
        terminal.setBackgroundColour( 5 );

        assertEquals( terminal.getBackgroundColour(), 5 );
        callCounter.assertNotCalled();
    }

    @Test
    void testBlitFromOrigin()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.blit( "test", "1234", "abcd" );

        assertThat( terminal, allOf(
            textMatches( new String[] {
                "test",
                "    ",
                "    ",
            } ), textColourMatches( new String[] {
                "1234",
                "0000",
                "0000",
            } ), backgroundColourMatches( new String[] {
                "abcd",
                "ffff",
                "ffff",
            } )
        ) );

        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testBlitWithOffset()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setCursorPos( 2, 1 );
        callCounter.reset();
        terminal.blit( "hi", "11", "ee" );

        assertThat( terminal, allOf(
            textMatches( new String[] {
                "    ",
                "  hi",
                "    ",
            } ),
            textColourMatches( new String[] {
                "0000",
                "0011",
                "0000",
            } ),
            backgroundColourMatches( new String[] {
                "ffff",
                "ffee",
                "ffff",
            } )
        ) );

        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testBlitOutOfBounds()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );
        TerminalBufferSnapshot old = new TerminalBufferSnapshot( terminal );

        terminal.setCursorPos( 2, -5 );
        callCounter.reset();
        terminal.blit( "hi", "11", "ee" );
        assertThat( terminal, old.matches() );
        callCounter.assertNotCalled();

        terminal.setCursorPos( 2, 5 );
        callCounter.reset();
        terminal.blit( "hi", "11", "ee" );
        assertThat( terminal, old.matches() );
        callCounter.assertNotCalled();
    }

    @Test
    void testWriteFromOrigin()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.write( "test" );

        assertThat( terminal, allOf(
            textMatches( new String[] {
                "test",
                "    ",
                "    ",
            } ), textColourMatches( new String[] {
                "0000",
                "0000",
                "0000",
            } ), backgroundColourMatches( new String[] {
                "ffff",
                "ffff",
                "ffff",
            } )
        ) );

        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testWriteWithOffset()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setCursorPos( 2, 1 );
        callCounter.reset();
        terminal.write( "hi" );

        assertThat( terminal, allOf(
            textMatches( new String[] {
                "    ",
                "  hi",
                "    ",
            } ),
            textColourMatches( new String[] {
                "0000",
                "0000",
                "0000",
            } ),
            backgroundColourMatches( new String[] {
                "ffff",
                "ffff",
                "ffff",
            } )
        ) );

        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testWriteOutOfBounds()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );
        TerminalBufferSnapshot old = new TerminalBufferSnapshot( terminal );

        terminal.setCursorPos( 2, -5 );
        callCounter.reset();
        terminal.write( "hi" );

        assertThat( terminal, old.matches() );
        callCounter.assertNotCalled();

        terminal.setCursorPos( 2, 5 );
        callCounter.reset();
        terminal.write( "hi" );
        assertThat( terminal, old.matches() );
        callCounter.assertNotCalled();
    }

    @Test
    void testScrollUp()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setLine( 1, "test", "1111", "eeee" );
        callCounter.reset();
        terminal.scroll( 1 );

        assertThat( terminal, allOf(
            textMatches( new String[] {
                "test",
                "    ",
                "    ",
            } ),
            textColourMatches( new String[] {
                "1111",
                "0000",
                "0000",
            } ),
            backgroundColourMatches( new String[] {
                "eeee",
                "ffff",
                "ffff",
            } )
        ) );

        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testScrollDown()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setLine( 1, "test", "1111", "eeee" );
        callCounter.reset();
        terminal.scroll( -1 );

        assertThat( terminal, allOf(
            textMatches( new String[] {
                "    ",
                "    ",
                "test",
            } ),
            textColourMatches( new String[] {
                "0000",
                "0000",
                "1111",
            } ),
            backgroundColourMatches( new String[] {
                "ffff",
                "ffff",
                "eeee",
            } )
        ) );

        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testScrollZeroLinesUnchanged()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );

        terminal.setLine( 1, "test", "1111", "eeee" );
        TerminalBufferSnapshot old = new TerminalBufferSnapshot( terminal );
        callCounter.reset();
        terminal.scroll( 0 );

        assertThat( terminal, old.matches() );

        callCounter.assertNotCalled();
    }

    @Test
    void testClear()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );
        TerminalBufferSnapshot old = new TerminalBufferSnapshot( terminal );

        terminal.setLine( 1, "test", "1111", "eeee" );
        callCounter.reset();
        terminal.clear();

        assertThat( terminal, old.matches() );

        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testClearLine()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );
        TerminalBufferSnapshot old = new TerminalBufferSnapshot( terminal );

        terminal.setLine( 1, "test", "1111", "eeee" );
        terminal.setCursorPos( 0, 1 );
        callCounter.reset();
        terminal.clearLine();

        assertThat( terminal, old.matches() );

        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testClearLineOutOfBounds()
    {
        CallCounter callCounter = new CallCounter();
        Terminal terminal = new Terminal( 4, 3, callCounter );
        TerminalBufferSnapshot old;

        terminal.setLine( 1, "test", "1111", "eeee" );
        old = new TerminalBufferSnapshot( terminal );
        terminal.setCursorPos( 0, -5 );
        callCounter.reset();
        terminal.clearLine();
        assertThat( terminal, old.matches() );
        callCounter.assertNotCalled();

        terminal.setLine( 1, "test", "1111", "eeee" );
        old = new TerminalBufferSnapshot( terminal );
        terminal.setCursorPos( 0, 5 );
        callCounter.reset();
        terminal.clearLine();
        assertThat( terminal, old.matches() );
        callCounter.assertNotCalled();
    }

    @Test
    void testPacketBufferRoundtrip()
    {
        Terminal writeTerminal = new Terminal( 2, 1 );

        writeTerminal.blit( "hi", "11", "ee" );
        writeTerminal.setCursorPos( 2, 5 );
        writeTerminal.setTextColour( 3 );
        writeTerminal.setBackgroundColour( 5 );

        FriendlyByteBuf packetBuffer = new FriendlyByteBuf( Unpooled.buffer() );
        writeTerminal.write( packetBuffer );

        CallCounter callCounter = new CallCounter();
        Terminal readTerminal = new Terminal( 2, 1, callCounter );
        packetBuffer.writeBytes( packetBuffer );
        readTerminal.read( packetBuffer );

        assertThat( readTerminal, allOf(
            textMatches( new String[] { "hi", } ),
            textColourMatches( new String[] { "11", } ),
            backgroundColourMatches( new String[] { "ee", } )
        ) );

        assertEquals( 2, readTerminal.getCursorX() );
        assertEquals( 5, readTerminal.getCursorY() );
        assertEquals( 3, readTerminal.getTextColour() );
        assertEquals( 5, readTerminal.getBackgroundColour() );
        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testNbtRoundtrip()
    {
        Terminal writeTerminal = new Terminal( 10, 5 );
        writeTerminal.blit( "hi", "11", "ee" );
        writeTerminal.setCursorPos( 2, 5 );
        writeTerminal.setTextColour( 3 );
        writeTerminal.setBackgroundColour( 5 );

        CompoundTag nbt = new CompoundTag();
        writeTerminal.writeToNBT( nbt );

        CallCounter callCounter = new CallCounter();
        Terminal readTerminal = new Terminal( 2, 1, callCounter );

        readTerminal.readFromNBT( nbt );

        assertThat( readTerminal, allOf(
            textMatches( new String[] { "hi", } ),
            textColourMatches( new String[] { "11", } ),
            backgroundColourMatches( new String[] { "ee", } )
        ) );

        assertEquals( 2, readTerminal.getCursorX() );
        assertEquals( 5, readTerminal.getCursorY() );
        assertEquals( 3, readTerminal.getTextColour() );
        assertEquals( 5, readTerminal.getBackgroundColour() );
        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testReadWriteNBTEmpty()
    {
        Terminal terminal = new Terminal( 0, 0 );

        CompoundTag nbt = new CompoundTag();
        terminal.writeToNBT( nbt );

        CallCounter callCounter = new CallCounter();
        terminal = new Terminal( 0, 1, callCounter );
        terminal.readFromNBT( nbt );

        assertThat( terminal, allOf(
            textMatches( new String[] { "", } ),
            textColourMatches( new String[] { "", } ),
            backgroundColourMatches( new String[] { "", } )
        ) );

        assertEquals( 0, terminal.getCursorX() );
        assertEquals( 0, terminal.getCursorY() );
        assertEquals( 0, terminal.getTextColour() );
        assertEquals( 15, terminal.getBackgroundColour() );
        callCounter.assertCalledTimes( 1 );
    }

    @Test
    void testGetColour()
    {
        // 0 - 9
        assertEquals( 0, Terminal.getColour( '0', Colour.WHITE ) );
        assertEquals( 1, Terminal.getColour( '1', Colour.WHITE ) );
        assertEquals( 8, Terminal.getColour( '8', Colour.WHITE ) );
        assertEquals( 9, Terminal.getColour( '9', Colour.WHITE ) );

        // a - f
        assertEquals( 10, Terminal.getColour( 'a', Colour.WHITE ) );
        assertEquals( 11, Terminal.getColour( 'b', Colour.WHITE ) );
        assertEquals( 14, Terminal.getColour( 'e', Colour.WHITE ) );
        assertEquals( 15, Terminal.getColour( 'f', Colour.WHITE ) );

        // char out of bounds -> use colour enum ordinal
        assertEquals( 0, Terminal.getColour( 'z', Colour.WHITE ) );
        assertEquals( 0, Terminal.getColour( '!', Colour.WHITE ) );
        assertEquals( 0, Terminal.getColour( 'Z', Colour.WHITE ) );
        assertEquals( 5, Terminal.getColour( 'Z', Colour.LIME ) );
    }

    private static final class TerminalBufferSnapshot
    {
        final String[] textLines;
        final String[] textColourLines;
        final String[] backgroundColourLines;

        private TerminalBufferSnapshot( Terminal terminal )
        {
            textLines = new String[terminal.getHeight()];
            textColourLines = new String[terminal.getHeight()];
            backgroundColourLines = new String[terminal.getHeight()];

            for( int i = 0; i < terminal.getHeight(); i++ )
            {
                textLines[i] = terminal.getLine( i ).toString();
                textColourLines[i] = terminal.getTextColourLine( i ).toString();
                backgroundColourLines[i] = terminal.getBackgroundColourLine( i ).toString();
            }
        }

        public Matcher<Terminal> matches()
        {
            return allOf(
                textMatches( textLines ), textColourMatches( textColourLines ), TerminalMatchers.backgroundColourMatches( backgroundColourLines )
            );
        }
    }
}
