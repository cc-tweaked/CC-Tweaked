// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.render.CableHighlightRenderer;
import dan200.computercraft.client.render.PocketItemRenderer;
import dan200.computercraft.client.render.PrintoutItemRenderer;
import dan200.computercraft.client.render.monitor.MonitorBlockEntityRenderer;
import dan200.computercraft.client.render.monitor.MonitorHighlightRenderer;
import dan200.computercraft.client.render.monitor.MonitorRenderState;
import dan200.computercraft.client.sound.SpeakerManager;
import dan200.computercraft.shared.CommonHooks;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.PrintoutItem;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import dan200.computercraft.shared.peripheral.modem.wired.CableShapes;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import dan200.computercraft.shared.util.PauseAwareTimer;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.function.Consumer;

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
        ClientPocketComputers.reset();
    }

    public static boolean drawHighlight(PoseStack transform, MultiBufferSource bufferSource, Camera camera, BlockHitResult hit) {
        return CableHighlightRenderer.drawHighlight(transform, bufferSource, camera, hit)
            || MonitorHighlightRenderer.drawHighlight(transform, bufferSource, camera, hit);
    }

    public static boolean onRenderHeldItem(
        PoseStack transform, MultiBufferSource render, int lightTexture, InteractionHand hand,
        float pitch, float equipProgress, float swingProgress, ItemStack stack
    ) {
        if (stack.getItem() instanceof PocketComputerItem) {
            PocketItemRenderer.INSTANCE.renderItemFirstPerson(transform, render, lightTexture, hand, pitch, equipProgress, swingProgress, stack);
            return true;
        }
        if (stack.getItem() instanceof PrintoutItem) {
            PrintoutItemRenderer.INSTANCE.renderItemFirstPerson(transform, render, lightTexture, hand, pitch, equipProgress, swingProgress, stack);
            return true;
        }

        return false;
    }

    public static boolean onRenderItemFrame(PoseStack transform, MultiBufferSource render, ItemFrame frame, ItemStack stack, int light) {
        if (stack.getItem() instanceof PrintoutItem) {
            PrintoutItemRenderer.onRenderInFrame(transform, render, frame, stack, light);
            return true;
        }

        return false;
    }

    public static void onPlayStreaming(SoundEngine engine, Channel channel, AudioStream stream) {
        SpeakerManager.onPlayStreaming(engine, channel, stream);
    }

    /**
     * Add additional information about the currently targeted block to the debug screen.
     *
     * @param addText A callback which adds a single line of text.
     */
    public static void addBlockDebugInfo(Consumer<String> addText) {
        var minecraft = Minecraft.getInstance();
        if (!minecraft.options.renderDebug || minecraft.level == null) return;
        if (minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK) return;

        var tile = minecraft.level.getBlockEntity(((BlockHitResult) minecraft.hitResult).getBlockPos());

        if (tile instanceof MonitorBlockEntity monitor) {
            addText.accept("");
            addText.accept(
                String.format("Targeted monitor: (%d, %d), %d x %d", monitor.getXIndex(), monitor.getYIndex(), monitor.getWidth(), monitor.getHeight())
            );
        } else if (tile instanceof TurtleBlockEntity turtle) {
            addText.accept("");
            addText.accept("Targeted turtle:");
            addText.accept(String.format("Id: %d", turtle.getComputerID()));
            addTurtleUpgrade(addText, turtle, TurtleSide.LEFT);
            addTurtleUpgrade(addText, turtle, TurtleSide.RIGHT);
        }
    }

    private static void addTurtleUpgrade(Consumer<String> out, TurtleBlockEntity turtle, TurtleSide side) {
        var upgrade = turtle.getUpgrade(side);
        if (upgrade != null) out.accept(String.format("Upgrade[%s]: %s", side, upgrade.getUpgradeID()));
    }

    /**
     * Add additional information about the game to the debug screen.
     *
     * @param addText A callback which adds a single line of text.
     */
    public static void addGameDebugInfo(Consumer<String> addText) {
        if (MonitorBlockEntityRenderer.hasRenderedThisFrame() && Minecraft.getInstance().options.renderDebug) {
            addText.accept("[CC:T] Monitor renderer: " + MonitorBlockEntityRenderer.currentRenderer());
        }
    }

    public static @Nullable BlockState getBlockBreakingState(BlockState state, BlockPos pos) {
        // Only apply to cables which have both a cable and modem
        if (state.getBlock() != ModRegistry.Blocks.CABLE.get()
            || !state.getValue(CableBlock.CABLE)
            || state.getValue(CableBlock.MODEM) == CableModemVariant.None
        ) {
            return null;
        }

        var hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return null;
        var hitPos = ((BlockHitResult) hit).getBlockPos();

        if (!hitPos.equals(pos)) return null;

        return WorldUtil.isVecInside(CableShapes.getModemShape(state), hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()))
            ? state.getBlock().defaultBlockState().setValue(CableBlock.MODEM, state.getValue(CableBlock.MODEM))
            : state.setValue(CableBlock.MODEM, CableModemVariant.None);
    }
}
