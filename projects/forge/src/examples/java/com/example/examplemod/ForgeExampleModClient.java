package com.example.examplemod;

import dan200.computercraft.api.client.turtle.RegisterTurtleModellersEvent;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * The client-side entry point for the Forge version of our example mod.
 */
@EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ForgeExampleModClient {
    // @start region=turtle_modellers
    @SubscribeEvent
    public static void onRegisterTurtleModellers(RegisterTurtleModellersEvent event) {
        event.register(ExampleMod.EXAMPLE_TURTLE_UPGRADE, TurtleUpgradeModeller.flatItem());
    }
    // @end region=turtle_modellers
}
