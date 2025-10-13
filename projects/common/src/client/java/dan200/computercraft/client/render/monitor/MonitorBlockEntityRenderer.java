// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render.monitor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

public class MonitorBlockEntityRenderer implements BlockEntityRenderer<MonitorBlockEntity, MonitorBlockEntityRenderer.State> {
    /**
     * {@link MonitorBlockEntity#RENDER_MARGIN}, but a tiny bit of additional padding to ensure that there is no space between
     * the monitor frame and contents.
     */
    private static final float MARGIN = (float) (MonitorBlockEntity.RENDER_MARGIN * 1.1);

    public MonitorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MonitorBlockEntity monitor, State state, float f, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(monitor, state, f, camera, crumblingOverlay);

        state.direction = monitor.getDirection();
        state.front = monitor.getFront();
        state.width = monitor.getWidth();
        state.height = monitor.getHeight();
        state.terminal = monitor.getOriginClientMonitor();
    }

    @Override
    public void submit(State state, PoseStack transform, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.terminal == null) return;

        // Determine orientation
        var dir = state.direction;
        var front = state.front;
        var yaw = dir.toYRot();
        var pitch = DirectionUtil.toPitchAngle(front);

        // Setup initial transform
        transform.pushPose();
        transform.translate(0.5, 0.5, 0.5);

        transform.mulPose(Axis.YN.rotationDegrees(yaw));
        transform.mulPose(Axis.XP.rotationDegrees(pitch));
        transform.translate(
            -0.5 + MonitorBlockEntity.RENDER_BORDER + MonitorBlockEntity.RENDER_MARGIN,
            state.height - 0.5 - (MonitorBlockEntity.RENDER_BORDER + MonitorBlockEntity.RENDER_MARGIN) + 0,
            0.5
        );
        var xSize = state.width - 2.0 * (MonitorBlockEntity.RENDER_MARGIN + MonitorBlockEntity.RENDER_BORDER);
        var ySize = state.height - 2.0 * (MonitorBlockEntity.RENDER_MARGIN + MonitorBlockEntity.RENDER_BORDER);

        // Draw the contents
        var terminal = state.terminal.getTerminal();
        if (terminal != null) {
            // Draw a terminal
            int width = terminal.getWidth(), height = terminal.getHeight();
            int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;
            var xScale = xSize / pixelWidth;
            var yScale = ySize / pixelHeight;
            transform.pushPose();
            transform.scale((float) xScale, (float) -yScale, 1.0f);

            var xMargin = (float) (MARGIN / xScale);
            var yMagin = (float) (MARGIN / yScale);

            collector.submitCustomGeometry(transform, FixedWidthFontRenderer.TERMINAL_TEXT, (pose, consumer) -> {
                FixedWidthFontRenderer.drawTerminalBackground(
                    new FixedWidthFontRenderer.QuadEmitter(pose.pose(), consumer),
                    0, 0, terminal, yMagin, yMagin, xMargin, xMargin
                );
            });
            collector.submitCustomGeometry(transform, FixedWidthFontRenderer.TERMINAL_TEXT_OFFSET, (pose, consumer) -> {
                var sink = new FixedWidthFontRenderer.QuadEmitter(pose.pose(), consumer);
                FixedWidthFontRenderer.drawTerminalForeground(sink, 0, 0, terminal);
                FixedWidthFontRenderer.drawCursor(sink, 0, 0, terminal);
            });

            transform.popPose();
        } else {
            collector.submitCustomGeometry(transform, FixedWidthFontRenderer.TERMINAL_TEXT, (pose, consumer) -> {
                FixedWidthFontRenderer.drawEmptyTerminal(
                    new FixedWidthFontRenderer.QuadEmitter(pose.pose(), consumer),
                    -MARGIN, MARGIN, (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2)
                );
            });
        }

        transform.popPose();
    }

    @Override
    public int getViewDistance() {
        return Config.monitorDistance;
    }

    @ForgeOverride
    public AABB getRenderBoundingBox(MonitorBlockEntity monitor) {
        return monitor.getRenderBoundingBox();
    }

    @Override
    public boolean shouldRender(MonitorBlockEntity monitor, Vec3 camera) {
        return BlockEntityRenderer.super.shouldRender(monitor, camera) && monitor.getXIndex() == 0 && monitor.getYIndex() == 0;
    }

    public static final class State extends BlockEntityRenderState {
        private Direction direction = Direction.NORTH;
        private Direction front = Direction.NORTH;
        private int width;
        private int height;
        private @Nullable ClientMonitor terminal;

        private State() {
        }
    }
}
