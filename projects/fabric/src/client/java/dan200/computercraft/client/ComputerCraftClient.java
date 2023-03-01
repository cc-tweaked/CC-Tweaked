// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import dan200.computercraft.client.model.turtle.TurtleModelLoader;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.network.client.ClientNetworkContext;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import dan200.computercraft.shared.platform.NetworkHandler;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static dan200.computercraft.core.util.Nullability.assertNonNull;

public class ComputerCraftClient {
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.ID, (client, handler, buf, responseSender) -> {
            var packet = NetworkHandler.decodeClient(buf);
            if (packet != null) client.execute(() -> packet.handle(ClientNetworkContext.get()));
        });

        ClientRegistry.register();
        ClientRegistry.registerItemColours(ColorProviderRegistry.ITEM::register);
        ClientRegistry.registerBlockEntityRenderers(BlockEntityRendererRegistry::register);
        ClientRegistry.registerMainThread();


        ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) -> ClientRegistry.registerExtraModels(out));
        ModelLoadingRegistry.INSTANCE.registerResourceProvider(loader -> (path, ctx) -> TurtleModelLoader.load(loader, path));
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
            var level = Minecraft.getInstance().level;
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CableBlock cable)) return ItemStack.EMPTY;

            return cable.getCloneItemStack(state, hit, level, pos, player);
        });
    }
}
