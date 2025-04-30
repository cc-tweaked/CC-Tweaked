package com.example.examplemod;

import dan200.computercraft.api.client.FabricComputerCraftAPIClient;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import net.fabricmc.api.ClientModInitializer;

public class FabricExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // @start region=turtle_model
        FabricComputerCraftAPIClient.registerTurtleUpgradeModeller(ExampleMod.EXAMPLE_TURTLE_UPGRADE, TurtleUpgradeModel.flatItem());
        // @end region=turtle_model
    }
}
