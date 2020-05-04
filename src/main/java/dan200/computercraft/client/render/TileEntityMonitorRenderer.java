/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.*;

public class TileEntityMonitorRenderer extends TileEntityRenderer<TileMonitor>
{
    /**
     * {@link TileMonitor#RENDER_MARGIN}, but a tiny bit of additional padding to ensure that there is no space between
     * the monitor frame and contents.
     */
    private static final float MARGIN = (float) (TileMonitor.RENDER_MARGIN * 1.1);

    private static final Matrix4f IDENTITY = TransformationMatrix.identity().getMatrix();

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
        BlockPos monitorPos = monitor.getPos();

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

        BlockPos originPos = origin.getPos();

        // Determine orientation
        Direction dir = origin.getDirection();
        Direction front = origin.getFront();
        float yaw = dir.getHorizontalAngle();
        float pitch = DirectionUtil.toPitchAngle( front );

        // Setup initial transform
        transform.push();
        transform.translate(
            originPos.getX() - monitorPos.getX() + 0.5,
            originPos.getY() - monitorPos.getY() + 0.5,
            originPos.getZ() - monitorPos.getZ() + 0.5
        );

        transform.rotate( Vector3f.YN.rotationDegrees( yaw ) );
        transform.rotate( Vector3f.XP.rotationDegrees( pitch ) );
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
            transform.push();
            transform.scale( (float) xScale, (float) -yScale, 1.0f );

            Matrix4f matrix = transform.getLast().getMatrix();

            // Sneaky hack here: we get a buffer now in order to flush existing ones and set up the appropriate
            // render state. I've no clue how well this'll work in future versions of Minecraft, but it does the trick
            // for now.
            IVertexBuilder buffer = renderer.getBuffer( FixedWidthFontRenderer.TYPE );
            FixedWidthFontRenderer.TYPE.setupRenderState();

            renderTerminal( matrix, originTerminal, (float) (MARGIN / xScale), (float) (MARGIN / yScale) );

            // We don't draw the cursor with the VBO, as it's dynamic and so we'll end up refreshing far more than is
            // reasonable.
            FixedWidthFontRenderer.drawCursor( matrix, buffer, 0, 0, terminal, !originTerminal.isColour() );

            transform.pop();
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal(
                transform.getLast().getMatrix(), renderer,
                -MARGIN, MARGIN,
                (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2)
            );
        }

        FixedWidthFontRenderer.drawBlocker(
            transform.getLast().getMatrix(), renderer,
            (float) -TileMonitor.RENDER_MARGIN, (float) TileMonitor.RENDER_MARGIN,
            (float) (xSize + 2 * TileMonitor.RENDER_MARGIN), (float) -(ySize + TileMonitor.RENDER_MARGIN * 2)
        );

        transform.pop();
    }

    private static void renderTerminal( Matrix4f matrix, ClientMonitor monitor, float xMargin, float yMargin )
    {
        Terminal terminal = monitor.getTerminal();

        MonitorRenderer renderType = MonitorRenderer.current();
        boolean redraw = monitor.pollTerminalChanged();
        if( monitor.createBuffer( renderType ) ) redraw = true;

        switch( renderType )
        {
            case VBO:
            {
                VertexBuffer vbo = monitor.buffer;
                if( redraw )
                {
                    Tessellator tessellator = Tessellator.getInstance();
                    BufferBuilder builder = tessellator.getBuffer();
                    builder.begin( FixedWidthFontRenderer.TYPE.getDrawMode(), FixedWidthFontRenderer.TYPE.getVertexFormat() );
                    FixedWidthFontRenderer.drawTerminalWithoutCursor(
                        IDENTITY, builder, 0, 0,
                        terminal, !monitor.isColour(), yMargin, yMargin, xMargin, xMargin
                    );

                    builder.finishDrawing();
                    vbo.upload( builder );
                }

                vbo.bindBuffer();
                FixedWidthFontRenderer.TYPE.getVertexFormat().setupBufferState( 0L );
                vbo.draw( matrix, FixedWidthFontRenderer.TYPE.getDrawMode() );
                VertexBuffer.unbindBuffer();
                FixedWidthFontRenderer.TYPE.getVertexFormat().clearBufferState();
                break;
            }

            case TBO:
            {
                if( !MonitorTextureBufferShader.use() ) return;

                int width = terminal.getWidth(), height = terminal.getHeight();
                int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;

                if( redraw )
                {
                    ByteBuffer buffer = GLAllocation.createDirectByteBuffer( width * height * 3 );
                    for( int y = 0; y < height; y++ )
                    {
                        TextBuffer text = terminal.getLine( y ), textColour = terminal.getTextColourLine( y ), background = terminal.getBackgroundColourLine( y );
                        for( int x = 0; x < width; x++ )
                        {
                            buffer.put( (byte) (text.charAt( x ) & 0xFF) );
                            buffer.put( (byte) getColour( textColour.charAt( x ), Colour.WHITE ) );
                            buffer.put( (byte) getColour( background.charAt( x ), Colour.BLACK ) );
                        }
                    }
                    buffer.flip();

                    GlStateManager.bindBuffer( GL31.GL_TEXTURE_BUFFER, monitor.tboBuffer );
                    GlStateManager.bufferData( GL31.GL_TEXTURE_BUFFER, buffer, GL20.GL_STATIC_DRAW );
                    GlStateManager.bindBuffer( GL31.GL_TEXTURE_BUFFER, 0 );
                }

                // Nobody knows what they're doing!
                GlStateManager.activeTexture( MonitorTextureBufferShader.TEXTURE_INDEX );
                GL11.glBindTexture( GL31.GL_TEXTURE_BUFFER, monitor.tboTexture );
                GlStateManager.activeTexture( GL13.GL_TEXTURE0 );

                MonitorTextureBufferShader.setupUniform( matrix, width, height, terminal.getPalette(), !monitor.isColour() );

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin( GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION );
                buffer.pos( -xMargin, -yMargin, 0 ).endVertex();
                buffer.pos( -xMargin, pixelHeight + yMargin, 0 ).endVertex();
                buffer.pos( pixelWidth + xMargin, -yMargin, 0 ).endVertex();
                buffer.pos( pixelWidth + xMargin, pixelHeight + yMargin, 0 ).endVertex();
                tessellator.draw();

                GlStateManager.useProgram( 0 );
                break;
            }
        }
    }
}
