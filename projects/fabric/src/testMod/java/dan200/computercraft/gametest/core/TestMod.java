// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.export.Exporter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.resources.ResourceLocation;

public class TestMod implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        TestHooks.init();

        var phase = new ResourceLocation(ComputerCraftAPI.MOD_ID, "test_mod");
        ServerLifecycleEvents.SERVER_STARTED.addPhaseOrdering(Event.DEFAULT_PHASE, phase);
        ServerLifecycleEvents.SERVER_STARTED.register(phase, TestHooks::onServerStarted);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CCTestCommand.register(dispatcher));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !TestHooks.onBeforeDestroyBlock(level, pos, state));

        TestHooks.loadTests(GameTestRegistry::register);
    }

    @Override
    public void onInitializeClient() {
        ServerTickEvents.START_SERVER_TICK.register(ClientTestHooks::onServerTick);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> ClientTestHooks.onOpenScreen(screen));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> Exporter.register(dispatcher));
    }
}
