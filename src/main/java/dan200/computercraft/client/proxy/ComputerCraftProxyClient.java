/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.render.TileEntityCableRenderer;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.client.render.TurtlePlayerRenderer;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.modem.wired.TileCable;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class ComputerCraftProxyClient
{
    @SubscribeEvent
    public static void setupClient( FMLClientSetupEvent event )
    {
        registerContainers();

        // Setup TESRs
        ClientRegistry.bindTileEntitySpecialRenderer( TileMonitor.class, new TileEntityMonitorRenderer() );
        ClientRegistry.bindTileEntitySpecialRenderer( TileCable.class, new TileEntityCableRenderer() );
        ClientRegistry.bindTileEntitySpecialRenderer( TileTurtle.class, new TileEntityTurtleRenderer() );

        RenderingRegistry.registerEntityRenderingHandler( TurtlePlayer.class, TurtlePlayerRenderer::new );
    }

    private static void registerContainers()
    {
        // My IDE doesn't think so, but we do actually need these generics.

        ScreenManager.<ContainerComputer, GuiComputer<ContainerComputer>>registerFactory( ContainerComputer.TYPE, GuiComputer::create );
        ScreenManager.<ContainerPocketComputer, GuiComputer<ContainerPocketComputer>>registerFactory( ContainerPocketComputer.TYPE, GuiComputer::createPocket );
        ScreenManager.registerFactory( ContainerTurtle.TYPE, GuiTurtle::new );

        ScreenManager.registerFactory( ContainerPrinter.TYPE, GuiPrinter::new );
        ScreenManager.registerFactory( ContainerDiskDrive.TYPE, GuiDiskDrive::new );
        ScreenManager.registerFactory( ContainerHeldItem.PRINTOUT_TYPE, GuiPrintout::new );

        ScreenManager.<ContainerViewComputer, GuiComputer<ContainerViewComputer>>registerFactory( ContainerViewComputer.TYPE, GuiComputer::createView );
    }

    @Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
    public static final class ForgeHandlers
    {
        @SubscribeEvent
        public static void onWorldUnload( WorldEvent.Unload event )
        {
            if( event.getWorld().isRemote() )
            {
                ClientMonitor.destroyAll();
            }
        }
    }
}
