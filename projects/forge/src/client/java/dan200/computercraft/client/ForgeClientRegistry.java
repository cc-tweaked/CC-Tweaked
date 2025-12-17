// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import com.google.common.reflect.TypeToken;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.turtle.RegisterTurtleModelEvent;
import dan200.computercraft.client.platform.ClientNetworkContextImpl;
import dan200.computercraft.client.platform.ForgeModelKey;
import dan200.computercraft.client.platform.ModelKey;
import dan200.computercraft.client.render.ExtendedItemFrameRenderState;
import dan200.computercraft.shared.network.NetworkMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.model.standalone.UnbakedStandaloneModel;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;


/**
 * Registers textures and models for items.
 */
@EventBusSubscriber(modid = ComputerCraftAPI.MOD_ID, value = Dist.CLIENT)
public final class ForgeClientRegistry {
    static final ContextKey<ExtendedItemFrameRenderState> ITEM_FRAME_STATE = new ContextKey<>(Identifier.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item_frame"));

    private ForgeClientRegistry() {
    }

    @SubscribeEvent
    public static void registerNetwork(RegisterClientPayloadHandlersEvent event) {
        var context = new ClientNetworkContextImpl();
        for (var type : NetworkMessages.getClientbound()) {
            event.register(type.type(), (packet, ctx) -> ctx.enqueueWork(() -> packet.handle(context)));
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterStandalone event) {
        // Load resources
        Queue<Runnable> tasks = new ArrayDeque<>();
        var state = ClientRegistry.gatherExtraModels(Minecraft.getInstance().getResourceManager(), tasks::add);
        Runnable task;
        while ((task = tasks.poll()) != null) task.run();

        ClientRegistry.registerExtraModels(new ClientRegistry.RegisterExtraModels() {
            @Override
            public <U, T> void register(ModelKey<T> key, U unbaked, BiConsumer<U, ResolvableModel.Resolver> resolve, BiFunction<U, ModelBaker, T> bake) {
                event.register(ForgeModelKey.key(key), new ModelWrapper<>(unbaked, resolve, bake));
            }
        }, state.resultNow());
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
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<ItemFrameRenderer<?>>() {
        }, (e, s) -> {
            var data = s.getRenderData(ITEM_FRAME_STATE);
            if (data == null) s.setRenderData(ITEM_FRAME_STATE, data = new ExtendedItemFrameRenderState());
            data.setup(e.getItem());
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        ClientRegistry.registerLayerDefinitions(event::registerLayerDefinition);
    }

    @SubscribeEvent
    public static void registerPictureInPictureRenderers(RegisterPictureInPictureRenderersEvent event) {
        ClientRegistry.registerPictureInPictureRenderers(event::register);
    }

    @SubscribeEvent
    public static void registerDebugScreenEntries(RegisterDebugEntriesEvent event) {
        ClientRegistry.registerDebugScreenEntries(event::register);
    }

    @SubscribeEvent
    public static void setupClient(FMLClientSetupEvent event) {
        ClientRegistry.register();
    }

    private record ModelWrapper<U, T>(
        U model, BiConsumer<U, ResolvableModel.Resolver> resolve, BiFunction<U, ModelBaker, T> bake
    ) implements UnbakedStandaloneModel<T> {
        @Override
        public T bake(ModelBaker baker) {
            return bake().apply(model(), baker);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolve().accept(model(), resolver);
        }
    }
}
