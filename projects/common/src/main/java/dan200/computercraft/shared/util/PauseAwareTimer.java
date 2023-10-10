// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import net.minecraft.Util;

/**
 * A monotonically increasing clock which accounts for the game being paused.
 */
public final class PauseAwareTimer {
    private static boolean paused;
    private static long pauseTime;
    private static long pauseOffset;

    private PauseAwareTimer() {
    }

    public static long getTime() {
        return (paused ? pauseTime : Util.getNanos()) - pauseOffset;
    }

    public static void tick(boolean isPaused) {
        if (isPaused == paused) return;

        if (isPaused) {
            pauseTime = Util.getNanos();
            paused = true;
        } else {
            pauseOffset += Util.getNanos() - pauseTime;
            paused = false;
        }
    }
}
