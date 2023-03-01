// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.terminal;

import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.test.core.CallCounter;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static dan200.computercraft.test.core.terminal.TerminalMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkedTerminalTest {
    @Test
    void testPacketBufferRoundtrip() {
        var writeTerminal = new NetworkedTerminal(2, 1, true);

        blit(writeTerminal, "hi", "11", "ee");
        writeTerminal.setCursorPos(2, 5);
        writeTerminal.setTextColour(3);
        writeTerminal.setBackgroundColour(5);

        var packetBuffer = new FriendlyByteBuf(Unpooled.buffer());
        writeTerminal.write(packetBuffer);

        var callCounter = new CallCounter();
        var readTerminal = new NetworkedTerminal(2, 1, true, callCounter);
        packetBuffer.writeBytes(packetBuffer);
        readTerminal.read(packetBuffer);

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
    void testNbtRoundtrip() {
        var writeTerminal = new NetworkedTerminal(10, 5, true);
        blit(writeTerminal, "hi", "11", "ee");
        writeTerminal.setCursorPos(2, 5);
        writeTerminal.setTextColour(3);
        writeTerminal.setBackgroundColour(5);

        var nbt = new CompoundTag();
        writeTerminal.writeToNBT(nbt);

        var callCounter = new CallCounter();
        var readTerminal = new NetworkedTerminal(2, 1, true, callCounter);

        readTerminal.readFromNBT(nbt);

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

        var nbt = new CompoundTag();
        terminal.writeToNBT(nbt);

        var callCounter = new CallCounter();
        terminal = new NetworkedTerminal(0, 1, true, callCounter);
        terminal.readFromNBT(nbt);

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
