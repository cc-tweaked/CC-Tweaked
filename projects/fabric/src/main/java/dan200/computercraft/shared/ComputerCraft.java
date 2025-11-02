// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.FabricDetailRegistries;
import dan200.computercraft.api.media.MediaLookup;
import dan200.computercraft.api.network.wired.WiredElementLookup;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.impl.Peripherals;
import dan200.computercraft.impl.PocketUpgrades;
import dan200.computercraft.impl.TurtleUpgrades;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.details.FluidDetails;
import dan200.computercraft.shared.integration.CreateIntegration;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import dan200.computercraft.shared.platform.FabricConfigFile;
import dan200.computercraft.shared.recipe.function.RecipeFunction;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.item.v1.ComponentTooltipAppenderRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;

public class ComputerCraft {
    private static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");

    /**
     * The client-side command to open a folder. Ideally this would live under the main {@code computercraft}
     * namespace, but unfortunately that overrides commands, rather than merging them.
     */
    public static final String CLIENT_OPEN_FOLDER = "computercraft-open-folder";

    public static void init() {
        for (var type : NetworkMessages.getServerbound()) registerPayloadType(PayloadTypeRegistry.playC2S(), type);
        for (var type : NetworkMessages.getClientbound()) registerPayloadType(PayloadTypeRegistry.playS2C(), type);

        for (var type : NetworkMessages.getServerbound()) {
            ServerPlayNetworking.registerGlobalReceiver(type.type(), (packet, player) -> packet.handle(player::player));
        }

        FabricRegistryBuilder.createSimple(RecipeFunction.REGISTRY).attribute(RegistryAttribute.SYNCED).buildAndRegister();

        DynamicRegistries.registerSynced(ITurtleUpgrade.REGISTRY, TurtleUpgrades.instance().upgradeCodec());
        DynamicRegistries.registerSynced(IPocketUpgrade.REGISTRY, PocketUpgrades.instance().upgradeCodec());

        ModRegistry.register();
        ModRegistry.registerMainThread();

        ModRegistry.registerPeripherals(new BlockComponentImpl<>(PeripheralLookup.get()));
        ModRegistry.registerWiredElements(new BlockComponentImpl<>(WiredElementLookup.get()));
        ModRegistry.registerMedia(new ItemComponentImpl<>(MediaLookup.get()));

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> CommandComputerCraft.register(dispatcher));

        // Register tooltips
        for (var tooltip : ModRegistry.DataComponents.TOOLTIP_COMPONENTS) {
            ComponentTooltipAppenderRegistry.addAfter(DataComponents.LORE, tooltip.get());
        }

        // Register hooks
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ((FabricConfigFile) ConfigSpec.serverSpec).load(
                server.getWorldPath(SERVERCONFIG).resolve(ComputerCraftAPI.MOD_ID + "-server.toml"),
                FabricLoader.getInstance().getConfigDir().resolve(ComputerCraftAPI.MOD_ID + "-server.toml")
            );
            CommonHooks.onServerStarting(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            CommonHooks.onServerStopped();
            ((FabricConfigFile) ConfigSpec.serverSpec).unload();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(CommonHooks::onServerStarted);

        ServerTickEvents.START_SERVER_TICK.register(CommonHooks::onServerTickStart);
        ServerTickEvents.START_SERVER_TICK.register(s -> CommonHooks.onServerTickEnd());
        ServerChunkEvents.CHUNK_UNLOAD.register((l, c) -> CommonHooks.onServerChunkUnload(c));
        ServerChunkEvents.CHUNK_LEVEL_TYPE_CHANGE.register((level, chunk, oldStatus, newStatus) ->
            CommonHooks.onChunkTicketLevelChanged(level, chunk.getPos().toLong(), ChunkLevel.byStatus(oldStatus), ChunkLevel.byStatus(newStatus)));

        PlayerBlockBreakEvents.BEFORE.register(FabricCommonHooks::onBlockDestroy);
        UseBlockCallback.EVENT.register(CommonHooks::onUseBlock);

        PlayerPickItemEvents.BLOCK.register((player, pos, state, includeData) ->
            state.getBlock() instanceof CableBlock cable ? cable.getCloneItemStack(player.level(), pos, state, includeData, player) : null
        );

        LootTableEvents.MODIFY.register((id, tableBuilder, source, registries) -> {
            var pool = CommonHooks.getExtraLootPool(id);
            if (pool != null) tableBuilder.withPool(pool);
        });

        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((tab, entries) -> CommonHooks.onBuildCreativeTab(
            BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElseThrow(),
            entries.getContext(), entries
        ));

        CommonHooks.onDatapackReload(ResourceLoader.get(PackType.SERVER_DATA)::registerReloader);

        FabricDetailRegistries.FLUID_VARIANT.addProvider(FluidDetails::fill);

        ComputerCraftAPI.registerGenericSource(new InventoryMethods());

        Peripherals.addGenericLookup(InventoryMethods::extractContainer);

        if (FabricLoader.getInstance().isModLoaded(CreateIntegration.ID)) CreateIntegration.setup();
    }

    private static <B extends FriendlyByteBuf, T extends CustomPacketPayload> void registerPayloadType(PayloadTypeRegistry<B> registry, CustomPacketPayload.TypeAndCodec<B, T> type) {
        registry.register(type.type(), type.codec());
    }

    private record BlockComponentImpl<T, C extends @Nullable Object>(
        BlockApiLookup<T, C> lookup
    ) implements ModRegistry.BlockComponent<T, C> {
        @Override
        public <B extends BlockEntity> void registerForBlockEntity(BlockEntityType<B> blockEntityType, BiFunction<? super B, C, @Nullable T> provider) {
            lookup.registerForBlockEntity(provider, blockEntityType);
        }
    }

    private record ItemComponentImpl<T>(
        ItemApiLookup<T, @Nullable Void> lookup
    ) implements ModRegistry.ItemComponent<T> {
        @Override
        public void registerForItems(BiFunction<ItemStack, @Nullable Void, @Nullable T> provider, ItemLike... items) {
            lookup().registerForItems(provider::apply, items);
        }

        @Override
        public void registerFallback(BiFunction<ItemStack, @Nullable Void, @Nullable T> provider) {
            lookup().registerFallback(provider::apply);
        }
    }
}
