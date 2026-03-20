// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.peripheral.speaker;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.ObjectLuaTable;
import dan200.computercraft.shared.util.PauseAwareTimer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DfpwmStateTest {
    @Test
    public void testEncoder() throws LuaException {
        var input = new int[]{ 4, 4, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -2, -2, -2, -2, -2, -3, -3, -3, -4, -4, -4, -4, -4, -5, -5, -5, -5, -5, -6, -6, -6, -7, -7, -7, -7, -7, -7, -7, -7, -7, -8, -8, -8, -8, -8, -8, -8, -8, -8, -8, -8, -8, -8, -8, -8, -8, -7, -7, -7, -7, -7, -7, -7, -7, -7, -6, -6, -6, -6, -6, -6, -6, -6, -6, -5, -5, -5, -5, -5, -5, -5, -4, -4, -4, -4, -4, -3, -3, -3, -3, -3, -3, -3, -2, -2, -2, -2, -2, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -2, -2, -2, -2, -2, -2, -2, -2, -2, -3, -3, -3, -3, -3, -3, -3, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -4, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -2, -2, -2, -2, -2, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -2, -2, -2, -2, -2, -3, -3, -3, -3, -3, -4, -4, -4, -4, -4, -5, -5, -5, -5, -5, -5, -5, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -7, -7, -7, -7, -7, -7, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -4, -4, -4, -4, -4, -4, -4, -4, -4, -3, -3, -3, -3, -3, -2, -2, -2, -2, -2, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -2, -2, -2, -2, -2, -2, -2, -2, -2, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -2, -2, -2, -2, -2, -2, -2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -2, -2, -2, -2, -2, -3, -3, -3, -3, -3, -4, -4, -4, -4, -4, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -5, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -6, -5, -5, -5, -5, -5, -5, -5, -4, -4, -4, -4, -4, -4, -4, -3, -3, -3, -3, -3, -3, -3, -2, -2, -2, -2, -2, -2, -2, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3 };
        Map<Object, Object> inputTbl = new HashMap<>();
        for (var i = 0; i < input.length; i++) inputTbl.put((double) (i + 1), input[i]);

        var state = new DfpwmState();
        state.pushBuffer(new ObjectLuaTable(inputTbl), input.length, Optional.empty());
        var result = state.pullPending(0).audio();
        var contents = new byte[result.remaining()];
        result.get(contents);

        assertArrayEquals(
            new byte[]{ 87, 74, 42, -91, -92, -108, 84, -87, -86, 86, -83, 90, -83, -43, 90, -85, -42, 106, -43, -86, 106, -107, 42, -107, 74, -87, 74, -91, 74, -91, -86, -86, 106, 85, 107, -83, 106, -83, -83, 86, -75, -86, 42, 85, -107, 82, 41, -91, 82, 74, 41, -107, -86, -44, -86, 86, -75, 106, -83, -75, -86, -75, 90, -83, -86, -86, -86, 82, -91, 74, -107, -86, 82, -87, 82, 85, 85, 85, -83, 86, -75, -86, -43, 90, -83, 90, 85, 85, -107, 42, -91, 82, -86, 82, 74, 41, 85, -87, -86, -86, 106, -75, 90, -83, 86, -85, 106, -43, 106, 85, 85, 85, 85, -107, 42, 85, -86, 42, -107, -86, -86, -86, -86, 106, -75, -86, 86, -85 },
            contents
        );
    }

    @Test
    public void testSampleOffsetAdvances() throws LuaException {
        var state = new DfpwmState();
        // Create a table with 1024 PCM samples (all zeros for simplicity)
        Map<Object, Object> inputTbl = new HashMap<>();
        for (var i = 1; i <= 1024; i++) inputTbl.put((double) i, 0);

        assertTrue(state.pushBuffer(new ObjectLuaTable(inputTbl), 1024, Optional.empty()));

        // Pull first chunk — offset should be 0
        var audio = state.pullPending(PauseAwareTimer.getTime());
        assertEquals(0L, audio.sampleOffset());

        // Push another buffer
        assertTrue(state.pushBuffer(new ObjectLuaTable(inputTbl), 1024, Optional.empty()));
        var audio2 = state.pullPending(PauseAwareTimer.getTime());
        // 1024 samples encode to 128 DFPWM bytes, which represent 1024 samples
        assertEquals(1024L, audio2.sampleOffset());
    }

    @Test
    public void testShouldSendPendingWhenBufferAvailable() throws LuaException {
        var state = new DfpwmState();
        assertFalse(state.shouldSendPending());
        Map<Object, Object> inputTbl = new HashMap<>();
        for (var i = 1; i <= 1024; i++) inputTbl.put((double) i, 0);
        state.pushBuffer(new ObjectLuaTable(inputTbl), 1024, Optional.empty());
        assertTrue(state.shouldSendPending());
    }

    @Test
    public void testIsPlayingAfterPull() throws LuaException {
        var state = new DfpwmState();
        Map<Object, Object> inputTbl = new HashMap<>();
        for (var i = 1; i <= 1024; i++) inputTbl.put((double) i, 0);
        state.pushBuffer(new ObjectLuaTable(inputTbl), 1024, Optional.empty());
        state.pullPending(PauseAwareTimer.getTime());
        assertTrue(state.isPlaying());
    }
}
