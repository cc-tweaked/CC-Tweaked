/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nonnull;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.*;

public class TileEntityMonitorRenderer implements BlockEntityRenderer<TileMonitor>
{ //FIXME get rid of GL Calls. Buffered things or whatever, more research needed.
    /**
     * {@link TileMonitor#RENDER_MARGIN}, but a tiny bit of additional padding to ensure that there is no space between the monitor frame and contents.
     */
    private static final float MARGIN = (float) (TileMonitor.RENDER_MARGIN * 1.1);
    private static final Matrix4f IDENTITY = AffineTransformation.identity()
        .getMatrix();
    private static ByteBuffer tboContents;

    public TileEntityMonitorRenderer( BlockEntityRendererFactory.Context context )
    {
//        super( rendererDispatcher );
    }
    @Override
    public void render( @Nonnull TileMonitor monitor, float partialTicks, @Nonnull MatrixStack transform, @Nonnull VertexConsumerProvider renderer,
                        int lightmapCoord, int overlayLight )
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
        float yaw = dir.asRotation();
        float pitch = DirectionUtil.toPitchAngle( front );

        // Setup initial transform
        transform.push();
        transform.translate( originPos.getX() - monitorPos.getX() + 0.5,
            originPos.getY() - monitorPos.getY() + 0.5,
            originPos.getZ() - monitorPos.getZ() + 0.5 );

        transform.multiply( Vec3f.NEGATIVE_Y.getDegreesQuaternion( yaw ) );
        transform.multiply( Vec3f.POSITIVE_X.getDegreesQuaternion( pitch ) );
        transform.translate( -0.5 + TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN,
            origin.getHeight() - 0.5 - (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN) + 0,
            0.50 );
        double xSize = origin.getWidth() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);
        double ySize = origin.getHeight() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);

        // Draw the background blocker
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin( VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE );
        FixedWidthFontRenderer.drawBlocker( transform.peek().getModel(),
            buffer,
            (float) -TileMonitor.RENDER_MARGIN,
            (float) TileMonitor.RENDER_MARGIN,
            (float) (xSize + 2 * TileMonitor.RENDER_MARGIN),
            (float) -(ySize + TileMonitor.RENDER_MARGIN * 2) );
//        buffer.end();
        Tessellator.getInstance().draw();
        // Set the contents slightly off the surface to prevent z-fighting
        transform.translate( 0.0, 0.0, 0.001 );

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

            Matrix4f matrix = transform.peek().getModel();

            // Sneaky hack here: we get a buffer now in order to flush existing ones and set up the appropriate
            // render state. I've no clue how well this'll work in future versions of Minecraft, but it does the trick
            // for now.
            buffer = Tessellator.getInstance().getBuffer();
            buffer.begin( VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR_TEXTURE );
//            FixedWidthFontRenderer.TYPE.startDrawing();

            renderTerminal( matrix, originTerminal, (float) (MARGIN / xScale), (float) (MARGIN / yScale) );

            // We don't draw the cursor with the VBO, as it's dynamic and so we'll end up refreshing far more than is
            // reasonable.
            FixedWidthFontRenderer.drawCursor( matrix, buffer, 0, 0, terminal, !originTerminal.isColour() );

            // To go along with sneaky hack above: make sure state changes are undone. I would have thought this would
            // happen automatically after these buffers are drawn, but chests will render weird around monitors without this.
