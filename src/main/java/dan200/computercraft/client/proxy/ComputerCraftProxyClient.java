/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.proxy;

import java.util.function.Supplier;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.ClientRegistry;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.GuiComputer;
import dan200.computercraft.client.gui.GuiDiskDrive;
import dan200.computercraft.client.gui.GuiPrinter;
import dan200.computercraft.client.gui.GuiPrintout;
import dan200.computercraft.client.gui.GuiTurtle;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.client.render.TurtlePlayerRenderer;
import dan200.computercraft.shared.ComputerCraftRegistry;
import dan200.computercraft.shared.common.ContainerHeldItem;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.peripheral.diskdrive.ContainerDiskDrive;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.printer.ContainerPrinter;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;

import net.minecraft.client.item.ModelPredicateProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.Item;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.mixin.object.builder.ModelPredicateProviderRegistrySpecificAccessor;

@Environment (EnvType.CLIENT)
public final class ComputerCraftProxyClient implements ClientModInitializer {

    public static void initEvents() {

    }

    @Override
    public void onInitializeClient() {
        FrameInfo.init();
        registerContainers();

        // While turtles themselves are not transparent, their upgrades may be.
        BlockRenderLayerMap.INSTANCE.putBlock(ComputerCraftRegistry.ModBlocks.TURTLE_NORMAL, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ComputerCraftRegistry.ModBlocks.TURTLE_ADVANCED, RenderLayer.getTranslucent());

        // Monitors' textures have transparent fronts and so count as cutouts.
        BlockRenderLayerMap.INSTANCE.putBlock(ComputerCraftRegistry.ModBlocks.MONITOR_NORMAL, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ComputerCraftRegistry.ModBlocks.MONITOR_ADVANCED, RenderLayer.getCutout());

        // Setup TESRs
        BlockEntityRendererRegistry.INSTANCE.register(ComputerCraftRegistry.ModTiles.MONITOR_NORMAL, TileEntityMonitorRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(ComputerCraftRegistry.ModTiles.MONITOR_ADVANCED, TileEntityMonitorRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(ComputerCraftRegistry.ModTiles.TURTLE_NORMAL, TileEntityTurtleRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(ComputerCraftRegistry.ModTiles.TURTLE_ADVANCED, TileEntityTurtleRenderer::new);

        ClientSpriteRegistryCallback.event(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
                                    .register(ClientRegistry::onTextureStitchEvent);
        ModelLoadingRegistry.INSTANCE.registerAppender(ClientRegistry::onModelBakeEvent);
        ModelLoadingRegistry.INSTANCE.registerResourceProvider(loader -> (name, context) -> TurtleModelLoader.INSTANCE.accepts(name) ?
                                                                                            TurtleModelLoader.INSTANCE.loadModel(
            name) : null);

        EntityRendererRegistry.INSTANCE.register(ComputerCraftRegistry.ModEntities.TURTLE_PLAYER, TurtlePlayerRenderer::new);

        registerItemProperty("state",
                             (stack, world, player) -> ItemPocketComputer.getState(stack)
                                                                         .ordinal(),
                             () -> ComputerCraftRegistry.ModItems.POCKET_COMPUTER_NORMAL,
                             () -> ComputerCraftRegistry.ModItems.POCKET_COMPUTER_ADVANCED);
        registerItemProperty("state",
                             (stack, world, player) -> IColouredItem.getColourBasic(stack) != -1 ? 1 : 0,
                             () -> ComputerCraftRegistry.ModItems.POCKET_COMPUTER_NORMAL,
                             () -> ComputerCraftRegistry.ModItems.POCKET_COMPUTER_ADVANCED);
        ClientRegistry.onItemColours();

        // TODO Verify this does things properly
        ServerWorldEvents.UNLOAD.register(((minecraftServer, serverWorld) -> {
            ClientMonitor.destroyAll();
        }));
    }


    // My IDE doesn't think so, but we do actually need these generics.
    private static void registerContainers() {
        ScreenRegistry.<ContainerComputer, GuiComputer<ContainerComputer>>register(ComputerCraftRegistry.ModContainers.COMPUTER, GuiComputer::create);
        ScreenRegistry.<ContainerPocketComputer, GuiComputer<ContainerPocketComputer>>register(ComputerCraftRegistry.ModContainers.POCKET_COMPUTER,
                                GuiComputer::createPocket);
        ScreenRegistry.<ContainerTurtle, GuiTurtle>register(ComputerCraftRegistry.ModContainers.TURTLE, GuiTurtle::new);

        ScreenRegistry.<ContainerPrinter, GuiPrinter>register(ComputerCraftRegistry.ModContainers.PRINTER, GuiPrinter::new);
        ScreenRegistry.<ContainerDiskDrive, GuiDiskDrive>register(ComputerCraftRegistry.ModContainers.DISK_DRIVE, GuiDiskDrive::new);
        ScreenRegistry.<ContainerHeldItem, GuiPrintout>register(ComputerCraftRegistry.ModContainers.PRINTOUT, GuiPrintout::new);

        ScreenRegistry.<ContainerViewComputer, GuiComputer<ContainerViewComputer>>register(ComputerCraftRegistry.ModContainers.VIEW_COMPUTER,
                                                                        GuiComputer::createView);
    }

    @SafeVarargs
    private static void registerItemProperty(String name, ModelPredicateProvider getter, Supplier<? extends Item>... items) {
        Identifier id = new Identifier(ComputerCraft.MOD_ID, name);
        for (Supplier<? extends Item> item : items) {
            ModelPredicateProviderRegistrySpecificAccessor.callRegister(item.get(), id, getter);
        }
    }

    //    @Mod.EventBusSubscriber (modid = ComputerCraft.MOD_ID, value = Dist.CLIENT)
    //    public static final class ForgeHandlers {
    //        @SubscribeEvent
    //        public static void onWorldUnload(WorldEvent.Unload event) {
    //            if (event.getWorld()
    //                     .isClient()) {
    //                ClientMonitor.destroyAll();
    //            }
    //        }
    //    }
}
