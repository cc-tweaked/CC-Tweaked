/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.client.util.DirectBuffers;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

public class TileEntityMonitorRenderer implements BlockEntityRenderer<TileMonitor>
{
    /**
     * {@link TileMonitor#RENDER_MARGIN}, but a tiny bit of additional padding to ensure that there is no space between
     * the monitor frame and contents.
     */
    private static final float MARGIN = (float) (TileMonitor.RENDER_MARGIN * 1.1);
    private static ByteBuffer backingBuffer;

    public TileEntityMonitorRenderer( BlockEntityRendererProvider.Context context )
    {
    }

    @Override
    public void render( @Nonnull TileMonitor monitor, float partialTicks, @Nonnull PoseStack transform, @Nonnull MultiBufferSource bufferSource, int lightmapCoord, int overlayLight )
    {
        // Render from the origin monitor
        ClientMonitor originTerminal = monitor.getClientMonitor();

        if( originTerminal == null ) return;
        TileMonitor origin = originTerminal.getOrigin();
        BlockPos monitorPos = monitor.getBlockPos();

        // Ensure each monitor terminal is rendered only once. We allow rendering a specific tile
        // multiple times in a single frame to ensure compatibility with shaders which may run a
        // pass multiple times.
        long renderFrame = FrameInfo.getRenderFrame();
        if( originTerminal.lastRenderFrame == renderFrame && !monitorPos.equals( originTerminal.lastRenderPos ) )
        {
            return;
        }

        originTerminal.lastRenderFrame = renderFrame;
        originTerminal.lastRenderPos = monitorPos;

        BlockPos originPos = origin.getBlockPos();

        // Determine orientation
        Direction dir = origin.getDirection();
        Direction front = origin.getFront();
        float yaw = dir.toYRot();
        float pitch = DirectionUtil.toPitchAngle( front );

        // Setup initial transform
        transform.pushPose();
        transform.translate(
            originPos.getX() - monitorPos.getX() + 0.5,
            originPos.getY() - monitorPos.getY() + 0.5,
            originPos.getZ() - monitorPos.getZ() + 0.5
        );

        transform.mulPose( Vector3f.YN.rotationDegrees( yaw ) );
        transform.mulPose( Vector3f.XP.rotationDegrees( pitch ) );
        transform.translate(
            -0.5 + TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN,
            origin.getHeight() - 0.5 - (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN) + 0,
            0.5
        );
        double xSize = origin.getWidth() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);
        double ySize = origin.getHeight() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);

        // Draw the contents
        Terminal terminal = originTerminal.getTerminal();
        if( terminal != null )
        {
            // Draw a terminal
            int width = terminal.getWidth(), height = terminal.getHeight();
            int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;
            double xScale = xSize / pixelWidth;
            double yScale = ySize / pixelHeight;
            transform.pushPose();
            transform.scale( (float) xScale, (float) -yScale, 1.0f );

            Matrix4f matrix = transform.last().pose();

            renderTerminal( matrix, originTerminal, (float) (MARGIN / xScale), (float) (MARGIN / yScale) );

            transform.popPose();
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal(
                FixedWidthFontRenderer.toVertexConsumer( transform.last().pose(), bufferSource.getBuffer( RenderTypes.TERMINAL_WITH_DEPTH ) ),
                -MARGIN, MARGIN,
                (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2)
            );
        }

        transform.popPose();
    }

    private static void renderTerminal( Matrix4f matrix, ClientMonitor monitor, float xMargin, float yMargin )
    {
        Terminal terminal = monitor.getTerminal();
        int width = terminal.getWidth(), height = terminal.getHeight();
        int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;

        MonitorRenderer renderType = MonitorRenderer.current();
        boolean redraw = monitor.pollTerminalChanged();
        if( monitor.createBuffer( renderType ) ) redraw = true;

        switch( renderType )
        {
            case TBO:
            {
                if( redraw )
                {
                    var terminalBuffer = getBuffer( width * height * 3 );
                    MonitorTextureBufferShader.setTerminalData( terminalBuffer, terminal );
                    DirectBuffers.setBufferData( GL31.GL_TEXTURE_BUFFER, monitor.tboBuffer, terminalBuffer, GL20.GL_STATIC_DRAW );

                    var uniformBuffer = getBuffer( MonitorTextureBufferShader.UNIFORM_SIZE );
                    MonitorTextureBufferShader.setUniformData( uniformBuffer, terminal, !monitor.isColour() );
                    DirectBuffers.setBufferData( GL31.GL_UNIFORM_BUFFER, monitor.tboUniform, uniformBuffer, GL20.GL_STATIC_DRAW );
                }

                // Nobody knows what they're doing!
                int active = GlStateManager._getActiveTexture();
                RenderSystem.activeTexture( MonitorTextureBufferShader.TEXTURE_INDEX );
                GL11.glBindTexture( GL31.GL_TEXTURE_BUFFER, monitor.tboTexture );
                RenderSystem.activeTexture( active );

                MonitorTextureBufferShader shader = RenderTypes.getMonitorTextureBufferShader();
                shader.setupUniform( monitor.tboUniform );

                BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                buffer.begin( RenderTypes.MONITOR_TBO.mode(), RenderTypes.MONITOR_TBO.format() );
                tboVertex( buffer, matrix, -xMargin, -yMargin );
                tboVertex( buffer, matrix, -xMargin, pixelHeight + yMargin );
                tboVertex( buffer, matrix, pixelWidth + xMargin, -yMargin );
                tboVertex( buffer, matrix, pixelWidth + xMargin, pixelHeight + yMargin );
                RenderTypes.MONITOR_TBO.end( buffer, 0, 0, 0 );

                break;
            }

            case VBO:
            {
                var vbo = monitor.buffer;
                if( redraw )
                {
                    int vertexSize = RenderTypes.TERMINAL_WITHOUT_DEPTH.format().getVertexSize();
                    ByteBuffer buffer = getBuffer( DirectFixedWidthFontRenderer.getVertexCount( terminal ) * vertexSize );

                    // Draw the main terminal and store how many vertices it has.
                    DirectFixedWidthFontRenderer.drawTerminalWithoutCursor(
                        buffer, 0, 0, terminal, !monitor.isColour(), yMargin, yMargin, xMargin, xMargin
                    );
                    int termIndexes = buffer.position() / vertexSize;

                    // If the cursor is visible, we append it to the end of our buffer. When rendering, we can either
                    // render n or n+1 quads and so toggle the cursor on and off.
                    DirectFixedWidthFontRenderer.drawCursor( buffer, 0, 0, terminal, !monitor.isColour() );

                    buffer.flip();

                    vbo.upload( termIndexes, RenderTypes.TERMINAL_WITHOUT_DEPTH.mode(), RenderTypes.TERMINAL_WITHOUT_DEPTH.format(), buffer );
                }

                RenderTypes.TERMINAL_WITHOUT_DEPTH.setupRenderState();
                vbo.drawWithShader(
                    matrix, RenderSystem.getProjectionMatrix(), RenderTypes.getTerminalShader(),
                    // As mentioned in the above comment, render the extra cursor quad if it is visible this frame. Each
                    // // quad has an index count of 6.
                    FixedWidthFontRenderer.isCursorVisible( terminal ) && FrameInfo.getGlobalCursorBlink() ? vbo.getIndexCount() + 6 : vbo.getIndexCount()
                );
                RenderTypes.TERMINAL_WITHOUT_DEPTH.clearRenderState();

                // TERMINAL_WITHOUT_DEPTH doesn't write to the depth blocker, so write a blocker over the monitor.
                BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                buffer.begin( RenderTypes.TERMINAL_BLOCKER.mode(), RenderTypes.TERMINAL_BLOCKER.format() );
                FixedWidthFontRenderer.drawBlocker(
                    FixedWidthFontRenderer.toVertexConsumer( matrix, buffer ),
                    -xMargin, -yMargin, pixelWidth + xMargin * 2, pixelHeight + yMargin * 2
                );
                RenderTypes.TERMINAL_BLOCKER.end( buffer, 0, 0, 0 );

                break;
            }
        }
    }

    private static void tboVertex( VertexConsumer builder, Matrix4f matrix, float x, float y )
    {
        // We encode position in the UV, as that's not transformed by the matrix.
        builder.vertex( matrix, x, y, 0 ).uv( x, y ).endVertex();
    }

    @Nonnull
    private static ByteBuffer getBuffer( int capacity )
    {

        ByteBuffer buffer = backingBuffer;
        if( buffer == null || buffer.capacity() < capacity )
        {
            buffer = backingBuffer = buffer == null ? MemoryTracker.create( capacity ) : MemoryTracker.resize( buffer, capacity );
        }

        buffer.clear();
        return buffer;
    }

    @Override
    public int getViewDistance()
    {
        return ComputerCraft.monitorDistance;
    }
}
