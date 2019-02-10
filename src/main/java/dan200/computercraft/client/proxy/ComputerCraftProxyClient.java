/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.render.TileEntityCableRenderer;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.shared.command.CommandCopy;
import dan200.computercraft.shared.peripheral.modem.wired.TileCable;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.proxy.ComputerCraftProxyCommon;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

public class ComputerCraftProxyClient extends ComputerCraftProxyCommon
{
    @Override
    public void preInit()
    {
        super.preInit();

        // Register any client-specific commands
        ClientCommandHandler.instance.registerCommand( CommandCopy.INSTANCE );
    }

    @Override
    public void init()
    {
        super.init();

        // Setup renderers
        ClientRegistry.bindTileEntitySpecialRenderer( TileMonitor.class, new TileEntityMonitorRenderer() );
        ClientRegistry.bindTileEntitySpecialRenderer( TileCable.class, new TileEntityCableRenderer() );
    }

    @Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Side.CLIENT )
    public static class ForgeHandlers
    {
        @SubscribeEvent
        public static void onWorldUnload( WorldEvent.Unload event )
        {
            if( event.getWorld().isRemote )
            {
                ClientMonitor.destroyAll();
            }
        }
    }


}
