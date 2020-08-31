/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.ClientRegistry;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.GuiComputer;
import dan200.computercraft.client.gui.GuiDiskDrive;
import dan200.computercraft.client.gui.GuiPocketComputer;
import dan200.computercraft.client.gui.GuiPrinter;
import dan200.computercraft.client.gui.GuiPrintout;
import dan200.computercraft.client.gui.GuiTurtle;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.network.container.ContainerType;
import dan200.computercraft.shared.network.container.PocketComputerContainerType;
import dan200.computercraft.shared.network.container.PrintoutContainerType;
import dan200.computercraft.shared.network.container.TileEntityContainerType;
import dan200.computercraft.shared.network.container.ViewComputerContainerType;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;

import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ArrayPropertyDelegate;

import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;

@SuppressWarnings ({
    "MethodCallSideOnly",
    "NewExpressionSideOnly"
})
public final class ComputerCraftProxyClient {
    public static void setup() {
        registerContainers();
        BlockEntityRendererRegistry.INSTANCE.register(Registry.ModTiles.MONITOR_NORMAL, TileEntityMonitorRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(Registry.ModTiles.MONITOR_ADVANCED, TileEntityMonitorRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(Registry.ModTiles.TURTLE_NORMAL, TileEntityTurtleRenderer::new);
        BlockEntityRendererRegistry.INSTANCE.register(Registry.ModTiles.TURTLE_ADVANCED, TileEntityTurtleRenderer::new);

        ClientSpriteRegistryCallback.event(SpriteAtlasTexture.BLOCK_ATLAS_TEX)
                                    .register(ClientRegistry::onTextureStitchEvent);
        ModelLoadingRegistry.INSTANCE.registerAppender(ClientRegistry::onModelBakeEvent);
        ModelLoadingRegistry.INSTANCE.registerResourceProvider(loader -> (name, context) -> TurtleModelLoader.INSTANCE.accepts(name) ?
                                                                                            TurtleModelLoader.INSTANCE.loadModel(
            name) : null);

        ClientTickCallback.EVENT.register(client -> FrameInfo.onTick());
    }

    private static void registerContainers() {
        // My IDE doesn't think so, but we do actually need these generics.
        ContainerType.registerGui(TileEntityContainerType::computer,
                                  (id, packet, player) -> GuiComputer.create(id, (TileComputer) packet.getTileEntity(player), player.inventory));
        ContainerType.registerGui(TileEntityContainerType::diskDrive, GuiDiskDrive::new);
        ContainerType.registerGui(TileEntityContainerType::printer, GuiPrinter::new);
        ContainerType.registerGui(TileEntityContainerType::turtle, (id, packet, player) -> {
            TileTurtle turtle = (TileTurtle) packet.getTileEntity(player);
            return new GuiTurtle(turtle,
                                 new ContainerTurtle(id, player.inventory, new SimpleInventory(TileTurtle.INVENTORY_SIZE), new ArrayPropertyDelegate(1)),
                                 player.inventory);
        });

        ContainerType.registerGui(PocketComputerContainerType::new, GuiPocketComputer::new);
        ContainerType.registerGui(PrintoutContainerType::new, GuiPrintout::new);
        ContainerType.registerGui(ViewComputerContainerType::new, (id, packet, player) -> {
            ClientComputer computer = ComputerCraft.clientComputerRegistry.get(packet.instanceId);
            if (computer == null) {
                ComputerCraft.clientComputerRegistry.add(packet.instanceId, computer = new ClientComputer(packet.instanceId));
            }
            ContainerViewComputer container = new ContainerViewComputer(id, computer);
            return new GuiComputer<>(container, player.inventory, packet.family, computer, packet.width, packet.height);
        });
    }
}
