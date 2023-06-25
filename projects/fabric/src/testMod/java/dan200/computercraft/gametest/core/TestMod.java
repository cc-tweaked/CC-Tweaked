// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.ComputerCraftAPIClient;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.export.Exporter;
import dan200.computercraft.testmod.ModEntrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class TestMod implements ModInitializer, ClientModInitializer {

    public static @Nullable TurtleUpgradeSerialiser<?> enchantedTool = null;

    @Override
    public void onInitialize() {
        TestHooks.init();

        // Register enchantable turtle tool

        Registry<TurtleUpgradeSerialiser<?>> turtleRegistry = (Registry<TurtleUpgradeSerialiser<?>>) BuiltInRegistries.REGISTRY.get(TurtleUpgradeSerialiser.registryId().location());
        enchantedTool = Registry.register(
            turtleRegistry, new ResourceLocation("cctest", ModEntrypoint.ENCHANTABLE_TOOL),
            ModEntrypoint.buildEnchantableTurtleTool().get()
        );

        var phase = new ResourceLocation(ComputerCraftAPI.MOD_ID, "test_mod");
        ServerLifecycleEvents.SERVER_STARTED.addPhaseOrdering(Event.DEFAULT_PHASE, phase);
        ServerLifecycleEvents.SERVER_STARTED.register(phase, TestHooks::onServerStarted);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CCTestCommand.register(dispatcher));

        TestHooks.loadTests(GameTestRegistry::register);
    }

    @Override
    public void onInitializeClient() {
        if (enchantedTool != null) {
            ComputerCraftAPIClient.registerTurtleUpgradeModeller(
                enchantedTool, TurtleUpgradeModeller.flatItem()
            );
        }

        ServerTickEvents.START_SERVER_TICK.register(ClientTestHooks::onServerTick);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> ClientTestHooks.onOpenScreen(screen));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> Exporter.register(dispatcher));
    }
}
