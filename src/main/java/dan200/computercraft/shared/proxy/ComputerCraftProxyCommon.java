/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.command.arguments.ArgumentSerializers;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.media.items.RecordMedia;
import dan200.computercraft.shared.network.Containers;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheral;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.turtle.FurnaceRefuelHandler;
import dan200.computercraft.shared.util.TickScheduler;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.MutableRegistry;

import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.fabricmc.fabric.api.registry.CommandRegistry;

public class ComputerCraftProxyCommon {
    private static MinecraftServer server;

    public static void setup() {
        NetworkHandler.setup();

        Registry.registerBlocks(net.minecraft.util.registry.Registry.BLOCK);
        Registry.registerTileEntities((MutableRegistry<BlockEntityType<?>>) net.minecraft.util.registry.Registry.BLOCK_ENTITY_TYPE);
        Registry.registerItems(net.minecraft.util.registry.Registry.ITEM);
        Registry.registerRecipes((MutableRegistry<RecipeSerializer<?>>) net.minecraft.util.registry.Registry.RECIPE_SERIALIZER);

        Containers.setup();

        registerProviders();
        registerHandlers();

        ArgumentSerializers.register();
    }

    private static void registerProviders() {
        // Register peripheral providers
        ComputerCraftAPI.registerPeripheralProvider((world, pos, side) -> {
            BlockEntity tile = world.getBlockEntity(pos);
            return tile instanceof IPeripheralTile ? ((IPeripheralTile) tile).getPeripheral(side) : null;
        });

        ComputerCraftAPI.registerPeripheralProvider((world, pos, side) -> {
            BlockEntity tile = world.getBlockEntity(pos);
            return ComputerCraft.enableCommandBlock && tile instanceof CommandBlockBlockEntity ?
                   new CommandBlockPeripheral((CommandBlockBlockEntity) tile) : null;
        });

        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider(new DefaultBundledRedstoneProvider());

        // Register media providers
        ComputerCraftAPI.registerMediaProvider(stack -> {
            Item item = stack.getItem();
            if (item instanceof IMedia) {
                return (IMedia) item;
            }
            if (item instanceof MusicDiscItem) {
                return RecordMedia.INSTANCE;
            }
            return null;
        });
    }

    private static void registerHandlers() {
        CommandRegistry.INSTANCE.register(false, CommandComputerCraft::register);

        ServerTickCallback.EVENT.register(server -> {
            MainThread.executePendingTasks();
            ComputerCraft.serverComputerRegistry.update();
            TickScheduler.tick();
        });

        ServerStartCallback.EVENT.register(server -> {
            ComputerCraftProxyCommon.server = server;
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            MainThread.reset();
            Tracking.reset();
        });

        ServerStopCallback.EVENT.register(server -> {
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            MainThread.reset();
            Tracking.reset();
            ComputerCraftProxyCommon.server = null;
        });

        TurtleEvent.EVENT_BUS.register(FurnaceRefuelHandler.INSTANCE);
        TurtleEvent.EVENT_BUS.register(new TurtlePermissions());
    }

    public static MinecraftServer getServer() {
        // Sorry asie
        return server;
    }

    public static final class ForgeHandlers {
        private ForgeHandlers() {
        }

        /*
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
        */
    }
}
