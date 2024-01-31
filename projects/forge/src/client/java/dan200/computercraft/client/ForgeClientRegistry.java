// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.turtle.RegisterTurtleModellersEvent;
import dan200.computercraft.client.model.turtle.TurtleModelLoader;
import dan200.computercraft.client.turtle.TurtleUpgradeModellers;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;

import java.io.IOException;

/**
 * Registers textures and models for items.
 */
@Mod.EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeClientRegistry {
    private static final Object lock = new Object();
    private static boolean gatheredModellers = false;

    private ForgeClientRegistry() {
    }

    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(new ResourceLocation(ComputerCraftAPI.MOD_ID, "turtle"), TurtleModelLoader.INSTANCE);
    }

    /**
     * Turtle upgrade modellers must be loaded before we gather additional models.
     * <p>
     * Unfortunately, due to the nature of parallel mod loading (resource loading and mod setup events are fired in
     * parallel), there's no way to guarantee this using existing events. Instead, we piggyback off
     * {@link ModelEvent.RegisterAdditional}, registering models the first time the event is fired.
     */
    private static void gatherModellers() {
        if (gatheredModellers) return;
        synchronized (lock) {
            if (gatheredModellers) return;

            gatheredModellers = true;
            ModLoader.get().postEvent(new RegisterTurtleModellersEvent(TurtleUpgradeModellers::register));
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        gatherModellers();
        ClientRegistry.registerExtraModels(event::register);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        ClientRegistry.registerShaders(event.getResourceProvider(), event::registerShader);
    }

    @SubscribeEvent
    public static void onTurtleModellers(RegisterTurtleModellersEvent event) {
        ClientRegistry.registerTurtleModellers(event);
    }

    @SubscribeEvent
    public static void onItemColours(RegisterColorHandlersEvent.Item event) {
        ClientRegistry.registerItemColours(event::register);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        ClientRegistry.registerMenuScreens(event::register);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        ClientRegistry.registerReloadListeners(event::registerReloadListener, Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void setupClient(FMLClientSetupEvent event) {
        ClientRegistry.register();
        event.enqueueWork(ClientRegistry::registerMainThread);
    }
}
