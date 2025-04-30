package com.example.examplemod;

import dan200.computercraft.api.client.turtle.RegisterTurtleModelEvent;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The client-side entry point for the Forge version of our example mod.
 */
@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ForgeExampleModClient {
    // @start region=turtle_model
    @SubscribeEvent
    public static void onRegisterTurtleModellers(RegisterTurtleModelEvent event) {
        event.register(ExampleMod.EXAMPLE_TURTLE_UPGRADE, TurtleUpgradeModel.flatItem());
    }
    // @end region=turtle_model
}
