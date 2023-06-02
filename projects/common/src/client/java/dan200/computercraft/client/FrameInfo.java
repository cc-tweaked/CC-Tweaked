// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client;

/**
 * Tracks the current client-side tick and frame.
 * <p>
 * These are updated via {@link ClientHooks}.
 */
public final class FrameInfo {
    private static int tick;
    private static long renderFrame;

    private FrameInfo() {
    }

    public static boolean getGlobalCursorBlink() {
        return (tick / 8) % 2 == 0;
    }

    public static long getRenderFrame() {
        return renderFrame;
    }

    public static void onTick() {
        tick++;
    }

    public static void onRenderTick() {
        renderFrame++;
    }
}
