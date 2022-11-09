/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.shared.computer.core.ResourceMount;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.computer.metrics.ComputerMBean;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessNetwork;
import dan200.computercraft.shared.peripheral.monitor.MonitorWatcher;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Event listeners for server/common code.
 * <p>
 * All event handlers should be defined in this class, and then invoked from a loader-specific event handler. This means
 * it's much easier to ensure that each hook is called in all loader source sets.
 */
public final class CommonHooks {
    private CommonHooks() {
    }

    public static void onServerTickStart(MinecraftServer server) {
        ServerContext.get(server).tick();
        TickScheduler.tick();
    }

    public static void onServerTickEnd() {
        MonitorWatcher.onTick();
    }

    public static void onServerStarting(MinecraftServer server) {
        if (server instanceof DedicatedServer dediServer && dediServer.getProperties().enableJmxMonitoring) {
            ComputerMBean.register();
        }

        resetState();
        ServerContext.create(server);
        ComputerMBean.start(server);
    }

    public static void onServerStopped() {
        resetState();
    }

    private static void resetState() {
        ServerContext.close();
        WirelessNetwork.resetNetworks();
        NetworkUtils.reset();
    }

    public static void onChunkWatch(LevelChunk chunk, ServerPlayer player) {
        MonitorWatcher.onWatch(chunk, player);
    }

    public static final ResourceLocation LOOT_TREASURE_DISK = new ResourceLocation(ComputerCraftAPI.MOD_ID, "treasure_disk");

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


    public static @Nullable LootPool.Builder getExtraLootPool(ResourceLocation lootTable) {
        if (!lootTable.getNamespace().equals("minecraft") || !TABLES.contains(lootTable)) return null;

        return LootPool.lootPool()
            .add(LootTableReference.lootTableReference(LOOT_TREASURE_DISK))
            .setRolls(ConstantValue.exactly(1));
    }

    public static void onDatapackReload(BiConsumer<String, PreparableReloadListener> addReload) {
        addReload.accept("mounts", ResourceMount.RELOAD_LISTENER);
        addReload.accept("turtle_upgrades", TurtleUpgrades.instance());
        addReload.accept("pocket_upgrades", PocketUpgrades.instance());
    }

    public static boolean onEntitySpawn(Entity entity) {
        return DropConsumer.onEntitySpawn(entity);
    }

    public static boolean onLivingDrop(Entity entity, ItemStack stack) {
        return DropConsumer.onLivingDrop(entity, stack);
    }
}
