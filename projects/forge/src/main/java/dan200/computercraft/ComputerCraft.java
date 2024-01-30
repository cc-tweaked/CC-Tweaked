// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft;

import com.electronwill.nightconfig.core.file.FileConfig;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.detail.ForgeDetailRegistries;
import dan200.computercraft.api.network.wired.WiredElementCapability;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.impl.Services;
import dan200.computercraft.shared.CommonHooks;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.details.FluidData;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.peripheral.commandblock.CommandBlockPeripheral;
import dan200.computercraft.shared.peripheral.generic.methods.EnergyMethods;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlockEntity;
import dan200.computercraft.shared.peripheral.modem.wired.WiredModemFullBlockEntity;
import dan200.computercraft.shared.peripheral.modem.wireless.WirelessModemBlockEntity;
import dan200.computercraft.shared.platform.ForgeConfigFile;
import dan200.computercraft.shared.platform.ForgeMessageType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import javax.annotation.Nullable;

@Mod(ComputerCraftAPI.MOD_ID)
@Mod.EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ComputerCraft {
    private static @Nullable IEventBus eventBus;

    public ComputerCraft(IEventBus eventBus) {
        withEventBus(eventBus, ModRegistry::register);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ((ForgeConfigFile) ConfigSpec.serverSpec).spec());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ((ForgeConfigFile) ConfigSpec.clientSpec).spec());
    }

    private static void withEventBus(IEventBus eventBus, Runnable task) {
        ComputerCraft.eventBus = eventBus;
        task.run();
        ComputerCraft.eventBus = null;
    }

    public static IEventBus getEventBus() {
        var bus = eventBus;
        if (bus == null) throw new NullPointerException("Bus is not available.");
        return bus;
    }

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.create(new RegistryBuilder<>(ITurtleUpgrade.serialiserRegistryKey()));
        event.create(new RegistryBuilder<>(IPocketUpgrade.serialiserRegistryKey()));
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(ModRegistry::registerMainThread);

        ComputerCraftAPI.registerGenericSource(new InventoryMethods());
        ComputerCraftAPI.registerGenericSource(new FluidMethods());
        ComputerCraftAPI.registerGenericSource(new EnergyMethods());

        ForgeComputerCraftAPI.registerGenericCapability(Capabilities.ItemHandler.BLOCK);
        ForgeComputerCraftAPI.registerGenericCapability(Capabilities.FluidHandler.BLOCK);
        ForgeComputerCraftAPI.registerGenericCapability(Capabilities.EnergyStorage.BLOCK);

        ForgeDetailRegistries.FLUID_STACK.addProvider(FluidData::fill);
    }

    @SubscribeEvent
    public static void registerNetwork(RegisterPayloadHandlerEvent event) {
        var registrar = event.registrar(ComputerCraftAPI.MOD_ID).versioned(ComputerCraftAPI.getInstalledVersion());

        for (var type : NetworkMessages.getServerbound()) {
            var forgeType = ForgeMessageType.cast(type);
            registrar.play(forgeType.id(), forgeType.reader(), builder -> builder.server(
                (t, context) -> context.workHandler().execute(() -> t.payload().handle(() -> (ServerPlayer) context.player().orElseThrow()))
            ));
        }

        for (var type : NetworkMessages.getClientbound()) {
            var forgeType = ForgeMessageType.cast(type);
            registrar.play(forgeType.id(), forgeType.reader(), builder -> builder.client(
                (t, context) -> context.workHandler().execute(() -> t.payload().handle(ClientHolderHolder.get()))
            ));
        }
    }

    /**
     * Attach capabilities to our block entities.
     *
     * @param event The event to register capabilities with.
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.COMPUTER_NORMAL.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.COMPUTER_ADVANCED.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.TURTLE_NORMAL.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.TURTLE_ADVANCED.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.SPEAKER.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.PRINTER.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.DISK_DRIVE.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.MONITOR_NORMAL.get(), (b, d) -> b.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.MONITOR_ADVANCED.get(), (b, d) -> b.peripheral());

        event.registerBlockEntity(
            PeripheralCapability.get(), BlockEntityType.COMMAND_BLOCK,
            (b, d) -> Config.enableCommandBlock ? new CommandBlockPeripheral(b) : null
        );

        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.WIRELESS_MODEM_NORMAL.get(), WirelessModemBlockEntity::getPeripheral);
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.WIRELESS_MODEM_ADVANCED.get(), WirelessModemBlockEntity::getPeripheral);
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.WIRED_MODEM_FULL.get(), WiredModemFullBlockEntity::getPeripheral);
        event.registerBlockEntity(PeripheralCapability.get(), ModRegistry.BlockEntities.CABLE.get(), CableBlockEntity::getPeripheral);

        event.registerBlockEntity(WiredElementCapability.get(), ModRegistry.BlockEntities.WIRED_MODEM_FULL.get(), (b, d) -> b.getElement());
        event.registerBlockEntity(WiredElementCapability.get(), ModRegistry.BlockEntities.CABLE.get(), CableBlockEntity::getWiredElement);
    }

    @SubscribeEvent
    public static void sync(ModConfigEvent.Loading event) {
        syncConfig(event.getConfig());
    }

    @SubscribeEvent
    public static void sync(ModConfigEvent.Reloading event) {
        syncConfig(event.getConfig());
    }

    private static void syncConfig(ModConfig config) {
        if (!config.getModId().equals(ComputerCraftAPI.MOD_ID)) return;

        var path = config.getConfigData() instanceof FileConfig fileConfig ? fileConfig.getNioPath() : null;

        if (config.getType() == ModConfig.Type.SERVER && ((ForgeConfigFile) ConfigSpec.serverSpec).spec().isLoaded()) {
            ConfigSpec.syncServer(path);
        } else if (config.getType() == ModConfig.Type.CLIENT) {
            ConfigSpec.syncClient(path);
        }
    }

    @SubscribeEvent
    public static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        CommonHooks.onBuildCreativeTab(event.getTabKey(), event.getParameters(), event);
    }

    /**
     * This holds an instance of {@link ClientNetworkContext}. This is a separate class to ensure that the instance is
     * lazily created when needed on the client.
     */
    private static final class ClientHolderHolder {
        private static final @Nullable ClientNetworkContext INSTANCE;
        private static final @Nullable Throwable ERROR;

        static {
            var helper = Services.tryLoad(ClientNetworkContext.class);
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        static ClientNetworkContext get() {
            var instance = INSTANCE;
            return instance == null ? Services.raise(ClientNetworkContext.class, ERROR) : instance;
        }
    }
}
