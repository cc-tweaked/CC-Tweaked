/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class FrameInfo
{
    private static final FrameInfo instance = new FrameInfo();

    public static FrameInfo instance()
    {
        return instance;
    }

    private int tick;
    private long renderFrame;

    private FrameInfo()
    {
    }

    public boolean getGlobalCursorBlink()
    {
        return (tick / 8) % 2 == 0;
    }

    public long getRenderFrame()
    {
        return renderFrame;
    }

    @SubscribeEvent
    public void onTick( TickEvent.ClientTickEvent event )
    {
        if( event.phase == TickEvent.Phase.START ) tick++;
    }

    @SubscribeEvent
    public void onRenderTick( TickEvent.RenderTickEvent event )
    {
        if( event.phase == TickEvent.Phase.START ) renderFrame++;
    }
}
