/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.command.arguments.ArgumentSerializers;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.media.items.RecordMedia;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheral;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.turtle.FurnaceRefuelHandler;
import dan200.computercraft.shared.turtle.SignInspectHandler;
import dan200.computercraft.shared.util.Config;
import dan200.computercraft.shared.util.TickScheduler;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class ComputerCraftProxyCommon
{
    private static MinecraftServer server;

    public static void init()
    {
        NetworkHandler.setup();

        registerProviders();
        registerHandlers();

        ArgumentSerializers.register();

        ComputerCraftAPI.registerGenericSource( new InventoryMethods() );
    }

    private static void registerProviders()
    {
        ComputerCraftAPI.registerPeripheralProvider( ( world, pos, side ) -> {
            BlockEntity tile = world.getBlockEntity( pos );
            return tile instanceof IPeripheralTile ? ((IPeripheralTile) tile).getPeripheral( side ) : null;
        } );

        ComputerCraftAPI.registerPeripheralProvider( ( world, pos, side ) -> {
            BlockEntity tile = world.getBlockEntity( pos );
            return ComputerCraft.enableCommandBlock && tile instanceof CommandBlockBlockEntity ?
                new CommandBlockPeripheral( (CommandBlockBlockEntity) tile ) : null;
        } );

        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider( new DefaultBundledRedstoneProvider() );

        // Register media providers
        ComputerCraftAPI.registerMediaProvider( stack -> {
            Item item = stack.getItem();
            if( item instanceof IMedia )
            {
                return (IMedia) item;
            }
            if( item instanceof MusicDiscItem )
            {
                return RecordMedia.INSTANCE;
            }
            return null;
        } );
    }

    private static void registerHandlers()
    {
        CommandRegistrationCallback.EVENT.register( CommandComputerCraft::register );

        ServerTickEvents.START_SERVER_TICK.register( server -> {
            MainThread.executePendingTasks();
            ComputerCraft.serverComputerRegistry.update();
            TickScheduler.tick();
        } );

        ServerLifecycleEvents.SERVER_STARTED.register( server -> {
            ComputerCraftProxyCommon.server = server;
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            MainThread.reset();
            Tracking.reset();
        } );

        ServerLifecycleEvents.SERVER_STOPPING.register( server -> {
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            MainThread.reset();
            Tracking.reset();
            ComputerCraftProxyCommon.server = null;
        } );

        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register( ( blockEntity, world ) -> {
            if( blockEntity instanceof TileGeneric )
            {
                ((TileGeneric) blockEntity).onChunkUnloaded();
            }
        } );

        // Config
        ServerLifecycleEvents.SERVER_STARTING.register( Config::serverStarting );
        ServerLifecycleEvents.SERVER_STOPPING.register( Config::serverStopping );

        TurtleEvent.EVENT_BUS.register( FurnaceRefuelHandler.INSTANCE );
        TurtleEvent.EVENT_BUS.register( new TurtlePermissions() );
        TurtleEvent.EVENT_BUS.register( new SignInspectHandler() );
    }

    public static void registerLoot()
    {
        registerCondition( "block_named", BlockNamedEntityLootCondition.TYPE );
        registerCondition( "player_creative", PlayerCreativeLootCondition.TYPE );
        registerCondition( "has_id", HasComputerIdLootCondition.TYPE );
    }

    private static void registerCondition( String name, LootConditionType serializer )
    {
        Registry.register( Registry.LOOT_CONDITION_TYPE, new Identifier( ComputerCraft.MOD_ID, name ), serializer );
    }
}
