// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render.monitor;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.integration.ShaderMod;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

public class MonitorBlockEntityRenderer implements BlockEntityRenderer<MonitorBlockEntity, MonitorBlockEntityRenderer.State> {
    /**
     * {@link MonitorBlockEntity#RENDER_MARGIN}, but a tiny bit of additional padding to ensure that there is no space between
     * the monitor frame and contents.
     */
    private static final float MARGIN = (float) (MonitorBlockEntity.RENDER_MARGIN * 1.1);

    private static @Nullable ByteBuffer backingBuffer;

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
            var yMargin = (float) (MARGIN / yScale);

            collector.submitCustomGeometry(transform, FixedWidthFontRenderer.TERMINAL_TEXT, (pose, buffer) -> {
                FixedWidthFontRenderer.drawTerminalBackground(pose.pose(), buffer, 0, 0, terminal, yMargin, yMargin, xMargin, xMargin);
            });
            collector.submitCustomGeometry(transform, FixedWidthFontRenderer.TERMINAL_TEXT_OFFSET, (pose, buffer) -> {
                FixedWidthFontRenderer.drawTerminalForeground(pose.pose(), buffer, 0, 0, terminal);
                FixedWidthFontRenderer.drawCursor(pose.pose(), buffer, 0, 0, terminal);
            });

