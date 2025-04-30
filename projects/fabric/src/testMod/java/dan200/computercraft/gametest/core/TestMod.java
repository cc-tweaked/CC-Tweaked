// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.export.Exporter;
import dan200.computercraft.gametest.api.ClientTestEnvironment;
import dan200.computercraft.mixin.gametest.RegistryDataLoaderLoaderAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

public class TestMod implements ModInitializer, ClientModInitializer {
    private static @Nullable List<TestInstance> tests = null;

    @Override
    public void onInitialize() {
        TestHooks.init();

        var phase = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "test_mod");
        ServerLifecycleEvents.SERVER_STARTED.addPhaseOrdering(Event.DEFAULT_PHASE, phase);
        ServerLifecycleEvents.SERVER_STARTED.register(phase, TestHooks::onServerStarted);
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) -> CCTestCommand.register(dispatcher, buildContext));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !TestHooks.onBeforeDestroyBlock(level, pos, state));

        Registry.register(BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE, ResourceLocation.fromNamespaceAndPath(TestHooks.MOD_ID, "client"), ClientTestEnvironment.CODEC);

        var tests = TestMod.tests = TestHooks.loadTests();
        for (var test : tests) Registry.register(BuiltInRegistries.TEST_FUNCTION, test.getId(), test.getFunction());
    }

    @Override
    public void onInitializeClient() {
        ServerTickEvents.START_SERVER_TICK.register(ClientTestHooks::onServerTick);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> ClientTestHooks.onOpenScreen(screen));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> Exporter.register(dispatcher));
    }

    public static void registerDynamicEntries(List<RegistryDataLoaderLoaderAccessor<?>> registriesList) {
        var registries = new IdentityHashMap<ResourceKey<? extends Registry<?>>, Registry<?>>(registriesList.size());
        for (var entry : registriesList) registries.put(entry.getRegistry().key(), entry.getRegistry());

        @SuppressWarnings("unchecked") var testInstances = (Registry<GameTestInstance>) registries.get(Registries.TEST_INSTANCE);
        if (testInstances == null) return;
        for (var test : Objects.requireNonNull(tests)) {
            if (!testInstances.containsKey(test.getId())) {
                Registry.register(testInstances, test.getId(), test.getInstance());
            }
        }
    }
}
