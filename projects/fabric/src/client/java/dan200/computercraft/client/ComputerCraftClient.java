// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.FabricComputerCraftAPIClient;
import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.client.model.ExtraModels;
import dan200.computercraft.client.platform.FabricModelKey;
import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.impl.Services;
import dan200.computercraft.shared.CommonHooks;
import dan200.computercraft.shared.ComputerCraft;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.platform.FabricConfigFile;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedExtraModel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.phys.BlockHitResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

public class ComputerCraftClient {
    public static void init() {
        var clientNetwork = Services.load(ClientNetworkContext.class);
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

        PreparableModelLoadingPlugin.register(
            (resources, executor) -> CompletableFuture.supplyAsync(() -> ExtraModels.loadAll(resources), executor),
            (state, context) -> ClientRegistry.registerExtraModels(
                (key, model) -> context.addModel(FabricModelKey.key(key), new SimpleUnbakedExtraModel<>(model, StandaloneModel::of)),
                (key, model) -> context.addModel(FabricModelKey.erased(key), new TurtleModelWrapper<>(model)),
                state
            )
        );

        BlockRenderLayerMap.INSTANCE.putBlock(ModRegistry.Blocks.COMPUTER_NORMAL.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModRegistry.Blocks.COMPUTER_COMMAND.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModRegistry.Blocks.COMPUTER_ADVANCED.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModRegistry.Blocks.MONITOR_NORMAL.get(), RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModRegistry.Blocks.MONITOR_ADVANCED.get(), RenderType.cutout());

        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientHooks.onTick());
        // This isn't 100% consistent with Forge, but not worth a mixin.
        WorldRenderEvents.START.register(context -> ClientHooks.onRenderTick());
        WorldRenderEvents.BLOCK_OUTLINE.register((context, hitResult) -> {
            var hit = Minecraft.getInstance().hitResult;
            if (hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(hitResult.blockPos())) {
                return !ClientHooks.drawHighlight(Nullability.assertNonNull(context.matrixStack()), assertNonNull(context.consumers()), context.camera(), blockHit);
            } else {
                return true;
            }
        });

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

        ItemTooltipCallback.EVENT.register(CommonHooks::onItemTooltip);

        ((FabricConfigFile) ConfigSpec.clientSpec).load(FabricLoader.getInstance().getConfigDir().resolve(ComputerCraftAPI.MOD_ID + "-client.toml"));
    }

    private record TurtleModelWrapper<T extends ITurtleUpgrade>(
        TurtleUpgradeModel.Unbaked<T> model
    ) implements UnbakedExtraModel<TurtleUpgradeModel<T>> {
        @Override
        public TurtleUpgradeModel<T> bake(ModelBaker baker) {
            return model().bake(baker);
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            model().resolveDependencies(resolver);
        }
    }
}
