/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.proxy;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.command.arguments.ArgumentSerializers;
import dan200.computercraft.shared.common.DefaultBundledRedstoneProvider;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.ConstantLootConditionSerializer;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.media.items.RecordMedia;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.util.NullStorage;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.ConstantRange;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTables;
import net.minecraft.world.storage.loot.TableLootEntry;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class ComputerCraftProxyCommon
{
    @SubscribeEvent
    @SuppressWarnings( "deprecation" )
    public static void init( FMLCommonSetupEvent event )
    {
        NetworkHandler.setup();

        net.minecraftforge.fml.DeferredWorkQueue.runLater( () -> {
            registerProviders();
            ArgumentSerializers.register();
            registerLoot();
        } );
    }

    public static void registerLoot()
    {
        LootConditionManager.registerCondition( ConstantLootConditionSerializer.of(
            new ResourceLocation( ComputerCraft.MOD_ID, "block_named" ),
            BlockNamedEntityLootCondition.class,
            BlockNamedEntityLootCondition.INSTANCE
        ) );

        LootConditionManager.registerCondition( ConstantLootConditionSerializer.of(
            new ResourceLocation( ComputerCraft.MOD_ID, "player_creative" ),
            PlayerCreativeLootCondition.class,
            PlayerCreativeLootCondition.INSTANCE
        ) );

        LootConditionManager.registerCondition( ConstantLootConditionSerializer.of(
            new ResourceLocation( ComputerCraft.MOD_ID, "has_id" ),
            HasComputerIdLootCondition.class,
            HasComputerIdLootCondition.INSTANCE
        ) );
    }

    private static void registerProviders()
    {
        // Register bundled power providers
        ComputerCraftAPI.registerBundledRedstoneProvider( new DefaultBundledRedstoneProvider() );

        // Register media providers
        ComputerCraftAPI.registerMediaProvider( stack -> {
            Item item = stack.getItem();
            if( item instanceof IMedia ) return (IMedia) item;
            if( item instanceof MusicDiscItem ) return RecordMedia.INSTANCE;
            return null;
        } );

        // Register capabilities
        CapabilityManager.INSTANCE.register( IWiredElement.class, new NullStorage<>(), () -> null );
        CapabilityManager.INSTANCE.register( IPeripheral.class, new NullStorage<>(), () -> null );
    }

    @Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
    public static final class ForgeHandlers
    {
        private ForgeHandlers()
        {
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
        */

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
        public static void onContainerOpen( PlayerContainerEvent.Open event )
        {
            // If we're opening a computer container then broadcast the terminal state
            Container container = event.getContainer();
            if( container instanceof IContainerComputer )
            {
                IComputer computer = ((IContainerComputer) container).getComputer();
                if( computer instanceof ServerComputer )
                {
                    ((ServerComputer) computer).sendTerminalState( event.getPlayer() );
                }
            }
        }

        @SubscribeEvent
        public static void onServerStarting( FMLServerStartingEvent event )
        {
            CommandComputerCraft.register( event.getCommandDispatcher() );
        }

        @SubscribeEvent
        public static void onServerStarted( FMLServerStartedEvent event )
        {
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            Tracking.reset();
        }

        @SubscribeEvent
        public static void onServerStopped( FMLServerStoppedEvent event )
        {
            ComputerCraft.serverComputerRegistry.reset();
            WirelessNetwork.resetNetworks();
            Tracking.reset();
        }

        public static final ResourceLocation LOOT_TREASURE_DISK = new ResourceLocation( ComputerCraft.MOD_ID, "treasure_disk" );

        private static final Set<ResourceLocation> TABLES = new HashSet<>( Arrays.asList(
            LootTables.CHESTS_SIMPLE_DUNGEON,
            LootTables.CHESTS_ABANDONED_MINESHAFT,
            LootTables.CHESTS_STRONGHOLD_CORRIDOR,
            LootTables.CHESTS_STRONGHOLD_CROSSING,
            LootTables.CHESTS_STRONGHOLD_LIBRARY,
            LootTables.CHESTS_DESERT_PYRAMID,
            LootTables.CHESTS_JUNGLE_TEMPLE,
            LootTables.CHESTS_IGLOO_CHEST,
            LootTables.CHESTS_WOODLAND_MANSION,
            LootTables.CHESTS_VILLAGE_VILLAGE_CARTOGRAPHER
        ) );

        @SubscribeEvent
        public static void lootLoad( LootTableLoadEvent event )
        {
            ResourceLocation name = event.getName();
            if( !name.getNamespace().equals( "minecraft" ) || !TABLES.contains( name ) ) return;

            event.getTable().addPool( LootPool.builder()
                .addEntry( TableLootEntry.builder( LOOT_TREASURE_DISK ) )
                .rolls( ConstantRange.of( 1 ) )
                .name( "computercraft_treasure" )
                .build() );
        }
    }
}
