// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.terminal;

import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.test.core.CallCounter;
import dan200.computercraft.test.shared.SerialisationUtils;
import org.junit.jupiter.api.Test;

import static dan200.computercraft.test.core.terminal.TerminalMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkedTerminalTest {
    @Test
    void testNbtRoundtrip() {
        var writeTerminal = new NetworkedTerminal(10, 5, true);
        blit(writeTerminal, "hi", "11", "ee");
        writeTerminal.setCursorPos(2, 5);
        writeTerminal.setTextColour(3);
        writeTerminal.setBackgroundColour(5);

        var nbt = SerialisationUtils.writeNBT(writeTerminal::writeToNBT);

        var callCounter = new CallCounter();
        var readTerminal = new NetworkedTerminal(2, 1, true, callCounter);

        SerialisationUtils.readNBT(nbt, readTerminal::readFromNBT);

        assertThat(readTerminal, allOf(
            textMatches(new String[]{ "hi", }),
            textColourMatches(new String[]{ "11", }),
            backgroundColourMatches(new String[]{ "ee", })
        ));

        assertEquals(2, readTerminal.getCursorX());
        assertEquals(5, readTerminal.getCursorY());
        assertEquals(3, readTerminal.getTextColour());
        assertEquals(5, readTerminal.getBackgroundColour());
        callCounter.assertCalledTimes(1);
    }

    @Test
    void testReadWriteNBTEmpty() {
        var terminal = new NetworkedTerminal(0, 0, true);

        var nbt = SerialisationUtils.writeNBT(terminal::writeToNBT);

        var callCounter = new CallCounter();
        terminal = new NetworkedTerminal(0, 1, true, callCounter);
        SerialisationUtils.readNBT(nbt, terminal::readFromNBT);

        assertThat(terminal, allOf(
            textMatches(new String[]{ "", }),
            textColourMatches(new String[]{ "", }),
            backgroundColourMatches(new String[]{ "", })
        ));

        assertEquals(0, terminal.getCursorX());
        assertEquals(0, terminal.getCursorY());
        assertEquals(0, terminal.getTextColour());
        assertEquals(15, terminal.getBackgroundColour());
        callCounter.assertCalledTimes(1);
    }

    private static void blit(Terminal terminal, String text, String fg, String bg) {
        terminal.blit(LuaValues.encode(text), LuaValues.encode(fg), LuaValues.encode(bg));
    }
}