            transform.popPose();
        } else {
            FixedWidthFontRenderer.drawEmptyTerminal(transform, collector, -MARGIN, MARGIN, (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2));
        }

        transform.popPose();
    }

    private static void renderTerminal(
        Matrix4f matrix, ClientMonitor monitor, MonitorRenderState renderState, Terminal terminal, float xMargin, float yMargin
    ) {
        var redraw = monitor.pollTerminalChanged();
        if (renderState.vertexBuffer == null) redraw = true;

        if (redraw) {
            // Cursor, Foreground, Background+Margin
            var maxQuadCount = 1 + (terminal.getWidth() * terminal.getHeight()) + ((terminal.getWidth() + 2) * (terminal.getHeight() + 2));
            var maxVertexCount = 4 * maxQuadCount;
            var sink = ShaderMod.get().getQuadEmitter(maxQuadCount, MonitorBlockEntityRenderer::getBuffer);

            DirectFixedWidthFontRenderer.drawTerminalBackground(sink, 0, 0, terminal, yMargin, yMargin, xMargin, xMargin);
            var vertexCountAfterBackground = sink.vertexCount();

            DirectFixedWidthFontRenderer.drawTerminalForeground(sink, 0, 0, terminal);
            var vertexCountAfterForeground = sink.vertexCount();

            DirectFixedWidthFontRenderer.drawCursor(sink, 0, 0, terminal);
            var vertexCountAfterCursor = sink.vertexCount();

            if (vertexCountAfterCursor > maxVertexCount) {
                throw new IllegalStateException("Drew too many vertices. Expected " + maxVertexCount + ", drew " + vertexCountAfterCursor);
            }

            if (vertexCountAfterCursor != 0) {
                renderState.register();

                var commandEncoder = RenderSystem.getDevice().createCommandEncoder();

                var resultBuffer = sink.byteBuffer().flip();

                // Ensure our buffer contains the correct number of vertices.
                if (resultBuffer.remaining() != sink.format().getVertexSize() * vertexCountAfterCursor) {
                    throw new IllegalStateException(String.format(
                        "Mismatched vertex count. Buffer is %d bytes long, but was expected to be %d (vertex size) * %d (vertex count) = %d bytes.",
                        resultBuffer.limit(), sink.format().getVertexSize(), vertexCountAfterCursor, sink.format().getVertexSize() * vertexCountAfterCursor
                    ));
                }

                // Upload the buffer, reallocating if required.
                if (renderState.vertexBuffer == null || resultBuffer.remaining() > renderState.vertexBuffer.size()) {
                    if (renderState.vertexBuffer != null) {
                        renderState.vertexBuffer.close();
                        renderState.vertexBuffer = null;
                    }
                    renderState.vertexBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "Monitor at " + monitor.getOrigin().getBlockPos(), GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, resultBuffer
                    );
                } else if (!renderState.vertexBuffer.isClosed()) {
                    commandEncoder.writeToBuffer(renderState.vertexBuffer.slice(), resultBuffer);
                }
            }

            renderState.vertexCountAfterBackground = vertexCountAfterBackground;
            renderState.vertexCountAfterForeground = vertexCountAfterForeground;
            renderState.vertexCountAfterCursor = vertexCountAfterCursor;
        }

        if (renderState.vertexCountAfterCursor == 0) return;

        // Our VBO renders coordinates in monitor-space rather than world space. A full sized monitor (8x6) will
        // use positions from (0, 0) to (164*FONT_WIDTH, 81*FONT_HEIGHT) = (984, 729). This is far outside the
        // normal render distance (~200), and the edges of the monitor fade out due to fog.
        // There's not really a good way around this, at least without using a custom render type (which the VBO
        // renderer is trying to avoid!). Instead, we just disable fog entirely by setting the fog start to an
        // absurdly high value.
        var oldFog = Nullability.assertNonNull(RenderSystem.getShaderFog());
        RenderSystem.setShaderFog(Minecraft.getInstance().gameRenderer.fogRenderer.getBuffer(FogRenderer.FogMode.NONE));

        // Compose the existing model view matrix with our transformation matrix.
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().mul(matrix);

        // Render background geometry
        drawWithShader(renderState, FixedWidthFontRenderer.TERMINAL_TEXT, RenderPipelines.TEXT, 0, renderState.vertexCountAfterBackground);
        drawWithShader(
            renderState, FixedWidthFontRenderer.TERMINAL_TEXT_OFFSET, RenderPipelines.TEXT_POLYGON_OFFSET, renderState.vertexCountAfterBackground,
            (
                FixedWidthFontRenderer.isCursorVisible(terminal) && FrameInfo.getGlobalCursorBlink()
                    ? renderState.vertexCountAfterCursor : renderState.vertexCountAfterForeground
            ) - renderState.vertexCountAfterBackground
        );

        // Clear state
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.setShaderFog(oldFog);
    }

    private static void drawWithShader(MonitorRenderState renderState, RenderType renderType, RenderPipeline pipeline, int vertexOffset, int vertexCount) {
        if (renderState.vertexBuffer == null) {
            throw new IllegalStateException("MonitorRenderState has not been initialised");
        }
        if (vertexCount == 0) return;

        var transforms = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrix(),
            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            new Vector3f(),
            new Matrix4f()
        );

        var autoStorageBuffer = RenderSystem.getSequentialBuffer(renderType.mode());
        var indexCount = FixedWidthFontRenderer.TERMINAL_TEXT.mode().indexCount(vertexCount);
        var indexBuffer = autoStorageBuffer.getBuffer(indexCount);

        var target = Minecraft.getInstance().getMainRenderTarget();
        var colourTarget = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : target.getColorTextureView();
        var depthTarget = target.useDepth
            ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView())
            : null;

        try (var renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "Monitor", Nullability.assertNonNull(colourTarget), OptionalInt.empty(), depthTarget, OptionalDouble.empty()
        )) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", transforms);
            renderPass.setVertexBuffer(0, renderState.vertexBuffer);
            renderPass.setIndexBuffer(indexBuffer, autoStorageBuffer.type());

            /*
            for (var j = 0; j < 12; j++) {
                var gpuTexture = RenderSystem.getShaderTexture(j);
                if (gpuTexture != null) renderPass.bindTexture("Sampler" + j, gpuTexture);
            }
            */

            renderPass.drawIndexed(vertexOffset, 0, indexCount, 1);
        }
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

    private static ByteBuffer getBuffer(int capacity) {
        var buffer = backingBuffer;
        if (buffer == null || buffer.capacity() < capacity) {
            buffer = backingBuffer = buffer == null ? MemoryUtil.memAlloc(capacity) : MemoryUtil.memRealloc(buffer, capacity);
        }

        buffer.clear();
        return buffer;
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
