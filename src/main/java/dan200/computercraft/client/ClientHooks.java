/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
public class ClientHooks
{
    @SubscribeEvent
    public static void onWorldUnload( LevelEvent.Unload event )
    {
        if( event.getLevel().isClientSide() )
        {
            ClientMonitor.destroyAll();
            SpeakerManager.reset();
        }
    }

    @SubscribeEvent
    public static void onLogIn( ClientPlayerNetworkEvent.LoggingIn event )
    {
        ClientPocketComputers.reset();
    }

    @SubscribeEvent
    public static void onLogOut( ClientPlayerNetworkEvent.LoggingOut event )
    {
        ClientPocketComputers.reset();
    }
}
