/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class FrameInfo
{
    private static int tick;
    private static long renderFrame;

    static
    {

    }

    private FrameInfo()
    {
    }

    public static void init()
    {
        ClientTickEvents.START_CLIENT_TICK.register( m -> {
            tick++;
        } );
    }

    public static boolean getGlobalCursorBlink()
    {
        return (tick / 8) % 2 == 0;
    }

    public static long getRenderFrame()
    {
        return renderFrame;
    }

    // TODO Call this in a callback
    public static void onTick()
    {
        tick++;
    }

    // TODO Call this in a callback
    public static void onRenderFrame()
    {
        renderFrame++;
    }
}
