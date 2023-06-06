// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.detail.FabricDetailRegistries;
import dan200.computercraft.api.node.wired.WiredElementLookup;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dan200.computercraft.shared.command.CommandComputerCraft;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.details.FluidDetails;
import dan200.computercraft.shared.network.client.UpgradesLoadedMessage;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheral;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlockEntity;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemFullBlockEntity;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemBlockEntity;
import dan200.computercraft.shared.platform.FabricConfigFile;
import dan200.computercraft.shared.platform.NetworkHandler;
import dan200.computercraft.shared.platform.PlatformHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.LevelResource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ComputerCraft {
    private static final LevelResource SERVERCONFIG = new LevelResource("serverconfig");

    public static void init() {
        NetworkHandler.init();
        ModRegistry.register();
        ModRegistry.registerMainThread();
        ModRegistry.registerCreativeTab(FabricItemGroup.builder(new ResourceLocation(ComputerCraftAPI.MOD_ID, "tab"))).build();

        // Register peripherals
        PeripheralLookup.get().registerForBlockEntity((b, d) -> b.peripheral(), ModRegistry.BlockEntities.COMPUTER_NORMAL.get());
        PeripheralLookup.get().registerForBlockEntity((b, d) -> b.peripheral(), ModRegistry.BlockEntities.COMPUTER_ADVANCED.get());
        PeripheralLookup.get().registerForBlockEntity((b, d) -> b.peripheral(), ModRegistry.BlockEntities.TURTLE_NORMAL.get());
        PeripheralLookup.get().registerForBlockEntity((b, d) -> b.peripheral(), ModRegistry.BlockEntities.TURTLE_ADVANCED.get());
        PeripheralLookup.get().registerForBlockEntity((b, d) -> b.peripheral(), ModRegistry.BlockEntities.SPEAKER.get());
        PeripheralLookup.get().registerForBlockEntity((b, d) -> b.peripheral(), ModRegistry.BlockEntities.PRINTER.get());
        PeripheralLookup.get().registerForBlockEntity((b, d) -> b.peripheral(), ModRegistry.BlockEntities.DISK_DRIVE.get());
        PeripheralLookup.get().registerForBlockEntity((b, d) -> b.peripheral(), ModRegistry.BlockEntities.MONITOR_NORMAL.get());
        PeripheralLookup.get().registerForBlockEntity((b, d) -> b.peripheral(), ModRegistry.BlockEntities.MONITOR_ADVANCED.get());
        PeripheralLookup.get().registerForBlockEntity(
            (b, d) -> Config.enableCommandBlock ? new CommandBlockPeripheral(b) : null,
            BlockEntityType.COMMAND_BLOCK
        );
        PeripheralLookup.get().registerForBlockEntity(WirelessModemBlockEntity::getPeripheral, ModRegistry.BlockEntities.WIRELESS_MODEM_NORMAL.get());
        PeripheralLookup.get().registerForBlockEntity(WirelessModemBlockEntity::getPeripheral, ModRegistry.BlockEntities.WIRELESS_MODEM_ADVANCED.get());
        PeripheralLookup.get().registerForBlockEntity(WiredModemFullBlockEntity::getPeripheral, ModRegistry.BlockEntities.WIRED_MODEM_FULL.get());
        PeripheralLookup.get().registerForBlockEntity(CableBlockEntity::getPeripheral, ModRegistry.BlockEntities.CABLE.get());

        WiredElementLookup.get().registerForBlockEntity((b, d) -> b.getElement(), ModRegistry.BlockEntities.WIRED_MODEM_FULL.get());
        WiredElementLookup.get().registerForBlockEntity(CableBlockEntity::getWiredElement, ModRegistry.BlockEntities.CABLE.get());

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
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> PlatformHelper.get().sendToPlayer(new UpgradesLoadedMessage(), player));

        ServerTickEvents.START_SERVER_TICK.register(CommonHooks::onServerTickStart);
        ServerTickEvents.START_SERVER_TICK.register(s -> CommonHooks.onServerTickEnd());

        PlayerBlockBreakEvents.BEFORE.register(FabricCommonHooks::onBlockDestroy);
        UseBlockCallback.EVENT.register(FabricCommonHooks::useOnBlock);

        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            var pool = CommonHooks.getExtraLootPool(id);
            if (pool != null) tableBuilder.withPool(pool);
        });

        CommonHooks.onDatapackReload((name, listener) -> ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new ReloadListener(name, listener)));

        ((FabricConfigFile) ConfigSpec.clientSpec).load(FabricLoader.getInstance().getConfigDir().resolve(ComputerCraftAPI.MOD_ID + "-client.toml"));

        FabricDetailRegistries.FLUID_VARIANT.addProvider(FluidDetails::fill);

        ComputerCraftAPI.registerGenericSource(new InventoryMethods());
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
}
