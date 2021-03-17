package dan200.computercraft.core.terminal;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TerminalTest
{
    class TerminalTestHelper {
        private final Terminal terminal;

        public TerminalTestHelper( Terminal terminal ) {
            this.terminal = terminal;
        }

        private boolean bufferMatches(String[] snapshot, TextBuffer[] buffer) {
            if ( snapshot.length != buffer.length ) {
                return false;
            }

            for ( int i = 0; i < snapshot.length; i++ ) {
                if ( !snapshot[i].equals( buffer[i].toString() ) ) {
                    return false;
                }
            }

            return true;
        }

        public Result textMatches( String[] snapshot ) {
            TextBuffer[] buffer = new TextBuffer[terminal.getHeight()];

            for ( int i = 0; i < terminal.getHeight(); i++ ) {
                buffer[i] = terminal.getLine(i);
            }

            return new Result(bufferMatches(snapshot, buffer), snapshot, buffer);
        }

        public Result textColourMatches( String[] snapshot ) {
            TextBuffer[] buffer = new TextBuffer[terminal.getHeight()];

            for ( int i = 0; i < terminal.getHeight(); i++ ) {
                buffer[i] = terminal.getTextColourLine(i);
            }

            return new Result(bufferMatches(snapshot, buffer), snapshot, buffer);
        }

        public Result backgroundColourMatches( String[] snapshot ) {
            TextBuffer[] buffer = new TextBuffer[terminal.getHeight()];

            for ( int i = 0; i < terminal.getHeight(); i++ ) {
                buffer[i] = terminal.getBackgroundColourLine(i);
            }

            return new Result(bufferMatches(snapshot, buffer), snapshot, buffer);
        }
    }

    class Result {
        private final boolean pass;
        private final String message;

        public Result(boolean pass, String[] expected, TextBuffer[] actual) {
            this.pass = pass;
            String[] actualStrings = new String[actual.length];
            for ( int i = 0; i < actual.length; i++ ) {
                actualStrings[i] = actual[i].toString();
            }

            this.message = String.format("Expected buffer: [%s] - Actual buffer: [%s]",
                String.join("|", expected),
                String.join("|", actualStrings)
            );
        }

        public boolean passed()
        {
            return pass;
        }

        public String getMessage()
        {
            return message;
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
        terminal.setLine(1, "ABCDEFGHIJKLMNOP", "0123456789abcdef", "fedcba9876543210");
        assertEquals( "ABCDEFGHIJKLMNOP", terminal.getLine(1).toString() );
        assertEquals( "0123456789abcdef", terminal.getTextColourLine(1).toString() );
        assertEquals( "fedcba9876543210", terminal.getBackgroundColourLine(1).toString() );
    }

    @Test
    void testDefaults()
    {
        Terminal terminal = new Terminal( 16, 9 );
        assertEquals( 0, terminal.getCursorX() );
        assertEquals( 0, terminal.getCursorY() );
        assertFalse(terminal.getCursorBlink());
        assertEquals(0, terminal.getTextColour());
        assertEquals(15, terminal.getBackgroundColour());
    }

    @Test
    void testDefaultTextBuffer()
    {
        TerminalTestHelper testHelper = new TerminalTestHelper(new Terminal( 4, 3 ));
        Result result = testHelper.textMatches(new String[] {
            "    ",
            "    ",
            "    ",
        });
        assertTrue(result.passed(), result.getMessage());
    }

    @Test
    void negativeTestDefaultTextBuffer()
    {
        TerminalTestHelper testHelper = new TerminalTestHelper(new Terminal( 4, 3 ));
        Result result = testHelper.textMatches(new String[] {
            "this",
            "fail",
            " :) ",
        });
        assertFalse(result.passed());
    }

    @Test
    void testDefaultTextColourBuffer()
    {
        TerminalTestHelper testHelper = new TerminalTestHelper(new Terminal( 4, 3 ));
        Result result = testHelper.textColourMatches(new String[] {
            "0000",
            "0000",
            "0000",
        });
        assertTrue(result.passed(), result.getMessage());
    }

    @Test
    void negativeTestDefaultTextColourBuffer()
    {
        TerminalTestHelper testHelper = new TerminalTestHelper(new Terminal( 4, 3 ));
        Result result = testHelper.textColourMatches(new String[] {
            "abcd",
            "abcd",
            "abcd",
        });
        assertFalse(result.passed(), result.getMessage());
    }

    @Test
    void testDefaultBackgroundColourBuffer()
    {
        TerminalTestHelper testHelper = new TerminalTestHelper(new Terminal( 4, 3 ));
        Result result = testHelper.backgroundColourMatches(new String[] {
            "ffff",
            "ffff",
            "ffff",
        });
        assertTrue(result.passed(), result.getMessage());
    }

    @Test
    void negativeTestDefaultBackgroundColourBuffer()
    {
        TerminalTestHelper testHelper = new TerminalTestHelper(new Terminal( 4, 3 ));
        Result result = testHelper.backgroundColourMatches(new String[] {
            "abcd",
            "abcd",
            "abcd",
        });
        assertFalse(result.passed(), result.getMessage());
    }
}