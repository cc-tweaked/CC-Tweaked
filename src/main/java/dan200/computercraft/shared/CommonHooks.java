/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.core.computer.MainThread;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.IContainerComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.ConstantRange;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTables;
import net.minecraft.world.storage.loot.TableLootEntry;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
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
        NetworkUtils.reset();
    }

    @SubscribeEvent
    public static void onServerStopped( FMLServerStoppedEvent event )
    {
        ComputerCraft.serverComputerRegistry.reset();
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
}
