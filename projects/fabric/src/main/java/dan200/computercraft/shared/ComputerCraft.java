// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.FabricDetailRegistries;
import dan200.computercraft.api.media.MediaLookup;
import dan200.computercraft.api.node.wired.WiredElementLookup;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dan200.computercraft.impl.Peripherals;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.details.FluidDetails;
import dan200.computercraft.shared.integration.CreateIntegration;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.client.UpgradesLoadedMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.platform.FabricConfigFile;
import dan200.computercraft.shared.platform.FabricMessageType;
import dan200.computercraft.shared.turtle.upgrades.TurtleStorage;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

public class ComputerCraft {
    private static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");

    public static void init() {
        for (var type : NetworkMessages.getServerbound()) {
            ServerPlayNetworking.registerGlobalReceiver(
                FabricMessageType.toFabricType(type), (packet, player, sender) -> packet.payload().handle(() -> player)
            );
        }

        ModRegistry.register();
        ModRegistry.registerMainThread();

        ModRegistry.registerPeripherals(new BlockComponentImpl<>(PeripheralLookup.get()));
        ModRegistry.registerWiredElements(new BlockComponentImpl<>(WiredElementLookup.get()));
        ModRegistry.registerMedia(new ItemComponentImpl<>(MediaLookup.get()));

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> CommandComputerCraft.register(dispatcher));

        // Register hooks
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ((FabricConfigFile) ConfigSpec.serverSpec).load(server.getWorldPath(SERVERCONFIG).resolve(ComputerCraftAPI.MOD_ID + "-server.toml"));
            CommonHooks.onServerStarting(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            CommonHooks.onServerStopped();
            ((FabricConfigFile) ConfigSpec.serverSpec).unload();
        });
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> ServerNetworking.sendToPlayer(new UpgradesLoadedMessage(), player));

        ServerTickEvents.START_SERVER_TICK.register(CommonHooks::onServerTickStart);
        ServerTickEvents.START_SERVER_TICK.register(s -> CommonHooks.onServerTickEnd());
        ServerChunkEvents.CHUNK_UNLOAD.register((l, c) -> CommonHooks.onServerChunkUnload(c));

        PlayerBlockBreakEvents.BEFORE.register(FabricCommonHooks::onBlockDestroy);

        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            var pool = CommonHooks.getExtraLootPool(id);
            if (pool != null) tableBuilder.withPool(pool);
        });

        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((tab, entries) -> CommonHooks.onBuildCreativeTab(
            BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).orElseThrow(),
            entries.getContext(), entries
        ));

        CommonHooks.onDatapackReload((name, listener) -> ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new ReloadListener(name, listener)));

        FabricDetailRegistries.FLUID_VARIANT.addProvider(FluidDetails::fill);

        ComputerCraftAPI.registerGenericSource(new InventoryMethods());

        Peripherals.addGenericLookup((world, pos, state, blockEntity, side, invalidate) -> InventoryMethods.extractContainer(world, pos, state, blockEntity, side));

        if (FabricLoader.getInstance().isModLoaded(CreateIntegration.ID)) CreateIntegration.setup();

        ResourceManagerHelper.registerBuiltinResourcePack(
            new ResourceLocation(ComputerCraftAPI.MOD_ID, TurtleStorage.DATA_PACK_NAME),
            FabricLoader.getInstance().getModContainer(ComputerCraftAPI.MOD_ID).orElseThrow(),
            Component.translatable(TurtleStorage.DATA_PACK_TRANSLATION),
            ResourcePackActivationType.NORMAL
        );
    }

    private record ReloadListener(String name, PreparableReloadListener listener)
        implements IdentifiableResourceReloadListener {

        @Override
        public ResourceLocation getFabricId() {
            return new ResourceLocation(ComputerCraftAPI.MOD_ID, name);
        }

        @Override
        public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
            return listener.reload(preparationBarrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
        }
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
