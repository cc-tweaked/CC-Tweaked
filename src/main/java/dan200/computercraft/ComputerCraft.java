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
import dan200.computercraft.shared.Config;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.peripheral.generic.data.FluidData;
import dan200.computercraft.shared.peripheral.generic.methods.EnergyMethods;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ComputerCraft.MOD_ID)
@Mod.EventBusSubscriber(modid = ComputerCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ComputerCraft {
    public static final String MOD_ID = "computercraft";

    public static int computerSpaceLimit = 1000 * 1000;
    public static int floppySpaceLimit = 125 * 1000;
    public static boolean commandRequireCreative = true;

    public static int computerThreads = 1;

    public static boolean enableCommandBlock = false;
    public static int modemRange = 64;
    public static int modemHighAltitudeRange = 384;
    public static int modemRangeDuringStorm = 64;
    public static int modemHighAltitudeRangeDuringStorm = 384;
    public static int maxNotesPerTick = 8;
    public static MonitorRenderer monitorRenderer = MonitorRenderer.BEST;
    public static int monitorDistance = 65;
    public static long monitorBandwidth = 1_000_000;

    public static boolean turtlesNeedFuel = true;
    public static int turtleFuelLimit = 20000;
    public static int advancedTurtleFuelLimit = 100000;
    public static boolean turtlesObeyBlockProtection = true;
    public static boolean turtlesCanPush = true;

    public static int computerTermWidth = 51;
    public static int computerTermHeight = 19;

    public static final int turtleTermWidth = 39;
    public static final int turtleTermHeight = 13;

    public static int pocketTermWidth = 26;
    public static int pocketTermHeight = 20;

    public static int monitorWidth = 8;
    public static int monitorHeight = 6;

    public static int uploadNagDelay = 5;

    public static final Logger log = LoggerFactory.getLogger(MOD_ID);

    public ComputerCraft() {
        Config.setup();
        ModRegistry.register();
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
        NetworkHandler.setup();

        event.enqueueWork(ModRegistry::registerMainThread);

        ComputerCraftAPI.registerGenericSource(new InventoryMethods());
        ComputerCraftAPI.registerGenericSource(new FluidMethods());
        ComputerCraftAPI.registerGenericSource(new EnergyMethods());

        ForgeComputerCraftAPI.registerGenericCapability(ForgeCapabilities.ITEM_HANDLER);
        ForgeComputerCraftAPI.registerGenericCapability(ForgeCapabilities.ENERGY);
        ForgeComputerCraftAPI.registerGenericCapability(ForgeCapabilities.FLUID_HANDLER);

        ForgeDetailRegistries.FLUID_STACK.addProvider(FluidData::fill);
    }

}
