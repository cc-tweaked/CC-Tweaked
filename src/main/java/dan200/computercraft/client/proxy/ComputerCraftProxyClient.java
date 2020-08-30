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
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.Supplier;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class ComputerCraftProxyClient
{
    @SubscribeEvent
    public static void setupClient( FMLClientSetupEvent event )
    {
        registerContainers();

        // While turtles themselves are not transparent, their upgrades may be.
        RenderLayers.setRenderLayer( Registry.ModBlocks.TURTLE_NORMAL.get(), RenderLayer.getTranslucent() );
        RenderLayers.setRenderLayer( Registry.ModBlocks.TURTLE_ADVANCED.get(), RenderLayer.getTranslucent() );

        // Monitors' textures have transparent fronts and so count as cutouts.
        RenderLayers.setRenderLayer( Registry.ModBlocks.MONITOR_NORMAL.get(), RenderLayer.getCutout() );
        RenderLayers.setRenderLayer( Registry.ModBlocks.MONITOR_ADVANCED.get(), RenderLayer.getCutout() );

        // Setup TESRs
        ClientRegistry.bindTileEntityRenderer( Registry.ModTiles.MONITOR_NORMAL.get(), TileEntityMonitorRenderer::new );
        ClientRegistry.bindTileEntityRenderer( Registry.ModTiles.MONITOR_ADVANCED.get(), TileEntityMonitorRenderer::new );
        ClientRegistry.bindTileEntityRenderer( Registry.ModTiles.TURTLE_NORMAL.get(), TileEntityTurtleRenderer::new );
        ClientRegistry.bindTileEntityRenderer( Registry.ModTiles.TURTLE_ADVANCED.get(), TileEntityTurtleRenderer::new );
        // TODO: ClientRegistry.bindTileEntityRenderer( TileCable.FACTORY, x -> new TileEntityCableRenderer() );

        RenderingRegistry.registerEntityRenderingHandler( Registry.ModEntities.TURTLE_PLAYER.get(), TurtlePlayerRenderer::new );

        registerItemProperty( "state",
            ( stack, world, player ) -> ItemPocketComputer.getState( stack ).ordinal(),
            Registry.ModItems.POCKET_COMPUTER_NORMAL, Registry.ModItems.POCKET_COMPUTER_ADVANCED
        );
        registerItemProperty( "state",
            ( stack, world, player ) -> IColouredItem.getColourBasic( stack ) != -1 ? 1 : 0,
            Registry.ModItems.POCKET_COMPUTER_NORMAL, Registry.ModItems.POCKET_COMPUTER_ADVANCED
        );
    }

    @SafeVarargs
    private static void registerItemProperty( String name, ModelPredicateProvider getter, Supplier<? extends Item>... items )
    {
        Identifier id = new Identifier( ComputerCraft.MOD_ID, name );
        for( Supplier<? extends Item> item : items )
        {
            ModelPredicateProviderRegistry.register( item.get(), id, getter );
        }
    }

    private static void registerContainers()
    {
        // My IDE doesn't think so, but we do actually need these generics.

        HandledScreens.<ContainerComputer, GuiComputer<ContainerComputer>>register( Registry.ModContainers.COMPUTER.get(), GuiComputer::create );
        HandledScreens.<ContainerPocketComputer, GuiComputer<ContainerPocketComputer>>register( Registry.ModContainers.POCKET_COMPUTER.get(), GuiComputer::createPocket );
        HandledScreens.register( Registry.ModContainers.TURTLE, GuiTurtle::new );

        HandledScreens.register( Registry.ModContainers.PRINTER, GuiPrinter::new );
        HandledScreens.register( Registry.ModContainers.DISK_DRIVE, GuiDiskDrive::new );
        HandledScreens.register( Registry.ModContainers.PRINTOUT, GuiPrintout::new );

        HandledScreens.<ContainerViewComputer, GuiComputer<ContainerViewComputer>>register( Registry.ModContainers.VIEW_COMPUTER.get(), GuiComputer::createView );
    }

    @Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, value = Dist.CLIENT )
    public static final class ForgeHandlers
    {
        @SubscribeEvent
        public static void onWorldUnload( WorldEvent.Unload event )
        {
            if( event.getWorld().isClient() )
            {
                ClientMonitor.destroyAll();
            }
        }
    }
}
