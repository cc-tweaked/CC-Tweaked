/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

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

    public static void onRenderFrame() {
        renderFrame++;
    }
}
