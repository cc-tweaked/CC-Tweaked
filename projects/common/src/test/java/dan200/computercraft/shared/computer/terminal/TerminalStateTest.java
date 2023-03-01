// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.terminal;

import dan200.computercraft.core.terminal.Terminal;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link TerminalState} round tripping works as expected.
 */
public class TerminalStateTest {
    @RepeatedTest(5)
    public void testCompressed() {
        var terminal = randomTerminal();

        var buffer = new FriendlyByteBuf(Unpooled.directBuffer());
        new TerminalState(terminal, true).write(buffer);

        checkEqual(terminal, read(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    @RepeatedTest(5)
    public void testUncompressed() {
        var terminal = randomTerminal();

        var buffer = new FriendlyByteBuf(Unpooled.directBuffer());
        new TerminalState(terminal, false).write(buffer);

        checkEqual(terminal, read(buffer));
        assertEquals(0, buffer.readableBytes());
    }

    private static NetworkedTerminal randomTerminal() {
        var random = new Random();
        var terminal = new NetworkedTerminal(10, 5, true);
        for (var y = 0; y < terminal.getHeight(); y++) {
            var buffer = terminal.getLine(y);
            for (var x = 0; x < buffer.length(); x++) buffer.setChar(x, (char) (random.nextInt(26) + 65));
        }

        return terminal;
    }

    private static void checkEqual(Terminal expected, Terminal actual) {
        assertNotNull(expected, "Expected cannot be null");
        assertNotNull(actual, "Actual cannot be null");
        assertEquals(expected.getHeight(), actual.getHeight(), "Heights must match");
        assertEquals(expected.getWidth(), actual.getWidth(), "Widths must match");

        for (var y = 0; y < expected.getHeight(); y++) {
            assertEquals(expected.getLine(y).toString(), actual.getLine(y).toString());
        }
    }

    private static NetworkedTerminal read(FriendlyByteBuf buffer) {
        var state = new TerminalState(buffer);
        assertTrue(state.colour);

        if (!state.hasTerminal()) return null;

        var other = new NetworkedTerminal(state.width, state.height, true);
        state.apply(other);
        return other;
    }
}
