// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.api;

import dan200.computercraft.gametest.core.TestAPI;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestSequence;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Assertion state of a computer.
 *
 * @see TestAPI For the Lua interface for this.
 * @see TestExtensionsKt#thenComputerOk(GameTestSequence, String, String)
 */
public class ComputerState {
    public static final String DONE = "DONE";

    protected static final Map<String, ComputerState> lookup = new ConcurrentHashMap<>();

    protected final Set<String> markers = new HashSet<>();
    protected @Nullable String error;

    public boolean isDone(String marker) {
        return markers.contains(marker);
    }

    public void check(String marker) {
        if (!markers.contains(marker)) throw new IllegalStateException("Not yet at " + marker);
        if (error != null) throw new GameTestAssertException(error);
    }

    public static @Nullable ComputerState get(String label) {
        return lookup.get(label);
    }
}
