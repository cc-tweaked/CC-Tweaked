// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.FabricComputerCraftAPIClient;
import dan200.computercraft.client.model.CustomModelLoader;
import dan200.computercraft.impl.Services;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.config.ConfigSpec;
import dan200.computercraft.shared.network.NetworkMessages;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import dan200.computercraft.shared.platform.FabricConfigFile;
import dan200.computercraft.shared.platform.FabricMessageType;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Objects;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

public class ComputerCraftClient {
    public static void init() {
        var clientNetwork = Services.load(ClientNetworkContext.class);
        for (var type : NetworkMessages.getClientbound()) {
            ClientPlayNetworking.registerGlobalReceiver(
                FabricMessageType.toFabricType(type), (packet, player, responseSender) -> packet.payload().handle(clientNetwork)
            );
        }

        ClientRegistry.register();
        ClientRegistry.registerTurtleModellers(FabricComputerCraftAPIClient::registerTurtleUpgradeModeller);
        ClientRegistry.registerItemColours(ColorProviderRegistry.ITEM::register);
        ClientRegistry.registerMainThread(ItemProperties::register);

        PreparableModelLoadingPlugin.register(CustomModelLoader::prepare, (state, context) -> {
            ClientRegistry.registerExtraModels(context::addModels);
            context.resolveModel().register(ctx -> state.loadModel(ctx.id()));
            context.modifyModelAfterBake().register((model, ctx) -> model == null ? null : state.wrapModel(ctx, model));
        });

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
                return !ClientHooks.drawHighlight(context.matrixStack(), assertNonNull(context.consumers()), context.camera(), blockHit);
            } else {
                return true;
            }
        });

        // Easier to hook in as an event than use BlockPickInteractionAware.
        ClientPickBlockGatherCallback.EVENT.register((player, hit) -> {
            if (hit.getType() != HitResult.Type.BLOCK) return ItemStack.EMPTY;

            var pos = ((BlockHitResult) hit).getBlockPos();
            var level = Objects.requireNonNull(Minecraft.getInstance().level);
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CableBlock cable)) return ItemStack.EMPTY;

            return cable.getCloneItemStack(state, hit, level, pos, player);
        });

        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess) -> ClientRegistry.registerClientCommands(dispatcher, FabricClientCommandSource::sendError)
        );

        ((FabricConfigFile) ConfigSpec.clientSpec).load(FabricLoader.getInstance().getConfigDir().resolve(ComputerCraftAPI.MOD_ID + "-client.toml"));
    }
}
