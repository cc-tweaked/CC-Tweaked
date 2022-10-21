/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.filesystem.ResourceMount;
import dan200.computercraft.core.tracking.ComputerMBean;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.computer.core.ServerComputerRegistry;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.ConstantRange;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.TableLootEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Miscellaneous hooks which are present on the client and server.
 *
 * These should possibly be refactored into separate classes at some point, but are fine here for now.
 *
 * @see dan200.computercraft.client.ClientHooks For client-specific ones.
 */
@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class CommonHooks
{
    private CommonHooks()
    {
    }

    @SubscribeEvent
    public static void onServerTick( TickEvent.ServerTickEvent event )
    {
        if( event.phase == TickEvent.Phase.START )
        {
            MainThread.executePendingTasks();
            ServerComputerRegistry.INSTANCE.update();
        }
    }

    @SubscribeEvent
    public static void onRegisterCommand( RegisterCommandsEvent event )
    {
        CommandComputerCraft.register( event.getDispatcher() );
    }

    @SubscribeEvent
    public static void onServerStarting( FMLServerStartingEvent event )
    {
        MinecraftServer server = event.getServer();
        if( server instanceof DedicatedServer && ((DedicatedServer) server).getProperties().enableJmxMonitoring )
        {
            ComputerMBean.register();
        }

        resetState();
        ComputerMBean.registerTracker();
    }

    @SubscribeEvent
    public static void onServerStopped( FMLServerStoppedEvent event )
    {
        resetState();
    }

    private static void resetState()
    {
        ServerComputerRegistry.INSTANCE.reset();
        MainThread.reset();
        WirelessNetwork.resetNetworks();
        Tracking.reset();
        NetworkUtils.reset();
    }

    public static final ResourceLocation LOOT_TREASURE_DISK = new ResourceLocation( ComputerCraft.MOD_ID, "treasure_disk" );

    private static final Set<ResourceLocation> TABLES = new HashSet<>( Arrays.asList(
        LootTables.SIMPLE_DUNGEON,
        LootTables.ABANDONED_MINESHAFT,
        LootTables.STRONGHOLD_CORRIDOR,
        LootTables.STRONGHOLD_CROSSING,
        LootTables.STRONGHOLD_LIBRARY,
        LootTables.DESERT_PYRAMID,
        LootTables.JUNGLE_TEMPLE,
        LootTables.IGLOO_CHEST,
        LootTables.WOODLAND_MANSION,
        LootTables.VILLAGE_CARTOGRAPHER
    ) );

    @SubscribeEvent
    public static void lootLoad( LootTableLoadEvent event )
    {
        ResourceLocation name = event.getName();
        if( !name.getNamespace().equals( "minecraft" ) || !TABLES.contains( name ) ) return;

        event.getTable().addPool( LootPool.lootPool()
            .add( TableLootEntry.lootTableReference( LOOT_TREASURE_DISK ) )
            .setRolls( ConstantRange.exactly( 1 ) )
            .name( "computercraft_treasure" )
            .build() );
    }

    @SubscribeEvent
    public static void onAddReloadListeners( AddReloadListenerEvent event )
    {
        event.addListener( ResourceMount.RELOAD_LISTENER );
    }

    @SubscribeEvent
    public static void onMissingEntityMappingsEvent( RegistryEvent.MissingMappings<EntityType<?>> event )
    {
        ResourceLocation id = new ResourceLocation( ComputerCraft.MOD_ID, "turtle_player" );
        for( RegistryEvent.MissingMappings.Mapping<EntityType<?>> mapping : event.getMappings( ComputerCraft.MOD_ID ) )
        {
            if( mapping.key.equals( id ) ) mapping.ignore();
        }
    }
}