//            buffer.end();
            Tessellator.getInstance().draw();

            transform.pop();
        }
        else
        {
            buffer = Tessellator.getInstance().getBuffer();
            buffer.begin( VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR_TEXTURE );
            FixedWidthFontRenderer.drawEmptyTerminal( transform.peek()
                    .getModel(),
                buffer,
                -MARGIN,
                MARGIN,
                (float) (xSize + 2 * MARGIN),
                (float) -(ySize + MARGIN * 2) );
//            buffer.end();
            Tessellator.getInstance().draw();
        }

        transform.pop();
    }

    private static void renderTerminal( Matrix4f matrix, ClientMonitor monitor, float xMargin, float yMargin )
    {
        Terminal terminal = monitor.getTerminal();

        MonitorRenderer renderType = MonitorRenderer.current();
        boolean redraw = monitor.pollTerminalChanged();
        if( monitor.createBuffer( renderType ) )
        {
            redraw = true;
        }

        switch( renderType )
        {
            case TBO:
            {
                if( !MonitorTextureBufferShader.use() )
                {
                    return;
                }

                int width = terminal.getWidth(), height = terminal.getHeight();
                int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;

                if( redraw )
                {
                    int size = width * height * 3;
                    if( tboContents == null || tboContents.capacity() < size )
                    {
                        tboContents = GlAllocationUtils.allocateByteBuffer( size );
                    }

                    ByteBuffer monitorBuffer = tboContents;
                    monitorBuffer.clear();
                    for( int y = 0; y < height; y++ )
                    {
                        TextBuffer text = terminal.getLine( y ), textColour = terminal.getTextColourLine( y ), background = terminal.getBackgroundColourLine( y );
                        for( int x = 0; x < width; x++ )
                        {
                            monitorBuffer.put( (byte) (text.charAt( x ) & 0xFF) );
                            monitorBuffer.put( (byte) getColour( textColour.charAt( x ), Colour.WHITE ) );
                            monitorBuffer.put( (byte) getColour( background.charAt( x ), Colour.BLACK ) );
                        }
                    }
                    monitorBuffer.flip();
                    GlStateManager._glBindBuffer( GL31.GL_TEXTURE_BUFFER, monitor.tboBuffer );
                    GlStateManager._glBufferData( GL31.GL_TEXTURE_BUFFER, monitorBuffer, GL20.GL_STATIC_DRAW );
                    GlStateManager._glBindBuffer( GL31.GL_TEXTURE_BUFFER, 0 );
                }

                // Nobody knows what they're doing!
                GlStateManager._activeTexture( MonitorTextureBufferShader.TEXTURE_INDEX );
                GL11.glBindTexture( GL31.GL_TEXTURE_BUFFER, monitor.tboTexture );
                GlStateManager._activeTexture( GL13.GL_TEXTURE0 );

                MonitorTextureBufferShader.setupUniform( matrix, width, height, terminal.getPalette(), !monitor.isColour() );

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();
                buffer.begin( VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION );
                buffer.vertex( -xMargin, -yMargin, 0 )
                    .next();
                buffer.vertex( -xMargin, pixelHeight + yMargin, 0 )
                    .next();
                buffer.vertex( pixelWidth + xMargin, -yMargin, 0 )
                    .next();
                buffer.vertex( pixelWidth + xMargin, pixelHeight + yMargin, 0 )
                    .next();
//                buffer.end();
                tessellator.draw();

                GlStateManager._glUseProgram( 0 );
                break;
            }

            case VBO:
                VertexBuffer vbo = monitor.buffer;
                if( redraw )
                {
                    Tessellator tessellator = Tessellator.getInstance();
                    BufferBuilder builder = tessellator.getBuffer();
                    builder.begin( VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE );
                    FixedWidthFontRenderer.drawTerminalWithoutCursor( IDENTITY,
                        builder,
                        0,
                        0,
                        terminal,
                        !monitor.isColour(),
                        yMargin,
                        yMargin,
                        xMargin,
                        xMargin );

                    builder.end();
                    vbo.upload( builder );
                }

                vbo.bind();
                VertexFormats.POSITION_COLOR_TEXTURE
                    .startDrawing();
//                vbo.draw( matrix, FixedWidthFontRenderer.TYPE.getDrawMode() );
                vbo.drawElements(); //FIXME: Is this ok?
                VertexBuffer.unbind();
                VertexFormats.POSITION_COLOR_TEXTURE
                    .endDrawing();
                break;
        }
    }
}
