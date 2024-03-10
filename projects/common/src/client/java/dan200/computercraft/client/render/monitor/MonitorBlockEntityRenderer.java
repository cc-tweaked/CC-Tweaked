// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render.monitor;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.integration.ShaderMod;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.client.render.vbo.DirectBuffers;
import dan200.computercraft.client.render.vbo.DirectVertexBuffer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;
import static dan200.computercraft.core.util.Nullability.assertNonNull;

public class MonitorBlockEntityRenderer implements BlockEntityRenderer<MonitorBlockEntity> {
    /**
     * {@link MonitorBlockEntity#RENDER_MARGIN}, but a tiny bit of additional padding to ensure that there is no space between
     * the monitor frame and contents.
     */
    private static final float MARGIN = (float) (MonitorBlockEntity.RENDER_MARGIN * 1.1);

    private static final Matrix3f IDENTITY_NORMAL = new Matrix3f().identity();

    private static @Nullable ByteBuffer backingBuffer;

    private static long lastFrame = -1;

    public MonitorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MonitorBlockEntity monitor, float partialTicks, PoseStack transform, MultiBufferSource bufferSource, int lightmapCoord, int overlayLight) {
        // Render from the origin monitor
        var originTerminal = monitor.getOriginClientMonitor();
        if (originTerminal == null) return;

        var origin = originTerminal.getOrigin();
        var renderState = originTerminal.getRenderState(MonitorRenderState::new);
        var monitorPos = monitor.getBlockPos();

        // Ensure each monitor terminal is rendered only once. We allow rendering a specific tile
        // multiple times in a single frame to ensure compatibility with shaders which may run a
        // pass multiple times.
        var renderFrame = FrameInfo.getRenderFrame();
        if (renderState.lastRenderFrame == renderFrame && !monitorPos.equals(renderState.lastRenderPos)) {
            return;
        }

        lastFrame = renderFrame;
        renderState.lastRenderFrame = renderFrame;
        renderState.lastRenderPos = monitorPos;

        var originPos = origin.getBlockPos();

        // Determine orientation
        var dir = origin.getDirection();
        var front = origin.getFront();
        var yaw = dir.toYRot();
        var pitch = DirectionUtil.toPitchAngle(front);

        // Setup initial transform
        transform.pushPose();
        transform.translate(
            originPos.getX() - monitorPos.getX() + 0.5,
            originPos.getY() - monitorPos.getY() + 0.5,
            originPos.getZ() - monitorPos.getZ() + 0.5
        );

        transform.mulPose(Axis.YN.rotationDegrees(yaw));
        transform.mulPose(Axis.XP.rotationDegrees(pitch));
        transform.translate(
            -0.5 + MonitorBlockEntity.RENDER_BORDER + MonitorBlockEntity.RENDER_MARGIN,
            origin.getHeight() - 0.5 - (MonitorBlockEntity.RENDER_BORDER + MonitorBlockEntity.RENDER_MARGIN) + 0,
            0.5
        );
        var xSize = origin.getWidth() - 2.0 * (MonitorBlockEntity.RENDER_MARGIN + MonitorBlockEntity.RENDER_BORDER);
        var ySize = origin.getHeight() - 2.0 * (MonitorBlockEntity.RENDER_MARGIN + MonitorBlockEntity.RENDER_BORDER);

        // Draw the contents
        var terminal = originTerminal.getTerminal();
        if (terminal != null && !ShaderMod.get().isRenderingShadowPass()) {
            // Draw a terminal
            int width = terminal.getWidth(), height = terminal.getHeight();
            int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;
            var xScale = xSize / pixelWidth;
            var yScale = ySize / pixelHeight;
            transform.pushPose();
            transform.scale((float) xScale, (float) -yScale, 1.0f);

            var matrix = transform.last().pose();

            renderTerminal(matrix, originTerminal, renderState, terminal, (float) (MARGIN / xScale), (float) (MARGIN / yScale));

            transform.popPose();
        } else {
            FixedWidthFontRenderer.drawEmptyTerminal(
                FixedWidthFontRenderer.toVertexConsumer(transform, bufferSource.getBuffer(RenderTypes.TERMINAL)),
                -MARGIN, MARGIN,
                (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2)
            );
        }

        transform.popPose();
    }

    private static void renderTerminal(
        Matrix4f matrix, ClientMonitor monitor, MonitorRenderState renderState, Terminal terminal, float xMargin, float yMargin
    ) {
        int width = terminal.getWidth(), height = terminal.getHeight();
        int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;

        var renderType = currentRenderer();
        var redraw = monitor.pollTerminalChanged();
        if (renderState.createBuffer(renderType)) redraw = true;

        switch (renderType) {
            case TBO -> {
                if (redraw) {
                    var terminalBuffer = getBuffer(width * height * 3);
                    MonitorTextureBufferShader.setTerminalData(terminalBuffer, terminal);
                    DirectBuffers.setBufferData(GL31.GL_TEXTURE_BUFFER, renderState.tboBuffer, terminalBuffer, GL20.GL_STATIC_DRAW);

                    var uniformBuffer = getBuffer(MonitorTextureBufferShader.UNIFORM_SIZE);
                    MonitorTextureBufferShader.setUniformData(uniformBuffer, terminal);
                    DirectBuffers.setBufferData(GL31.GL_UNIFORM_BUFFER, renderState.tboUniform, uniformBuffer, GL20.GL_STATIC_DRAW);
                }

                // Nobody knows what they're doing!
                var active = GlStateManager._getActiveTexture();
                RenderSystem.activeTexture(MonitorTextureBufferShader.TEXTURE_INDEX);
                GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, renderState.tboTexture);
                RenderSystem.activeTexture(active);

                var shader = RenderTypes.getMonitorTextureBufferShader();
                shader.setupUniform(renderState.tboUniform);

                var buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(RenderTypes.MONITOR_TBO.mode(), RenderTypes.MONITOR_TBO.format());
                tboVertex(buffer, matrix, -xMargin, -yMargin);
                tboVertex(buffer, matrix, -xMargin, pixelHeight + yMargin);
                tboVertex(buffer, matrix, pixelWidth + xMargin, -yMargin);
                tboVertex(buffer, matrix, pixelWidth + xMargin, pixelHeight + yMargin);
                RenderTypes.MONITOR_TBO.end(buffer, VertexSorting.DISTANCE_TO_ORIGIN);
            }
            case VBO -> {
                var backgroundBuffer = assertNonNull(renderState.backgroundBuffer);
                var foregroundBuffer = assertNonNull(renderState.foregroundBuffer);
                if (redraw) {
                    var size = DirectFixedWidthFontRenderer.getVertexCount(terminal);

                    // In an ideal world we could upload these both into one buffer. However, we can't render VBOs with
                    // and starting and ending offset, and so need to use two buffers instead.

                    renderToBuffer(backgroundBuffer, size, sink ->
                        DirectFixedWidthFontRenderer.drawTerminalBackground(sink, 0, 0, terminal, yMargin, yMargin, xMargin, xMargin));

                    renderToBuffer(foregroundBuffer, size, sink -> {
                        DirectFixedWidthFontRenderer.drawTerminalForeground(sink, 0, 0, terminal);
                        // If the cursor is visible, we append it to the end of our buffer. When rendering, we can either
                        // render n or n+1 quads and so toggle the cursor on and off.
                        DirectFixedWidthFontRenderer.drawCursor(sink, 0, 0, terminal);
                    });
                }

                // Our VBO doesn't transform its vertices with the provided pose stack, which means that the inverse view
                // rotation matrix gives entirely wrong numbers for fog distances. We just set it to the identity which
                // gives a good enough approximation.
                var oldInverseRotation = RenderSystem.getInverseViewRotationMatrix();
                RenderSystem.setInverseViewRotationMatrix(IDENTITY_NORMAL);

                RenderTypes.TERMINAL.setupRenderState();

                // Render background geometry
                backgroundBuffer.bind();
                backgroundBuffer.drawWithShader(matrix, RenderSystem.getProjectionMatrix(), RenderTypes.getTerminalShader());

                // Render foreground geometry with glPolygonOffset enabled.
                RenderSystem.polygonOffset(-1.0f, -10.0f);
                RenderSystem.enablePolygonOffset();

                foregroundBuffer.bind();
                foregroundBuffer.drawWithShader(
                    matrix, RenderSystem.getProjectionMatrix(), RenderTypes.getTerminalShader(),
                    // As mentioned in the above comment, render the extra cursor quad if it is visible this frame. Each
                    // // quad has an index count of 6.
                    FixedWidthFontRenderer.isCursorVisible(terminal) && FrameInfo.getGlobalCursorBlink()
                        ? foregroundBuffer.getIndexCount() + 6 : foregroundBuffer.getIndexCount()
                );

                // Clear state
                RenderSystem.polygonOffset(0.0f, -0.0f);
                RenderSystem.disablePolygonOffset();
                RenderTypes.TERMINAL.clearRenderState();
                VertexBuffer.unbind();

                RenderSystem.setInverseViewRotationMatrix(oldInverseRotation);
            }
            case BEST -> throw new IllegalStateException("Impossible: Should never use BEST renderer");
        }
    }

    private static void renderToBuffer(DirectVertexBuffer vbo, int size, Consumer<DirectFixedWidthFontRenderer.QuadEmitter> draw) {
        var sink = ShaderMod.get().getQuadEmitter(size, MonitorBlockEntityRenderer::getBuffer);
        var buffer = sink.buffer();

        draw.accept(sink);
        buffer.flip();
        vbo.upload(buffer.limit() / sink.format().getVertexSize(), RenderTypes.TERMINAL.mode(), sink.format(), buffer);
    }

    private static void tboVertex(VertexConsumer builder, Matrix4f matrix, float x, float y) {
        // We encode position in the UV, as that's not transformed by the matrix.
        builder.vertex(matrix, x, y, 0).uv(x, y).endVertex();
    }

    private static ByteBuffer getBuffer(int capacity) {
        var buffer = backingBuffer;
        if (buffer == null || buffer.capacity() < capacity) {
            buffer = backingBuffer = buffer == null ? MemoryTracker.create(capacity) : MemoryTracker.resize(buffer, capacity);
        }

        buffer.clear();
        return buffer;
    }

    @Override
    public int getViewDistance() {
        return Config.monitorDistance;
    }

    /**
     * Determine if any monitors were rendered this frame.
     *
     * @return Whether any monitors were rendered.
     */
    public static boolean hasRenderedThisFrame() {
        return FrameInfo.getRenderFrame() == lastFrame;
    }

    /**
     * Get the current renderer to use.
     *
     * @return The current renderer. Will not return {@link MonitorRenderer#BEST}.
     */
    public static MonitorRenderer currentRenderer() {
        var current = Config.monitorRenderer;
        if (current == MonitorRenderer.BEST) current = Config.monitorRenderer = bestRenderer();
        return current;
    }

    private static MonitorRenderer bestRenderer() {
        return MonitorRenderer.VBO;
    }
}
