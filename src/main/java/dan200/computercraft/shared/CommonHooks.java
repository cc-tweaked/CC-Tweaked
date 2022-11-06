/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.computer.core.ResourceMount;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.computer.metrics.ComputerMBean;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.client.UpgradesLoadedMessage;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.*;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Miscellaneous hooks which are present on the client and server.
 * <p>
 * These should possibly be refactored into separate classes at some point, but are fine here for now.
 *
 * @see dan200.computercraft.client.ClientHooks For client-specific ones.
 */
@Mod.EventBusSubscriber(modid = ComputerCraft.MOD_ID)
public final class CommonHooks {
    private CommonHooks() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ServerContext.get(ServerLifecycleHooks.getCurrentServer()).tick();
        }
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent event) {
        CommandComputerCraft.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        var server = event.getServer();
        if (server instanceof DedicatedServer dediServer && dediServer.getProperties().enableJmxMonitoring) {
            ComputerMBean.register();
        }

        resetState();
        ServerContext.create(server);
        ComputerMBean.start(server);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        resetState();
    }

    private static void resetState() {
        ServerContext.close();
        WirelessNetwork.resetNetworks();
        NetworkUtils.reset();
    }

    public static final ResourceLocation LOOT_TREASURE_DISK = new ResourceLocation(ComputerCraft.MOD_ID, "treasure_disk");

    private static final Set<ResourceLocation> TABLES = new HashSet<>(Arrays.asList(
        BuiltInLootTables.SIMPLE_DUNGEON,
        BuiltInLootTables.ABANDONED_MINESHAFT,
        BuiltInLootTables.STRONGHOLD_CORRIDOR,
        BuiltInLootTables.STRONGHOLD_CROSSING,
        BuiltInLootTables.STRONGHOLD_LIBRARY,
        BuiltInLootTables.DESERT_PYRAMID,
        BuiltInLootTables.JUNGLE_TEMPLE,
        BuiltInLootTables.IGLOO_CHEST,
        BuiltInLootTables.WOODLAND_MANSION,
        BuiltInLootTables.VILLAGE_CARTOGRAPHER
    ));

    @SubscribeEvent
    public static void lootLoad(LootTableLoadEvent event) {
        var name = event.getName();
        if (!name.getNamespace().equals("minecraft") || !TABLES.contains(name)) return;

        event.getTable().addPool(LootPool.lootPool()
            .add(LootTableReference.lootTableReference(LOOT_TREASURE_DISK))
            .setRolls(ConstantValue.exactly(1))
            .name("computercraft_treasure")
            .build());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ResourceMount.RELOAD_LISTENER);
        event.addListener(TurtleUpgrades.instance());
        event.addListener(PocketUpgrades.instance());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        var packet = new UpgradesLoadedMessage();
        if (event.getPlayer() == null) {
            NetworkHandler.sendToAllPlayers(packet);
        } else {
            NetworkHandler.sendToPlayer(event.getPlayer(), packet);
        }
    }
}
