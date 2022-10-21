/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.text.DirectFixedWidthFontRenderer;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.client.util.DirectBuffers;
import dan200.computercraft.client.util.DirectVertexBuffer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.FONT_WIDTH;

public class TileEntityMonitorRenderer extends TileEntityRenderer<TileMonitor>
{
    /**
     * {@link TileMonitor#RENDER_MARGIN}, but a tiny bit of additional padding to ensure that there is no space between
     * the monitor frame and contents.
     */
    private static final float MARGIN = (float) (TileMonitor.RENDER_MARGIN * 1.1);
    private static ByteBuffer backingBuffer;

    public TileEntityMonitorRenderer( TileEntityRendererDispatcher rendererDispatcher )
    {
        super( rendererDispatcher );
    }

    @Override
    public void render( @Nonnull TileMonitor monitor, float partialTicks, @Nonnull MatrixStack transform, @Nonnull IRenderTypeBuffer renderer, int lightmapCoord, int overlayLight )
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
            renderTerminal( renderer, matrix, originTerminal, (float) (MARGIN / xScale), (float) (MARGIN / yScale) );

            // Force a flush of the buffer. WorldRenderer.updateCameraAndRender will "finish" all the built-in buffers
            // before calling renderer.finish, which means the blocker isn't actually rendered at that point!
            renderer.getBuffer( RenderType.solid() );

            transform.popPose();
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal(
                transform.last().pose(), renderer.getBuffer( RenderTypes.TERMINAL_WITH_DEPTH ),
                -MARGIN, MARGIN,
                (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2)
            );
        }

        transform.popPose();
    }

    private static void renderTerminal( IRenderTypeBuffer bufferSource, Matrix4f matrix, ClientMonitor monitor, float xMargin, float yMargin )
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
                if( !MonitorTextureBufferShader.use() ) return;

                if( redraw )
                {
                    ByteBuffer terminalBuffer = getBuffer( width * height * 3 );
                    MonitorTextureBufferShader.setTerminalData( terminalBuffer, terminal );
                    DirectBuffers.setBufferData( GL31.GL_TEXTURE_BUFFER, monitor.tboBuffer, terminalBuffer, GL20.GL_STATIC_DRAW );

                    ByteBuffer uniformBuffer = getBuffer( MonitorTextureBufferShader.UNIFORM_SIZE );
                    MonitorTextureBufferShader.setUniformData( uniformBuffer, terminal );
                    DirectBuffers.setBufferData( GL31.GL_UNIFORM_BUFFER, monitor.tboUniform, uniformBuffer, GL20.GL_STATIC_DRAW );
                }

                // Sneaky hack here: we get a buffer now in order to flush existing ones and set up the appropriate
                // render state. I've no clue how well this'll work in future versions of Minecraft, but it does the trick
                // for now.
                bufferSource.getBuffer( RenderTypes.TERMINAL_WITH_DEPTH );
                RenderTypes.TERMINAL_WITH_DEPTH.setupRenderState();

                // Nobody knows what they're doing!
                GlStateManager._activeTexture( MonitorTextureBufferShader.TEXTURE_INDEX );
                GL11.glBindTexture( GL31.GL_TEXTURE_BUFFER, monitor.tboTexture );
                GlStateManager._activeTexture( GL13.GL_TEXTURE0 );

                MonitorTextureBufferShader.setupUniform( matrix, monitor.tboUniform );

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuilder();
                buffer.begin( GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION );
                buffer.vertex( -xMargin, -yMargin, 0 ).endVertex();
                buffer.vertex( -xMargin, pixelHeight + yMargin, 0 ).endVertex();
                buffer.vertex( pixelWidth + xMargin, -yMargin, 0 ).endVertex();
                buffer.vertex( pixelWidth + xMargin, pixelHeight + yMargin, 0 ).endVertex();
                tessellator.end();

                GlStateManager._glUseProgram( 0 );
                break;
            }

            case VBO:
            {
                DirectVertexBuffer vbo = monitor.buffer;
                if( redraw )
                {
                    int vertexSize = RenderTypes.TERMINAL_WITHOUT_DEPTH.format().getVertexSize();
                    ByteBuffer buffer = getBuffer( DirectFixedWidthFontRenderer.getVertexCount( terminal ) * vertexSize );

                    // Draw the main terminal and store how many vertices it has.
                    DirectFixedWidthFontRenderer.drawTerminalWithoutCursor(
                        buffer, 0, 0, terminal, yMargin, yMargin, xMargin, xMargin
                    );
                    int termIndexes = buffer.position() / vertexSize;

                    // If the cursor is visible, we append it to the end of our buffer. When rendering, we can either
                    // render n or n+1 quads and so toggle the cursor on and off.
                    DirectFixedWidthFontRenderer.drawCursor( buffer, 0, 0, terminal );

                    buffer.flip();

                    vbo.upload( termIndexes, RenderTypes.TERMINAL_WITHOUT_DEPTH.format(), buffer );
                }

                // As with the TBO backend we use getBuffer to flush existing buffers. This time we use TERMINAL_WITHOUT_DEPTH
                // instead and render a separate depth blocker.
                bufferSource.getBuffer( RenderTypes.TERMINAL_WITHOUT_DEPTH );
                RenderTypes.TERMINAL_WITHOUT_DEPTH.setupRenderState();

                vbo.draw(
                    matrix,
                    // As mentioned in the uploading block, render the extra cursor quad if it is visible this frame.
                    // Each quad has an index count of 6.
                    FixedWidthFontRenderer.isCursorVisible( terminal ) && FrameInfo.getGlobalCursorBlink() ? vbo.getIndexCount() + 6 : vbo.getIndexCount()
                );

                FixedWidthFontRenderer.drawBlocker(
                    matrix, bufferSource.getBuffer( RenderTypes.TERMINAL_BLOCKER ),
                    -xMargin, -yMargin, pixelWidth + xMargin * 2, pixelHeight + yMargin * 2
                );
                break;
            }
        }
    }

    @Nonnull
    private static ByteBuffer getBuffer( int capacity )
    {

        ByteBuffer buffer = backingBuffer;
        if( buffer == null || buffer.capacity() < capacity )
        {
            buffer = backingBuffer = buffer == null ? DirectBuffers.createByteBuffer( capacity ) : DirectBuffers.resizeByteBuffer( buffer, capacity );
        }

        buffer.clear();
        return buffer;
    }
}
