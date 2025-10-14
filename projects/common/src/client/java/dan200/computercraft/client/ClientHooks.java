// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.render.CableHighlightRenderer;
import dan200.computercraft.client.render.ExtendedItemFrameRenderState;
import dan200.computercraft.client.render.PocketItemRenderer;
import dan200.computercraft.client.render.PrintoutItemRenderer;
import dan200.computercraft.client.render.monitor.MonitorHighlightRenderer;
import dan200.computercraft.client.render.monitor.MonitorRenderState;
import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.shared.CommonHooks;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.util.PauseAwareTimer;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Event listeners for client-only code.
 * <p>
 * This is the client-only version of {@link CommonHooks}, and so should be where all client-specific event handlers are
 * defined.
 */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void onTick() {
        FrameInfo.onTick();
    }

    public static void onRenderTick() {
        PauseAwareTimer.tick(Minecraft.getInstance().isPaused());
        FrameInfo.onRenderTick();
    }

    public static void onWorldUnload() {
        MonitorRenderState.destroyAll();
        SpeakerManager.reset();
    }

    public static void onDisconnect() {
        ClientPocketComputers.reset();
    }

    public static boolean drawHighlight(PoseStack transform, MultiBufferSource bufferSource, Camera camera, BlockHitResult hit) {
        // TODO: Reconsider this API once https://github.com/FabricMC/fabric/pull/4906/ is merged.
        return CableHighlightRenderer.drawHighlight(transform, bufferSource, camera, hit)
            || MonitorHighlightRenderer.drawHighlight(transform, bufferSource, camera, hit);
    }

    public static boolean onRenderHeldItem(
        PoseStack transform, SubmitNodeCollector collector, int lightTexture, InteractionHand hand,
        float pitch, float equipProgress, float swingProgress, ItemStack stack
    ) {
        if (stack.getItem() instanceof PocketComputerItem) {
            PocketItemRenderer.INSTANCE.renderItemFirstPerson(transform, collector, lightTexture, hand, pitch, equipProgress, swingProgress, stack);
            return true;
        }
        if (stack.getItem() instanceof PrintoutItem) {
            PrintoutItemRenderer.INSTANCE.renderItemFirstPerson(transform, collector, lightTexture, hand, pitch, equipProgress, swingProgress, stack);
            return true;
        }

        return false;
    }

    public static boolean onRenderItemFrame(PoseStack transform, SubmitNodeCollector render, ItemFrameRenderState frame, ExtendedItemFrameRenderState state) {
        if (state.printoutData != null) {
            transform.mulPose(Axis.ZP.rotationDegrees(frame.rotation * 360.0f / 8.0f));
            PrintoutItemRenderer.onRenderInFrame(transform, render, frame, state.printoutData, state.isBook);
            return true;
        }

        return false;
    }

    public static void onPlayStreaming(SoundEngine engine, Channel channel, AudioStream stream) {
        SpeakerManager.onPlayStreaming(engine, channel, stream);
    }

    public static BlockState getBlockBreakingState(BlockState state, BlockPos pos) {
        // Only apply to cables which have both a cable and modem
        if (state.getBlock() != ModRegistry.Blocks.CABLE.get()
            || !state.getValue(CableBlock.CABLE)
            || state.getValue(CableBlock.MODEM) == CableModemVariant.None
        ) {
            return state;
        }

        var hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return state;
        var hitPos = ((BlockHitResult) hit).getBlockPos();

        if (!hitPos.equals(pos)) return state;

        return WorldUtil.isVecInside(CableShapes.getModemShape(state), hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()))
            ? state.getBlock().defaultBlockState().setValue(CableBlock.MODEM, state.getValue(CableBlock.MODEM))
            : state.setValue(CableBlock.MODEM, CableModemVariant.None);
    }
}
