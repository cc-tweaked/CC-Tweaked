// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.client.render.monitor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dan200.computercraft.annotations.ForgeOverride;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.integration.ShaderMod;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorBlockEntity;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
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

    private static final ByteBufferBuilder backingBufferBuilder = new ByteBufferBuilder(0x4000);

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
                FixedWidthFontRenderer.toVertexConsumer(transform, bufferSource.getBuffer(FixedWidthFontRenderer.TERMINAL_TEXT)),
                -MARGIN, MARGIN,
                (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2)
            );
        }

        transform.popPose();
    }

    private static void renderTerminal(
        Matrix4f matrix, ClientMonitor monitor, MonitorRenderState renderState, Terminal terminal, float xMargin, float yMargin
    ) {
        var redraw = monitor.pollTerminalChanged();
        if (renderState.createBuffer()) redraw = true;

        var backgroundBuffer = assertNonNull(renderState.backgroundBuffer);
        var foregroundBuffer = assertNonNull(renderState.foregroundBuffer);
        if (redraw) {
            var size = DirectFixedWidthFontRenderer.getVertexCount(terminal);

            // In an ideal world we could upload these both into one buffer. However, we can't render VBOs with
            // and starting and ending offset, and so need to use two buffers instead.

            renderToBuffer(backgroundBuffer, size, sink ->
                DirectFixedWidthFontRenderer.drawTerminalBackground(sink, 0, 0, terminal, yMargin, yMargin, xMargin, xMargin));

            renderToBuffer(foregroundBuffer, size + 4, sink -> {
                DirectFixedWidthFontRenderer.drawTerminalForeground(sink, 0, 0, terminal);
                // If the cursor is visible, we append it to the end of our buffer. When rendering, we can either
                // render n or n+1 quads and so toggle the cursor on and off.
                DirectFixedWidthFontRenderer.drawCursor(sink, 0, 0, terminal);
            });
        }

        // Our VBO renders coordinates in monitor-space rather than world space. A full sized monitor (8x6) will
        // use positions from (0, 0) to (164*FONT_WIDTH, 81*FONT_HEIGHT) = (984, 729). This is far outside the
        // normal render distance (~200), and the edges of the monitor fade out due to fog.
        // There's not really a good way around this, at least without using a custom render type (which the VBO
        // renderer is trying to avoid!). Instead, we just disable fog entirely by setting the fog start to an
        // absurdly high value.
        var oldFog = RenderSystem.getShaderFog();
        RenderSystem.setShaderFog(FogParameters.NO_FOG);

        FixedWidthFontRenderer.TERMINAL_TEXT.setupRenderState();

        // Compose the existing model view matrix with our transformation matrix.
        var modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrix);

        // Render background geometry
        backgroundBuffer.bind();
        backgroundBuffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());

        // Render foreground geometry with glPolygonOffset enabled.
        RenderSystem.polygonOffset(-1.0f, -10.0f);
        RenderSystem.enablePolygonOffset();

        foregroundBuffer.bind();
        drawWithShader(
            foregroundBuffer, modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader(),
            // As mentioned in the above comment, render the extra cursor quad if it is visible this frame.
            FixedWidthFontRenderer.isCursorVisible(terminal) && FrameInfo.getGlobalCursorBlink()
                ? foregroundBuffer.indexCount
                : foregroundBuffer.indexCount - FixedWidthFontRenderer.TERMINAL_TEXT.mode().indexCount(4)
        );

        // Clear state
        RenderSystem.polygonOffset(0.0f, -0.0f);
        RenderSystem.disablePolygonOffset();
        FixedWidthFontRenderer.TERMINAL_TEXT.clearRenderState();
        VertexBuffer.unbind();

        RenderSystem.setShaderFog(oldFog);
    }

    private static void renderToBuffer(VertexBuffer vbo, int size, Consumer<DirectFixedWidthFontRenderer.QuadEmitter> draw) {
        var sink = ShaderMod.get().getQuadEmitter(size, backingBufferBuilder);
        draw.accept(sink);

        var result = backingBufferBuilder.build();
        if (result == null) {
            // If we have nothing to draw, just mark it as empty. We'll skip drawing in drawWithShader.
            vbo.indexCount = 0;
            return;
        }

        var buffer = result.byteBuffer();
        var vertices = buffer.limit() / sink.format().getVertexSize();

        vbo.bind();
        vbo.upload(new MeshData(result, new MeshData.DrawState(
            sink.format(),
            vertices, FixedWidthFontRenderer.TERMINAL_TEXT.mode().indexCount(vertices),
            FixedWidthFontRenderer.TERMINAL_TEXT.mode(), VertexFormat.IndexType.least(vertices)
        )));
    }

    private static void drawWithShader(VertexBuffer buffer, Matrix4f modelView, Matrix4f projection, @Nullable CompiledShaderProgram compiledShaderProgram, int indicies) {
        var originalIndexCount = buffer.indexCount;
        if (originalIndexCount == 0) return;

        try {
            buffer.indexCount = indicies;
            buffer.drawWithShader(modelView, projection, compiledShaderProgram);
        } finally {
            buffer.indexCount = originalIndexCount;
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
}
