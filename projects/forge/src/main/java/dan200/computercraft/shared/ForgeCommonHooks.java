// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.network.client.UpgradesLoadedMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Forge-specific dispatch for {@link CommonHooks}.
 */
@EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID)
public class ForgeCommonHooks {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        switch (event.phase) {
            case START -> CommonHooks.onServerTickStart(event.getServer());
            case END -> CommonHooks.onServerTickEnd();
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        CommonHooks.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CommonHooks.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CommonHooks.onServerStopped();
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent event) {
        CommandComputerCraft.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel && event.getChunk() instanceof LevelChunk chunk) {
            CommonHooks.onServerChunkUnload(chunk);
        }
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Sent event) {
        CommonHooks.onChunkWatch(event.getChunk(), event.getPlayer());
    }

    @SubscribeEvent
    public static void onChunkTicketLevelChanged(ChunkTicketLevelUpdatedEvent event) {
        CommonHooks.onChunkTicketLevelChanged(event.getLevel(), event.getChunkPos(), event.getOldTicketLevel(), event.getNewTicketLevel());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        CommonHooks.onDatapackReload((id, listener) -> event.addListener(listener));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        var packet = new UpgradesLoadedMessage();
        if (event.getPlayer() == null) {
            ServerNetworking.sendToAllPlayers(packet, event.getPlayerList().getServer());
        } else {
            ServerNetworking.sendToPlayer(packet, event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void lootLoad(LootTableLoadEvent event) {
        var pool = CommonHooks.getExtraLootPool(ResourceKey.create(Registries.LOOT_TABLE, event.getName()));
        if (pool != null) event.getTable().addPool(pool.build());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (CommonHooks.onEntitySpawn(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(itemEntity -> CommonHooks.onLivingDrop(event.getEntity(), itemEntity.getItem()));
    }
}
