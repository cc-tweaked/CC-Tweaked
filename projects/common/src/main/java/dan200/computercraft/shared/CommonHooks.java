// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.core.apis.http.NetworkUtils;
import dan200.computercraft.shared.computer.core.ResourceMount;
import dan200.computercraft.shared.computer.core.ServerContext;
import dan200.computercraft.shared.computer.metrics.ComputerMBean;
import dan200.computercraft.shared.lectern.CustomLecternBlock;
import dan200.computercraft.shared.peripheral.monitor.MonitorWatcher;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Event listeners for server/common code.
 * <p>
 * All event handlers should be defined in this class, and then invoked from a loader-specific event handler. This means
 * it's much easier to ensure that each hook is called in all loader source sets.
 */
public final class CommonHooks {
    private static @Nullable MinecraftServer server;

    private CommonHooks() {
    }

    /**
     * Get the currently running server.
     *
     * @return The currently running server.
     * @deprecated The usage of this is <em>strongly</em> discouraged, and the current server should be passed around
     * directly.
     */
    @Deprecated
    public static @Nullable MinecraftServer getServer() {
        return server;
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

    public static void onServerStarted(MinecraftServer server) {
        CommonHooks.server = server;
        // ItemDetails requires creative tabs to be populated, however by default this is done lazily on the client and
        // not at all on the server! We instead do this once on server startup.
        CreativeModeTabs.tryRebuildTabContents(server.getWorldData().enabledFeatures(), false, server.registryAccess());
    }

    public static void onServerStopped() {
        resetState();
    }

    private static void resetState() {
        server = null;
        ServerContext.close();
        NetworkUtils.reset();
    }

    public static void onServerChunkUnload(LevelChunk chunk) {
        if (!(chunk.getLevel() instanceof ServerLevel)) throw new IllegalArgumentException("Not a server chunk.");
        TickScheduler.onChunkUnload(chunk);
    }

    public static void onChunkWatch(LevelChunk chunk, ServerPlayer player) {
        MonitorWatcher.onWatch(chunk, player);
    }

    public static void onChunkTicketLevelChanged(ServerLevel level, long chunkPos, int oldLevel, int newLevel) {
        TickScheduler.onChunkTicketChanged(level, chunkPos, oldLevel, newLevel);
    }

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isSpectator()) return InteractionResult.PASS;

        var pos = hitResult.getBlockPos();
        var heldItem = player.getItemInHand(hand);
        var blockState = level.getBlockState(pos);

        if (blockState.is(Blocks.LECTERN) && !blockState.getValue(LecternBlock.HAS_BOOK)) {
            return CustomLecternBlock.tryPlaceItem(player, level, pos, blockState, heldItem);
        }

        return InteractionResult.PASS;
    }

    public static final ResourceKey<LootTable> TREASURE_DISK_LOOT = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "treasure_disk"));

    private static final Set<ResourceKey<LootTable>> TREASURE_DISK_LOOT_TABLES = Set.of(
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
    );

    public static LootPool.@Nullable Builder getExtraLootPool(ResourceKey<LootTable> lootTable) {
        if (!TREASURE_DISK_LOOT_TABLES.contains(lootTable)) {
            return null;
        }

        return LootPool.lootPool()
            .add(NestedLootTable.lootTableReference(TREASURE_DISK_LOOT))
            .setRolls(ConstantValue.exactly(1));
    }

    public static void onDatapackReload(BiConsumer<String, PreparableReloadListener> addReload) {
        addReload.accept("mounts", ResourceMount.RELOAD_LISTENER);
    }

    public static boolean onEntitySpawn(Entity entity) {
        return DropConsumer.onEntitySpawn(entity);
    }

    public static boolean onLivingDrop(Entity entity, ItemStack stack) {
        return DropConsumer.onLivingDrop(entity, stack);
    }

    /**
     * Add items to an existing creative tab.
     *
     * @param key     The {@link ResourceKey} for this creative tab.
     * @param context Additional parameters used for building the contents.
     * @param out     The creative tab output to append items to.
     */
    public static void onBuildCreativeTab(ResourceKey<CreativeModeTab> key, CreativeModeTab.ItemDisplayParameters context, CreativeModeTab.Output out) {
        if (key == CreativeModeTabs.OP_BLOCKS && context.hasPermissions()) {
            out.accept(ModRegistry.Items.COMPUTER_COMMAND.get());
        }
    }

    public static void onItemTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flags, List<Component> out) {
        var appender = new TooltipAppender(out);
        addToTooltip(stack, ModRegistry.DataComponents.PRINTOUT.get(), context, appender, flags);
        addToTooltip(stack, ModRegistry.DataComponents.TREASURE_DISK.get(), context, appender, flags);

        // Disk and computer IDs require some conditional logic, so we don't bother using TooltipProvider.

        var diskId = stack.get(ModRegistry.DataComponents.DISK_ID.get());
        if (diskId != null && flags.isAdvanced()) diskId.addToTooltip("gui.computercraft.tooltip.disk_id", appender);

        var computerId = stack.get(ModRegistry.DataComponents.COMPUTER_ID.get());
        if (computerId != null && (flags.isAdvanced() || !stack.has(DataComponents.CUSTOM_NAME))) {
            computerId.addToTooltip("gui.computercraft.tooltip.computer_id", appender);
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

    private static <T extends TooltipProvider> void addToTooltip(ItemStack stack, DataComponentType<T> component, Item.TooltipContext context, Consumer<Component> out, TooltipFlag flags) {
        var provider = stack.get(component);
        if (provider != null) provider.addToTooltip(context, out, flags);
    }
}
