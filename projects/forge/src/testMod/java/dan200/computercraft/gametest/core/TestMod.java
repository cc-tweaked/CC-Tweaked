// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest.core;

import dan200.computercraft.api.client.ComputerCraftAPIClient;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.export.Exporter;
import dan200.computercraft.testmod.EnchantableTurtleTool;
import dan200.computercraft.testmod.ModEntrypoint;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod("cctest")
public class TestMod {
    public static DeferredRegister<TurtleUpgradeSerialiser<?>> turtleRegistry = DeferredRegister.create(
        TurtleUpgradeSerialiser.registryId(),
        "cctest"
    );
    public static Supplier<TurtleUpgradeSerialiser<EnchantableTurtleTool>> ENCHANTED_TOOL = turtleRegistry.register(
        ModEntrypoint.ENCHANTABLE_TOOL,
        ModEntrypoint.buildEnchantableTurtleTool()
    );

    public TestMod() {
        TestHooks.init();

        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(EventPriority.LOW, (ServerStartedEvent e) -> TestHooks.onServerStarted(e.getServer()));
        bus.addListener((RegisterCommandsEvent e) -> CCTestCommand.register(e.getDispatcher()));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> TestMod::onInitializeClient);

        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((RegisterGameTestsEvent event) -> TestHooks.loadTests(event::register));
        turtleRegistry.register(modBus);
    }

    private static void onInitializeClient() {
        var bus = MinecraftForge.EVENT_BUS;

        bus.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase == TickEvent.Phase.START) ClientTestHooks.onServerTick(e.getServer());
        });
        bus.addListener((ScreenEvent.Opening e) -> {
            if (ClientTestHooks.onOpenScreen(e.getScreen())) e.setCanceled(true);
        });
        bus.addListener((RegisterClientCommandsEvent e) -> Exporter.register(e.getDispatcher()));

        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((FMLClientSetupEvent e) -> ComputerCraftAPIClient.registerTurtleUpgradeModeller(ENCHANTED_TOOL.get(), TurtleUpgradeModeller.flatItem()));
    }
}
