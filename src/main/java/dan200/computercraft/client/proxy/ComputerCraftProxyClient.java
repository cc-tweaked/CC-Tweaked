/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.*;
import dan200.computercraft.client.render.TileEntityCableRenderer;
import dan200.computercraft.client.render.TileEntityMonitorRenderer;
import dan200.computercraft.client.render.TileEntityTurtleRenderer;
import dan200.computercraft.shared.computer.blocks.TileComputer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.network.container.*;
import dan200.computercraft.shared.peripheral.modem.wired.TileCable;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.turtle.blocks.TileTurtle;
import dan200.computercraft.shared.turtle.inventory.ContainerTurtle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.BiFunction;

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
    }

    private static void registerContainers()
    {
        ContainerType.registerGui( TileEntityContainerType::computer, ( packet, player ) ->
            new GuiComputer( (TileComputer) packet.getTileEntity( player ) ) );
        ContainerType.registerGui( TileEntityContainerType::diskDrive, GuiDiskDrive::new );
        ContainerType.registerGui( TileEntityContainerType::printer, GuiPrinter::new );
        ContainerType.registerGui( TileEntityContainerType::turtle, ( packet, player ) -> {
            TileTurtle turtle = (TileTurtle) packet.getTileEntity( player );
            return new GuiTurtle( turtle, new ContainerTurtle( player.inventory, turtle.getAccess(), turtle.getClientComputer() ) );
        } );

        ContainerType.registerGui( PocketComputerContainerType::new, GuiPocketComputer::new );
        ContainerType.registerGui( PrintoutContainerType::new, GuiPrintout::new );
        ContainerType.registerGui( ViewComputerContainerType::new, ( packet, player ) -> {
            ClientComputer computer = ComputerCraft.clientComputerRegistry.get( packet.instanceId );
            if( computer == null )
            {
                ComputerCraft.clientComputerRegistry.add( packet.instanceId, computer = new ClientComputer( packet.instanceId ) );
            }

            ContainerViewComputer container = new ContainerViewComputer( computer );
            return new GuiComputer( container, packet.family, computer, packet.width, packet.height );
        } );

        ModLoadingContext.get().registerExtensionPoint( ExtensionPoint.GUIFACTORY, () -> packet -> {
            ContainerType<?> type = ContainerType.factories.get( packet.getId() ).get();
            if( packet.getAdditionalData() != null ) type.fromBytes( packet.getAdditionalData() );
            return ((BiFunction<ContainerType<?>, EntityPlayer, GuiContainer>) ContainerType.guiFactories.get( packet.getId() ))
                .apply( type, Minecraft.getInstance().player );
        } );
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
