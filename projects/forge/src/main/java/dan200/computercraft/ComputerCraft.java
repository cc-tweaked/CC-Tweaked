/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.ForgeComputerCraftAPI;
import dan200.computercraft.api.detail.ForgeDetailRegistries;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.api.turtle.event.TurtleRefuelEvent;
import dan200.computercraft.impl.TurtleRefuelHandlers;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.details.FluidData;
import dan200.computercraft.shared.peripheral.generic.methods.EnergyMethods;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.platform.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.OptionalInt;

@Mod(ComputerCraftAPI.MOD_ID)
@Mod.EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ComputerCraft {
    public ComputerCraft() {
        ModRegistry.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigSpec.serverSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigSpec.clientSpec);

        // Register a fallback handler for the turtle refuel event.
        TurtleRefuelHandlers.register((turtle, stack, slot, limit) -> {
            @SuppressWarnings("removal") var event = new TurtleRefuelEvent(turtle, stack);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.getHandler() == null) return OptionalInt.empty();
            if (limit == 0) return OptionalInt.of(0);
            return OptionalInt.of(event.getHandler().refuel(turtle, stack, slot, limit));
        });

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
        event.register(IWiredElement.class);
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
