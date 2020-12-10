/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.client.render.TurtlePlayerRenderer;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
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

        // While turtles themselves are not transparent, their upgrades may be.
        RenderTypeLookup.setRenderLayer( Registry.ModBlocks.TURTLE_NORMAL.get(), RenderType.getTranslucent() );
        RenderTypeLookup.setRenderLayer( Registry.ModBlocks.TURTLE_ADVANCED.get(), RenderType.getTranslucent() );

        // Monitors' textures have transparent fronts and so count as cutouts.
        RenderTypeLookup.setRenderLayer( Registry.ModBlocks.MONITOR_NORMAL.get(), RenderType.getCutout() );
        RenderTypeLookup.setRenderLayer( Registry.ModBlocks.MONITOR_ADVANCED.get(), RenderType.getCutout() );

        // Setup TESRs
        ClientRegistry.bindTileEntityRenderer( Registry.ModTiles.MONITOR_NORMAL.get(), TileEntityMonitorRenderer::new );
        ClientRegistry.bindTileEntityRenderer( Registry.ModTiles.MONITOR_ADVANCED.get(), TileEntityMonitorRenderer::new );
        ClientRegistry.bindTileEntityRenderer( Registry.ModTiles.TURTLE_NORMAL.get(), TileEntityTurtleRenderer::new );
        ClientRegistry.bindTileEntityRenderer( Registry.ModTiles.TURTLE_ADVANCED.get(), TileEntityTurtleRenderer::new );

        RenderingRegistry.registerEntityRenderingHandler( Registry.ModEntities.TURTLE_PLAYER.get(), TurtlePlayerRenderer::new );
    }

    private static void registerContainers()
    {
        // My IDE doesn't think so, but we do actually need these generics.

        ScreenManager.<ContainerComputer, GuiComputer<ContainerComputer>>registerFactory( Registry.ModContainers.COMPUTER.get(), GuiComputer::create );
        ScreenManager.<ContainerPocketComputer, GuiComputer<ContainerPocketComputer>>registerFactory( Registry.ModContainers.POCKET_COMPUTER.get(), GuiComputer::createPocket );
        ScreenManager.registerFactory( Registry.ModContainers.TURTLE.get(), GuiTurtle::new );

        ScreenManager.registerFactory( Registry.ModContainers.PRINTER.get(), GuiPrinter::new );
        ScreenManager.registerFactory( Registry.ModContainers.DISK_DRIVE.get(), GuiDiskDrive::new );
        ScreenManager.registerFactory( Registry.ModContainers.PRINTOUT.get(), GuiPrintout::new );

        ScreenManager.<ContainerViewComputer, GuiComputer<ContainerViewComputer>>registerFactory( Registry.ModContainers.VIEW_COMPUTER.get(), GuiComputer::createView );
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
