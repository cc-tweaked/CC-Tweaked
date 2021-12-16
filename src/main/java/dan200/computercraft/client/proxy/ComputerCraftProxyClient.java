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
import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.fabric.events.ComputerCraftCustomEvents;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorWatcher;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;

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

        ComputerCraftCustomEvents.CLIENT_UNLOAD_WORLD_EVENT.register( () -> {
            SpeakerManager.reset();
            ClientMonitor.destroyAll();
        } );

        // Config
        ClientLifecycleEvents.CLIENT_STARTED.register( Config::clientStarted );
    }

    @Override
    public void onInitializeClient()
    {
        FrameInfo.init();
        MonitorWatcher.init();
        registerContainers();

        // While turtles themselves are not transparent, their upgrades may be.
        BlockRenderLayerMap.INSTANCE.putBlock( Registry.ModBlocks.TURTLE_NORMAL, RenderType.translucent() );
        BlockRenderLayerMap.INSTANCE.putBlock( Registry.ModBlocks.TURTLE_ADVANCED, RenderType.translucent() );
        // Monitors' textures have transparent fronts and so count as cutouts.
        BlockRenderLayerMap.INSTANCE.putBlock( Registry.ModBlocks.MONITOR_NORMAL, RenderType.cutout() );
        BlockRenderLayerMap.INSTANCE.putBlock( Registry.ModBlocks.MONITOR_ADVANCED, RenderType.cutout() );

        // Setup TESRs
        BlockEntityRendererRegistry.register( Registry.ModBlockEntities.MONITOR_NORMAL, TileEntityMonitorRenderer::new );
        BlockEntityRendererRegistry.register( Registry.ModBlockEntities.MONITOR_ADVANCED, TileEntityMonitorRenderer::new );
        BlockEntityRendererRegistry.register( Registry.ModBlockEntities.TURTLE_NORMAL, TileEntityTurtleRenderer::new );
        BlockEntityRendererRegistry.register( Registry.ModBlockEntities.TURTLE_ADVANCED, TileEntityTurtleRenderer::new );

        ClientSpriteRegistryCallback.event( InventoryMenu.BLOCK_ATLAS )
            .register( ClientRegistry::onTextureStitchEvent );
        ModelLoadingRegistry.INSTANCE.registerModelProvider( ClientRegistry::onModelBakeEvent );
        ModelLoadingRegistry.INSTANCE.registerResourceProvider( loader -> ( name, context ) -> TurtleModelLoader.INSTANCE.accepts( name ) ?
            TurtleModelLoader.INSTANCE.loadModel(
                name ) : null );

        EntityRendererRegistry.register( Registry.ModEntities.TURTLE_PLAYER, TurtlePlayerRenderer::new );

        registerItemProperty( "state",
            ( stack, world, player, integer ) -> ItemPocketComputer.getState( stack )
                .ordinal(),
            () -> Registry.ModItems.POCKET_COMPUTER_NORMAL,
            () -> Registry.ModItems.POCKET_COMPUTER_ADVANCED );
        registerItemProperty( "state",
            ( stack, world, player, integer ) -> IColouredItem.getColourBasic( stack ) != -1 ? 1 : 0,
            () -> Registry.ModItems.POCKET_COMPUTER_NORMAL,
            () -> Registry.ModItems.POCKET_COMPUTER_ADVANCED );
        ClientRegistry.onItemColours();

        initEvents();
    }

    // My IDE doesn't think so, but we do actually need these generics.
    private static void registerContainers()
    {
        ScreenRegistry.<ContainerComputerBase, GuiComputer<ContainerComputerBase>>register( Registry.ModContainers.COMPUTER, GuiComputer::create );
        ScreenRegistry.<ContainerComputerBase, GuiComputer<ContainerComputerBase>>register( Registry.ModContainers.POCKET_COMPUTER,
            GuiComputer::createPocket );
        ScreenRegistry.<ContainerComputerBase, NoTermComputerScreen<ContainerComputerBase>>register( Registry.ModContainers.POCKET_COMPUTER_NO_TERM,
            NoTermComputerScreen::new );
        ScreenRegistry.<ContainerTurtle, GuiTurtle>register( Registry.ModContainers.TURTLE, GuiTurtle::new );

        ScreenRegistry.<ContainerPrinter, GuiPrinter>register( Registry.ModContainers.PRINTER, GuiPrinter::new );
        ScreenRegistry.<ContainerDiskDrive, GuiDiskDrive>register( Registry.ModContainers.DISK_DRIVE, GuiDiskDrive::new );
        ScreenRegistry.<ContainerHeldItem, GuiPrintout>register( Registry.ModContainers.PRINTOUT, GuiPrintout::new );

        ScreenRegistry.<ContainerViewComputer, GuiComputer<ContainerViewComputer>>register( Registry.ModContainers.VIEW_COMPUTER,
            GuiComputer::createView );
    }

    @SafeVarargs
    private static void registerItemProperty( String name, ClampedItemPropertyFunction getter, Supplier<? extends Item>... items )
    {
        ResourceLocation id = new ResourceLocation( ComputerCraft.MOD_ID, name );
        for( Supplier<? extends Item> item : items )
        {
            FabricModelPredicateProviderRegistry.register( item.get(), id, getter );
        }
    }
}
