// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core;

import dan200.computercraft.export.Exporter;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod("cctest")
public class TestMod {
    public TestMod(IEventBus modBus) {
        TestHooks.init();

        var bus = NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.LOW, (ServerStartedEvent e) -> TestHooks.onServerStarted(e.getServer()));
        bus.addListener((RegisterCommandsEvent e) -> CCTestCommand.register(e.getDispatcher(), e.getBuildContext()));
        bus.addListener((BlockEvent.BreakEvent e) -> {
            if (TestHooks.onBeforeDestroyBlock(e.getLevel(), e.getPos(), e.getState())) e.setCanceled(true);
        });

        if (FMLEnvironment.dist == Dist.CLIENT) TestMod.onInitializeClient();

        var tests = TestHooks.loadTests();
        modBus.addListener((RegisterEvent event) -> {
            for (var test : tests) event.register(Registries.TEST_FUNCTION, test.getId(), test::getFunction);
        });
        modBus.addListener((RegisterGameTestsEvent event) -> {
            for (var test : tests) event.registerTest(test.getId(), test.getInstance());
        });
    }

    private static void onInitializeClient() {
        var bus = NeoForge.EVENT_BUS;

        bus.addListener((ServerTickEvent.Pre e) -> ClientTestHooks.onServerTick(e.getServer()));
        bus.addListener((ScreenEvent.Opening e) -> {
            if (ClientTestHooks.onOpenScreen(e.getScreen())) e.setCanceled(true);
        });
        bus.addListener((RegisterClientCommandsEvent e) -> Exporter.register(e.getDispatcher()));
    }
}
