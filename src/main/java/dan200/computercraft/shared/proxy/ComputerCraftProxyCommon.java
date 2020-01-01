/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.shared.Config;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.datafix.Fixes;
import dan200.computercraft.shared.integration.charset.IntegrationCharset;
import dan200.computercraft.shared.media.items.RecordMedia;
import dan200.computercraft.shared.network.Containers;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheral;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.CreativeTabMain;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import net.minecraft.command.CommandHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import pl.asie.charset.ModCharset;

public class ComputerCraftProxyCommon
{
    public void preInit()
    {
        NetworkHandler.setup();

        ComputerCraft.mainCreativeTab = new CreativeTabMain( CreativeTabs.getNextID() );

        EntityRegistry.registerModEntity(
            new ResourceLocation( ComputerCraft.MOD_ID, "turtle_player" ), TurtlePlayer.class, "turtle_player",
            0, ComputerCraft.instance, Integer.MAX_VALUE, Integer.MAX_VALUE, false
        );
    }

    public void init()
    {
        registerProviders();
        NetworkRegistry.INSTANCE.registerGuiHandler( ComputerCraft.instance, Containers.INSTANCE );

        Fixes.register( FMLCommonHandler.instance().getDataFixer() );
        if( Loader.isModLoaded( ModCharset.MODID ) ) IntegrationCharset.register();
    }

    public static void initServer( MinecraftServer server )
    {
        CommandHandler handler = (CommandHandler) server.getCommandManager();
        handler.registerCommand( new CommandComputerCraft() );
    }

    private static void registerProviders()
    {
        // Register peripheral providers
        ComputerCraftAPI.registerPeripheralProvider( ( world, pos, side ) -> {
            TileEntity tile = world.getTileEntity( pos );
            return tile instanceof IPeripheralTile ? ((IPeripheralTile) tile).getPeripheral( side ) : null;
        } );

        ComputerCraftAPI.registerPeripheralProvider( ( world, pos, side ) -> {
            TileEntity tile = world.getTileEntity( pos );
            return ComputerCraft.enableCommandBlock && tile instanceof TileEntityCommandBlock ? new CommandBlockPeripheral( (TileEntityCommandBlock) tile ) : null;
        } );

        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider( new DefaultBundledRedstoneProvider() );

        // Register media providers
        ComputerCraftAPI.registerMediaProvider( stack -> {
            Item item = stack.getItem();
            if( item instanceof IMedia ) return (IMedia) item;
            if( item instanceof ItemRecord ) return RecordMedia.INSTANCE;
            return null;
        } );

        // Register network providers
        CapabilityWiredElement.register();
    }

    @Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
    public static final class ForgeHandlers
    {
        private ForgeHandlers()
        {
        }

        @SubscribeEvent
        public static void onConnectionOpened( FMLNetworkEvent.ClientConnectedToServerEvent event )
        {
            ComputerCraft.clientComputerRegistry.reset();
        }

        @SubscribeEvent
        public static void onConnectionClosed( FMLNetworkEvent.ClientDisconnectionFromServerEvent event )
        {
            ComputerCraft.clientComputerRegistry.reset();
        }

        @SubscribeEvent
        public static void onClientTick( TickEvent.ClientTickEvent event )
        {
            if( event.phase == TickEvent.Phase.START )
            {
                ComputerCraft.clientComputerRegistry.update();
            }
        }

        @SubscribeEvent
        public static void onServerTick( TickEvent.ServerTickEvent event )
        {
            if( event.phase == TickEvent.Phase.START )
            {
                MainThread.executePendingTasks();
                ComputerCraft.serverComputerRegistry.update();
            }
        }

        @SubscribeEvent
        public static void onConfigChanged( ConfigChangedEvent.OnConfigChangedEvent event )
        {
            if( event.getModID().equals( ComputerCraft.MOD_ID ) ) Config.sync();
        }

        @SubscribeEvent
        public static void onContainerOpen( PlayerContainerEvent.Open event )
        {
            // If we're opening a computer container then broadcast the terminal state
            Container container = event.getContainer();
            if( container instanceof IContainerComputer )
            {
                IComputer computer = ((IContainerComputer) container).getComputer();
                if( computer instanceof ServerComputer )
                {
                    ((ServerComputer) computer).sendTerminalState( event.getEntityPlayer() );
                }
            }
        }
    }
}
