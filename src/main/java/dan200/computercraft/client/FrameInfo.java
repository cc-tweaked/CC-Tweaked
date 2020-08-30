/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class FrameInfo {
    private static int tick;
    private static long renderFrame;
    static {

    }

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(m -> {
            tick++;
        });
    }

    private FrameInfo() {
    }

    public static boolean getGlobalCursorBlink() {
        return (tick / 8) % 2 == 0;
    }

    public static long getRenderFrame() {
        return renderFrame;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            renderFrame++;
        }
    }
}
