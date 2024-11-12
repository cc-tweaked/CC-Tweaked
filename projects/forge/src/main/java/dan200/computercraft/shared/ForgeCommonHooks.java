// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.computer.blocks.ComputerBlockEntity;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.integration.Optifine;
import dan200.computercraft.shared.network.client.UpgradesLoadedMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheral;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlockEntity;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlockEntity;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemFullBlockEntity;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemBlockEntity;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dan200.computercraft.shared.peripheral.printer.PrinterBlockEntity;
import dan200.computercraft.shared.peripheral.redstone.RedstoneRelayBlockEntity;
import dan200.computercraft.shared.peripheral.speaker.SpeakerBlockEntity;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import dan200.computercraft.shared.util.CapabilityProvider;
import dan200.computercraft.shared.util.SidedCapabilityProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;
import static dan200.computercraft.shared.Capabilities.CAPABILITY_WIRED_ELEMENT;
import static net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER;

/**
 * Forge-specific dispatch for {@link CommonHooks}.
 */
@Mod.EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID)
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
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Optifine.warnAboutOptifine(event.getEntity()::sendSystemMessage);
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
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
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

    private static final ResourceLocation PERIPHERAL = new ResourceLocation(ComputerCraftAPI.MOD_ID, "peripheral");
    private static final ResourceLocation WIRED_ELEMENT = new ResourceLocation(ComputerCraftAPI.MOD_ID, "wired_node");
    private static final ResourceLocation INVENTORY = new ResourceLocation(ComputerCraftAPI.MOD_ID, "inventory");

    /**
     * Attach capabilities to our block entities.
     *
     * @param event The {@link AttachCapabilitiesEvent} event.
     */
    @SubscribeEvent
    public static void onCapability(AttachCapabilitiesEvent<BlockEntity> event) {
        var blockEntity = event.getObject();
        if (blockEntity instanceof ComputerBlockEntity computer) {
            CapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, computer::peripheral);
        } else if (blockEntity instanceof TurtleBlockEntity turtle) {
            CapabilityProvider.attach(event, INVENTORY, ITEM_HANDLER, () -> new InvWrapper(turtle));

            var peripheral = CapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, turtle::peripheral);
            turtle.onMoved(peripheral::invalidate);
        } else if (blockEntity instanceof DiskDriveBlockEntity diskDrive) {
            CapabilityProvider.attach(event, INVENTORY, ITEM_HANDLER, () -> new InvWrapper(diskDrive));
            CapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, diskDrive::peripheral);
        } else if (blockEntity instanceof CableBlockEntity cable) {
            var peripheralHandler = SidedCapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, cable::getPeripheral);
            var elementHandler = SidedCapabilityProvider.attach(event, WIRED_ELEMENT, CAPABILITY_WIRED_ELEMENT, cable::getWiredElement);
            cable.onModemChanged(() -> {
                peripheralHandler.invalidate();
                elementHandler.invalidate();
            });
        } else if (blockEntity instanceof WiredModemFullBlockEntity modem) {
            SidedCapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, modem::getPeripheral);
            CapabilityProvider.attach(event, WIRED_ELEMENT, CAPABILITY_WIRED_ELEMENT, modem::getElement);
        } else if (blockEntity instanceof WirelessModemBlockEntity modem) {
            var peripheral = SidedCapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, modem::getPeripheral);
            modem.onModemChanged(peripheral::invalidate);
        } else if (blockEntity instanceof MonitorBlockEntity monitor) {
            CapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, monitor::peripheral);
        } else if (blockEntity instanceof SpeakerBlockEntity speaker) {
            CapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, speaker::peripheral);
        } else if (blockEntity instanceof PrinterBlockEntity printer) {
            CapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, printer::peripheral);
            // We don't need to invalidate here as the block's can't be rotated on the X axis!
            SidedCapabilityProvider.attach(
                event, INVENTORY, ITEM_HANDLER,
                s -> s == null ? new InvWrapper(printer) : new SidedInvWrapper(printer, s)
            );
        } else if (blockEntity instanceof RedstoneRelayBlockEntity redstone) {
            CapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, redstone::peripheral);
        } else if (Config.enableCommandBlock && blockEntity instanceof CommandBlockEntity commandBlock) {
            CapabilityProvider.attach(event, PERIPHERAL, CAPABILITY_PERIPHERAL, () -> new CommandBlockPeripheral(commandBlock));
        }
    }

    @SubscribeEvent
    public static void lootLoad(LootTableLoadEvent event) {
        var pool = CommonHooks.getExtraLootPool(event.getName());
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
