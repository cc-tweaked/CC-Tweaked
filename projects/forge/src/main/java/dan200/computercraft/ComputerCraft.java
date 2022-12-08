/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.detail.ForgeDetailRegistries;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.details.FluidData;
import dan200.computercraft.shared.peripheral.generic.methods.EnergyMethods;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.platform.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigSpec.serverSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigSpec.clientSpec);

        NetworkHandler.setup();
    }

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.create(new RegistryBuilder<TurtleUpgradeSerialiser<?>>()
            .setName(TurtleUpgradeSerialiser.REGISTRY_ID.location())
            .disableSaving().disableSync());

        event.create(new RegistryBuilder<PocketUpgradeSerialiser<?>>()
            .setName(PocketUpgradeSerialiser.REGISTRY_ID.location())
            .disableSaving().disableSync());
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(WiredElement.class);
        event.register(IPeripheral.class);
    }

    @SubscribeEvent
    public static void registerCreativeTab(CreativeModeTabEvent.Register event) {
        event.registerCreativeModeTab(new ResourceLocation(ComputerCraftAPI.MOD_ID, "tab"), ModRegistry::registerCreativeTab);
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
    }

    @SubscribeEvent
    public static void sync(ModConfigEvent.Loading event) {
        ConfigSpec.sync(event.getConfig());
    }

    @SubscribeEvent
    public static void sync(ModConfigEvent.Reloading event) {
        ConfigSpec.sync(event.getConfig());
    }
}
