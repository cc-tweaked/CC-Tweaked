// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import com.google.common.reflect.TypeToken;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.api.client.turtle.RegisterTurtleModelEvent;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.client.model.ExtraModels;
import dan200.computercraft.client.platform.ForgeModelKey;
import dan200.computercraft.client.render.ExtendedItemFrameRenderState;
import dan200.computercraft.client.turtle.TurtleUpgradeModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.model.standalone.UnbakedStandaloneModel;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;


/**
 * Registers textures and models for items.
 */
@EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ForgeClientRegistry {
    static final ContextKey<ExtendedItemFrameRenderState> ITEM_FRAME_STATE = new ContextKey<>(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item_frame"));

    private ForgeClientRegistry() {
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        TurtleUpgradeModels.fetch(() -> ModLoader.postEvent(new RegisterTurtleModelEvent(TurtleUpgradeModels::register)));

        var extraModels = ExtraModels.loadAll(Minecraft.getInstance().getResourceManager());
        ClientRegistry.registerExtraModels(
            (key, model) -> event.register(ForgeModelKey.key(key), StandaloneModel::of),
            (key, model) -> event.register(ForgeModelKey.erased(key), new TurtleModelWrapper<>(model)),
            extraModels
        );
    }

    @SubscribeEvent
    public static void registerTurtleModels(RegisterTurtleModelEvent event) {
        ClientRegistry.registerTurtleModels(event);
    }

    @SubscribeEvent
    public static void registerItemModels(RegisterItemModelsEvent event) {
        ClientRegistry.registerItemModels(event::register);
    }

    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.ItemTintSources event) {
        ClientRegistry.registerItemColours(event::register);
    }

    @SubscribeEvent
    public static void registerSelectItemProperties(RegisterSelectItemModelPropertyEvent event) {
        ClientRegistry.registerSelectItemProperties(event::register);
    }

    @SubscribeEvent
    public static void registerConditionalItemProperties(RegisterConditionalItemModelPropertyEvent event) {
        ClientRegistry.registerConditionalItemProperties(event::register);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        ClientRegistry.registerMenuScreens(event::register);
    }

    @SubscribeEvent
    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        ClientRegistry.registerReloadListeners(event::addListener, Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<ItemFrameRenderer<?>>() {
        }, (e, s) -> {
            var data = s.getRenderData(ITEM_FRAME_STATE);
            if (data == null) s.setRenderData(ITEM_FRAME_STATE, data = new ExtendedItemFrameRenderState());
            data.setup(e.getItem());
        });
    }

    @SubscribeEvent
    public static void setupClient(FMLClientSetupEvent event) {
        ClientRegistry.register();
    }

    private record TurtleModelWrapper<T extends ITurtleUpgrade>(
        TurtleUpgradeModel.Unbaked<T> model
    ) implements UnbakedStandaloneModel<TurtleUpgradeModel<T>> {
        @Override
        public TurtleUpgradeModel<T> bake(ModelBaker baker) {
            return model().bake(baker);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            model().resolveDependencies(resolver);
        }
    }
}
