// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft;

import com.electronwill.nightconfig.core.file.FileConfig;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.detail.ForgeDetailRegistries;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.shared.CommonHooks;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.details.FluidData;
import dan200.computercraft.shared.integration.CreateIntegration;
import dan200.computercraft.shared.integration.MoreRedIntegration;
import dan200.computercraft.shared.peripheral.generic.methods.EnergyMethods;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.platform.ForgeConfigFile;
import dan200.computercraft.shared.platform.NetworkHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

@Mod(ComputerCraftAPI.MOD_ID)
@Mod.EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ComputerCraft {
    public ComputerCraft() {
        ModRegistry.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ((ForgeConfigFile) ConfigSpec.serverSpec).spec());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ((ForgeConfigFile) ConfigSpec.clientSpec).spec());

        NetworkHandler.setup();
    }

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.create(new RegistryBuilder<TurtleUpgradeSerialiser<?>>()
            .setName(TurtleUpgradeSerialiser.registryId().location())
            .disableSaving().disableSync());

        event.create(new RegistryBuilder<PocketUpgradeSerialiser<?>>()
            .setName(PocketUpgradeSerialiser.registryId().location())
            .disableSaving().disableSync());
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(WiredElement.class);
        event.register(IPeripheral.class);
    }

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(ModRegistry::registerMainThread);

        ComputerCraftAPI.registerGenericSource(new InventoryMethods());
        ComputerCraftAPI.registerGenericSource(new FluidMethods());
        ComputerCraftAPI.registerGenericSource(new EnergyMethods());

        ForgeComputerCraftAPI.registerGenericCapability(ForgeCapabilities.ITEM_HANDLER);
        ForgeComputerCraftAPI.registerGenericCapability(ForgeCapabilities.ENERGY);
        ForgeComputerCraftAPI.registerGenericCapability(ForgeCapabilities.FLUID_HANDLER);

        ForgeDetailRegistries.FLUID_STACK.addProvider(FluidData::fill);

        if (ModList.get().isLoaded(CreateIntegration.ID)) event.enqueueWork(CreateIntegration::setup);
        if (ModList.get().isLoaded(MoreRedIntegration.MOD_ID)) MoreRedIntegration.setup();
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
}
