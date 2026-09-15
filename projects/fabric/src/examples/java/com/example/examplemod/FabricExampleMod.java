package com.example.examplemod;

import com.example.examplemod.peripheral.BrewingStandPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

/**
 * The main entry point for our example mod.
 */
public class FabricExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // @start region=turtle_upgrades
        var turtleUpgradeSerialisers = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).lookupOrThrow(ITurtleUpgrade.typeRegistry());
        Registry.register(turtleUpgradeSerialisers, Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, "example_turtle_upgrade"), ExampleMod.EXAMPLE_TURTLE_UPGRADE);
        // @end region=turtle_upgrades

        ExampleMod.registerComputerCraft();

        // @start region=peripherals
        PeripheralLookup.get().registerForBlockEntity((f, s) -> new BrewingStandPeripheral(f), BlockEntityTypes.BREWING_STAND);
        // @end region=peripherals
    }
}
