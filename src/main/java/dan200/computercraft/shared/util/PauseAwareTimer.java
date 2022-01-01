/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A monotonically increasing clock which accounts for the game being paused.
 */
@Mod.EventBusSubscriber( Dist.CLIENT )
public final class PauseAwareTimer
{
    private static boolean paused;
    private static long pauseTime;
    private static long pauseOffset;

    private PauseAwareTimer()
    {
    }

    public static long getTime()
    {
        return (paused ? pauseTime : Util.getNanos()) - pauseOffset;
    }

    @SubscribeEvent
    public static void tick( TickEvent.RenderTickEvent event )
    {
        if( event.phase != TickEvent.Phase.START ) return;

        boolean isPaused = Minecraft.getInstance().isPaused();
        if( isPaused == paused ) return;

        if( isPaused )
        {
            pauseTime = Util.getNanos();
            paused = true;
        }
        else
        {
            pauseOffset += Util.getNanos() - pauseTime;
            paused = false;
        }
    }
}
