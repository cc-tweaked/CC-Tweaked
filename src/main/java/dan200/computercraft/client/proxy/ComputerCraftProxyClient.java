/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.ClientRegistry;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.render.TileEntityCableRenderer;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.client.render.TurtleModelLoader;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.network.container.*;
import dan200.computercraft.shared.peripheral.modem.wired.TileCable;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.render.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;

public final class ComputerCraftProxyClient
{
    public static void setup()
    {
        registerContainers();

        // Setup TESRs
        BlockEntityRendererRegistry.INSTANCE.register( TileMonitor.class, new TileEntityMonitorRenderer() );
        BlockEntityRendererRegistry.INSTANCE.register( TileCable.class, new TileEntityCableRenderer() );
        BlockEntityRendererRegistry.INSTANCE.register( TileTurtle.class, new TileEntityTurtleRenderer() );

        ClientRegistry.onItemColours();
        ClientSpriteRegistryCallback.registerBlockAtlas( ClientRegistry::onTextureStitchEvent );
        ModelLoadingRegistry.INSTANCE.registerAppender( ClientRegistry::onModelBakeEvent );
        ModelLoadingRegistry.INSTANCE.registerResourceProvider( loader -> ( name, context ) ->
            TurtleModelLoader.INSTANCE.accepts( name ) ? TurtleModelLoader.INSTANCE.loadModel( name ) : null
        );
    }

    private static void registerContainers()
    {
        ContainerType.registerGui( TileEntityContainerType::computer, ( id, packet, player ) ->
            GuiComputer.create( id, (TileComputer) packet.getTileEntity( player ), player.inventory ) );
        ContainerType.registerGui( TileEntityContainerType::diskDrive, GuiDiskDrive::new );
        ContainerType.registerGui( TileEntityContainerType::printer, GuiPrinter::new );
        ContainerType.registerGui( TileEntityContainerType::turtle, ( id, packet, player ) -> {
            TileTurtle turtle = (TileTurtle) packet.getTileEntity( player );
            return new GuiTurtle( turtle, new ContainerTurtle( id, player.inventory, turtle.getAccess(), turtle.getClientComputer() ), player.inventory );
        } );

        ContainerType.registerGui( PocketComputerContainerType::new, GuiPocketComputer::new );
        ContainerType.registerGui( PrintoutContainerType::new, GuiPrintout::new );
        ContainerType.registerGui( ViewComputerContainerType::new, ( id, packet, player ) -> {
            ClientComputer computer = ComputerCraft.clientComputerRegistry.get( packet.instanceId );
            if( computer == null )
            {
                ComputerCraft.clientComputerRegistry.add( packet.instanceId, computer = new ClientComputer( packet.instanceId ) );
            }

            ContainerViewComputer container = new ContainerViewComputer( id, computer );
            return new GuiComputer<>( container, player.inventory, packet.family, computer, packet.width, packet.height );
        } );
    }

    /*
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
    */
}
