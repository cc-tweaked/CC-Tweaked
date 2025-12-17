// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.FabricComputerCraftAPIClient;
import dan200.computercraft.client.platform.ClientNetworkContextImpl;
import dan200.computercraft.client.platform.FabricModelKey;
import dan200.computercraft.client.platform.ModelKey;
import dan200.computercraft.client.render.BlockOutlineRenderer;
import dan200.computercraft.shared.ComputerCraft;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.platform.FabricConfigFile;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedExtraModel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.Util;
import net.minecraft.world.phys.BlockHitResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ComputerCraftClient {
    public static void init() {
        var clientNetwork = new ClientNetworkContextImpl();
        for (var type : NetworkMessages.getClientbound()) {
            ClientPlayNetworking.registerGlobalReceiver(
                type.type(), (packet, responseSender) -> packet.handle(clientNetwork)
            );
        }

        ClientRegistry.register();
        ClientRegistry.registerTurtleModels(FabricComputerCraftAPIClient::registerTurtleUpgradeModeller);
        ClientRegistry.registerMenuScreens(MenuScreens::register);
        ClientRegistry.registerItemModels(ItemModels.ID_MAPPER::put);
        ClientRegistry.registerItemColours(ItemTintSources.ID_MAPPER::put);
        ClientRegistry.registerSelectItemProperties(SelectItemModelProperties.ID_MAPPER::put);
        ClientRegistry.registerConditionalItemProperties(ConditionalItemModelProperties.ID_MAPPER::put);
        ClientRegistry.registerLayerDefinitions((id, factory) -> EntityModelLayerRegistry.registerModelLayer(id, factory::get));

        PreparableModelLoadingPlugin.register(
            (state, executor) -> ClientRegistry.gatherExtraModels(state.resourceManager(), executor),
            (state, context) -> ClientRegistry.registerExtraModels(new ClientRegistry.RegisterExtraModels() {
                @Override
                public <U, T> void register(ModelKey<T> key, U unbaked, BiConsumer<U, ResolvableModel.Resolver> resolve, BiFunction<U, ModelBaker, T> bake) {
                    context.addModel(FabricModelKey.key(key), new ModelWrapper<>(unbaked, resolve, bake));
                }
            }, state)
        );

        BlockRenderLayerMap.putBlock(ModRegistry.Blocks.COMPUTER_NORMAL.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModRegistry.Blocks.COMPUTER_COMMAND.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModRegistry.Blocks.COMPUTER_ADVANCED.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModRegistry.Blocks.MONITOR_NORMAL.get(), ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(ModRegistry.Blocks.MONITOR_ADVANCED.get(), ChunkSectionLayer.CUTOUT);

        ClientRegistry.registerPictureInPictureRenderers(new ClientRegistry.RegisterPictureInPictureRenderer() {
            @Override
            public <T extends PictureInPictureRenderState> void register(Class<T> ty, Function<MultiBufferSource.BufferSource, PictureInPictureRenderer<T>> f) {
                SpecialGuiElementRegistry.register(c -> f.apply(c.vertexConsumers()));
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientHooks.onTick());
        // This isn't 100% consistent with Forge, but not worth a mixin.
        WorldRenderEvents.START_MAIN.register(context -> ClientHooks.onRenderTick());
        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hitResult) -> {
            var hit = Minecraft.getInstance().hitResult;
            if (!(hit instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(hitResult.pos())) {
                return true;
            }

            var camera = context.gameRenderer().getMainCamera();
            var renderer = ClientHooks.drawHighlight(camera, blockHit);
            if (renderer == null) return true;

            BlockOutlineRenderer.render(context.matrices(), context.consumers(), renderer);
            return false;
        });

        ClientRegistry.registerDebugScreenEntries(DebugScreenEntries::register);

        // Register our open folder command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal(ComputerCraft.CLIENT_OPEN_FOLDER)
                .requires(x -> Minecraft.getInstance().getSingleplayerServer() != null)
                .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("path", StringArgumentType.string())
                    .executes(c -> {
                        var file = Path.of(c.getArgument("path", String.class));
                        if (Files.isDirectory(file)) Util.getPlatform().openFile(file.toFile());
                        return 0;
                    })
                )));

        ((FabricConfigFile) ConfigSpec.clientSpec).load(FabricLoader.getInstance().getConfigDir().resolve(ComputerCraftAPI.MOD_ID + "-client.toml"));
    }

    private record ModelWrapper<U, T>(
        U model, BiConsumer<U, Resolver> resolve, BiFunction<U, ModelBaker, T> bake
    ) implements UnbakedExtraModel<T> {
        @Override
        public T bake(ModelBaker baker) {
            return bake().apply(model(), baker);
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            resolve().accept(model(), resolver);
        }
    }
}
