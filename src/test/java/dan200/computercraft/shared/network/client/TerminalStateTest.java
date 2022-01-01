/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.network.client;

import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link TerminalState} round tripping works as expected.
 */
public class TerminalStateTest
{
    @RepeatedTest( 5 )
    public void testCompressed()
    {
        Terminal terminal = randomTerminal();

        FriendlyByteBuf buffer = new FriendlyByteBuf( Unpooled.directBuffer() );
        new TerminalState( true, terminal, true ).write( buffer );

        checkEqual( terminal, read( buffer ) );
        assertEquals( 0, buffer.readableBytes() );
    }

    @RepeatedTest( 5 )
    public void testUncompressed()
    {
        Terminal terminal = randomTerminal();

        FriendlyByteBuf buffer = new FriendlyByteBuf( Unpooled.directBuffer() );
        new TerminalState( true, terminal, false ).write( buffer );

        checkEqual( terminal, read( buffer ) );
        assertEquals( 0, buffer.readableBytes() );
    }

    private static Terminal randomTerminal()
    {
        Random random = new Random();
        Terminal terminal = new Terminal( 10, 5 );
        for( int y = 0; y < terminal.getHeight(); y++ )
        {
            TextBuffer buffer = terminal.getLine( y );
            for( int x = 0; x < buffer.length(); x++ ) buffer.setChar( x, (char) (random.nextInt( 26 ) + 65) );
        }

        return terminal;
    }

    private static void checkEqual( Terminal expected, Terminal actual )
    {
        assertNotNull( expected, "Expected cannot be null" );
        assertNotNull( actual, "Actual cannot be null" );
        assertEquals( expected.getHeight(), actual.getHeight(), "Heights must match" );
        assertEquals( expected.getWidth(), actual.getWidth(), "Widths must match" );

        for( int y = 0; y < expected.getHeight(); y++ )
        {
            assertEquals( expected.getLine( y ).toString(), actual.getLine( y ).toString() );
        }
    }

    private static Terminal read( FriendlyByteBuf buffer )
    {
        TerminalState state = new TerminalState( buffer );
        assertTrue( state.colour );

        if( !state.hasTerminal() ) return null;

        Terminal other = new Terminal( state.width, state.height );
        state.apply( other );
        return other;
    }
}
