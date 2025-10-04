// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.command.CommandComputerCraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Forge-specific dispatch for {@link CommonHooks}.
 */
@EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID)
public class ForgeCommonHooks {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        CommonHooks.onServerTickStart(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        CommonHooks.onServerTickEnd();
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
        if (event.getLevel() instanceof ServerLevel) CommonHooks.onServerChunkUnload(event.getChunk());
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
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        var result = CommonHooks.onUseBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (result == InteractionResult.PASS) return;

        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        CommonHooks.onDatapackReload(event::addListener);
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

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        var appender = new TooltipAppender(event.getToolTip());

        var display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        for (var component : ModRegistry.DataComponents.TOOLTIP_COMPONENTS) {
            stack.addToTooltip(component.get(), event.getContext(), display, appender, event.getFlags());
        }
    }

    /**
     * Inserts additional tooltip items directly after the custom name, rather than at the very end.
     */
    private static final class TooltipAppender implements Consumer<Component> {
        private final List<Component> out;
        private int index = 1;

        private TooltipAppender(List<Component> out) {
            this.out = out;
        }

        @Override
        public void accept(Component component) {
            out.add(index++, component);
        }
    }
}
