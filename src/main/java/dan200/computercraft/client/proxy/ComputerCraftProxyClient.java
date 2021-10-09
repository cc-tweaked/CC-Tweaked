/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.ClientRegistry;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.client.render.TurtlePlayerRenderer;
import dan200.computercraft.fabric.events.ClientUnloadWorldEvent;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import dan200.computercraft.shared.util.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.item.UnclampedModelPredicateProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.Item;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

@Environment( EnvType.CLIENT )
public final class ComputerCraftProxyClient implements ClientModInitializer
{

    private static void initEvents()
    {
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register( ( blockEntity, world ) -> {
            if( blockEntity instanceof TileGeneric )
            {
                ((TileGeneric) blockEntity).onChunkUnloaded();
            }
        } );

        ClientUnloadWorldEvent.EVENT.register( () -> ClientMonitor.destroyAll() );

        // Config
        ClientLifecycleEvents.CLIENT_STARTED.register( Config::clientStarted );
    }

    @Override
    public void onInitializeClient()
    {
        FrameInfo.init();
        registerContainers();

        // While turtles themselves are not transparent, their upgrades may be.
        BlockRenderLayerMap.INSTANCE.putBlock( ComputerCraftRegistry.ModBlocks.TURTLE_NORMAL, RenderLayer.getTranslucent() );
        BlockRenderLayerMap.INSTANCE.putBlock( ComputerCraftRegistry.ModBlocks.TURTLE_ADVANCED, RenderLayer.getTranslucent() );
        // Monitors' textures have transparent fronts and so count as cutouts.
        BlockRenderLayerMap.INSTANCE.putBlock( ComputerCraftRegistry.ModBlocks.MONITOR_NORMAL, RenderLayer.getCutout() );
        BlockRenderLayerMap.INSTANCE.putBlock( ComputerCraftRegistry.ModBlocks.MONITOR_ADVANCED, RenderLayer.getCutout() );

        // Setup TESRs
        BlockEntityRendererRegistry.register( ComputerCraftRegistry.ModTiles.MONITOR_NORMAL, TileEntityMonitorRenderer::new );
        BlockEntityRendererRegistry.register( ComputerCraftRegistry.ModTiles.MONITOR_ADVANCED, TileEntityMonitorRenderer::new );
        BlockEntityRendererRegistry.register( ComputerCraftRegistry.ModTiles.TURTLE_NORMAL, TileEntityTurtleRenderer::new );
        BlockEntityRendererRegistry.register( ComputerCraftRegistry.ModTiles.TURTLE_ADVANCED, TileEntityTurtleRenderer::new );

        ClientSpriteRegistryCallback.event( PlayerScreenHandler.BLOCK_ATLAS_TEXTURE )
            .register( ClientRegistry::onTextureStitchEvent );
        ModelLoadingRegistry.INSTANCE.registerModelProvider( ClientRegistry::onModelBakeEvent );
        ModelLoadingRegistry.INSTANCE.registerResourceProvider( loader -> ( name, context ) -> TurtleModelLoader.INSTANCE.accepts( name ) ?
            TurtleModelLoader.INSTANCE.loadModel(
                name ) : null );

        EntityRendererRegistry.register( ComputerCraftRegistry.ModEntities.TURTLE_PLAYER, TurtlePlayerRenderer::new );

        registerItemProperty( "state",
            ( stack, world, player, integer ) -> ItemPocketComputer.getState( stack )
                .ordinal(),
            () -> ComputerCraftRegistry.ModItems.POCKET_COMPUTER_NORMAL,
            () -> ComputerCraftRegistry.ModItems.POCKET_COMPUTER_ADVANCED );
        registerItemProperty( "state",
            ( stack, world, player, integer ) -> IColouredItem.getColourBasic( stack ) != -1 ? 1 : 0,
            () -> ComputerCraftRegistry.ModItems.POCKET_COMPUTER_NORMAL,
            () -> ComputerCraftRegistry.ModItems.POCKET_COMPUTER_ADVANCED );
        ClientRegistry.onItemColours();

        initEvents();
    }

    // My IDE doesn't think so, but we do actually need these generics.
    private static void registerContainers()
    {
        ScreenRegistry.<ContainerComputerBase, GuiComputer<ContainerComputerBase>>register( ComputerCraftRegistry.ModContainers.COMPUTER, GuiComputer::create );
        ScreenRegistry.<ContainerComputerBase, GuiComputer<ContainerComputerBase>>register( ComputerCraftRegistry.ModContainers.POCKET_COMPUTER,
            GuiComputer::createPocket );
        ScreenRegistry.<ContainerComputerBase, NoTermComputerScreen<ContainerComputerBase>>register( ComputerCraftRegistry.ModContainers.POCKET_COMPUTER_NO_TERM,
            NoTermComputerScreen::new );
        ScreenRegistry.<ContainerTurtle, GuiTurtle>register( ComputerCraftRegistry.ModContainers.TURTLE, GuiTurtle::new );

        ScreenRegistry.<ContainerPrinter, GuiPrinter>register( ComputerCraftRegistry.ModContainers.PRINTER, GuiPrinter::new );
        ScreenRegistry.<ContainerDiskDrive, GuiDiskDrive>register( ComputerCraftRegistry.ModContainers.DISK_DRIVE, GuiDiskDrive::new );
        ScreenRegistry.<ContainerHeldItem, GuiPrintout>register( ComputerCraftRegistry.ModContainers.PRINTOUT, GuiPrintout::new );

        ScreenRegistry.<ContainerViewComputer, GuiComputer<ContainerViewComputer>>register( ComputerCraftRegistry.ModContainers.VIEW_COMPUTER,
            GuiComputer::createView );
    }

    @SafeVarargs
    private static void registerItemProperty( String name, UnclampedModelPredicateProvider getter, Supplier<? extends Item>... items )
    {
        Identifier id = new Identifier( ComputerCraft.MOD_ID, name );
        for( Supplier<? extends Item> item : items )
        {
            FabricModelPredicateProviderRegistry.register( item.get(), id, getter );
        }
    }
}
