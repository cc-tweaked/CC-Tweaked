/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public final class FrameInfo
{
    private static int tick;
    private static long renderFrame;

    private FrameInfo()
    {
    }

    public static boolean getGlobalCursorBlink()
    {
        return (tick / 8) % 2 == 0;
    }

    public static long getRenderFrame()
    {
        return renderFrame;
    }

    @SubscribeEvent
    public static void onTick( TickEvent.ClientTickEvent event )
    {
        if( event.phase == TickEvent.Phase.START ) tick++;
    }

    @SubscribeEvent
    public static void onRenderTick( TickEvent.RenderTickEvent event )
    {
        if( event.phase == TickEvent.Phase.START ) renderFrame++;
    }
}
