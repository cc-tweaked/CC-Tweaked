package com.example.examplemod;

import com.example.examplemod.peripheral.BrewingStandPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The main entry point for the Forge version of our example mod.
 */
@Mod(ExampleMod.MOD_ID)
public class ForgeExampleMod {
    public ForgeExampleMod(IEventBus modBus) {
        // Register our turtle upgrade. If writing a Forge-only mod, you'd normally use DeferredRegister instead.
        // However, this is an easy way to implement this in a multi-loader-compatible manner.

        // @start region=turtle_upgrades
        modBus.addListener((RegisterEvent event) -> {
            event.register(
                ITurtleUpgrade.typeRegistry(),
                ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "example_turtle_upgrade"),
                () -> ExampleMod.EXAMPLE_TURTLE_UPGRADE
            );
        });
        // @end region=turtle_upgrades

        modBus.addListener((FMLCommonSetupEvent event) -> ExampleMod.registerComputerCraft());

        // @start region=peripherals
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(PeripheralCapability.get(), BlockEntityType.BREWING_STAND, (b, d) -> new BrewingStandPeripheral(b));
        });
        // @end region=peripherals
    }
}
