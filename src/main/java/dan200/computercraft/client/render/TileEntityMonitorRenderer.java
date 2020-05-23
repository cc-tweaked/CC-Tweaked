/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.DirectionUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.*;
import static dan200.computercraft.shared.peripheral.monitor.TileMonitor.RENDER_MARGIN;

public class TileEntityMonitorRenderer extends TileEntitySpecialRenderer<TileMonitor>
{
    private static final float MARGIN = (float) (TileMonitor.RENDER_MARGIN * 1.1);

    @Override
    public void render( @Nonnull TileMonitor tileEntity, double posX, double posY, double posZ, float f, int i, float f2 )
    {
        renderMonitorAt( tileEntity, posX, posY, posZ, f, i );
    }

    private static void renderMonitorAt( TileMonitor monitor, double posX, double posY, double posZ, float f, int i )
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
        posX += originPos.getX() - monitorPos.getX();
        posY += originPos.getY() - monitorPos.getY();
        posZ += originPos.getZ() - monitorPos.getZ();

        // Determine orientation
        EnumFacing dir = origin.getDirection();
        EnumFacing front = origin.getFront();
        float yaw = dir.getHorizontalAngle();
        float pitch = DirectionUtil.toPitchAngle( front );

        GlStateManager.pushMatrix();

        // Setup initial transform
        GlStateManager.translate( posX + 0.5, posY + 0.5, posZ + 0.5 );
        GlStateManager.rotate( -yaw, 0.0f, 1.0f, 0.0f );
        GlStateManager.rotate( pitch, 1.0f, 0.0f, 0.0f );
        GlStateManager.translate(
            -0.5 + TileMonitor.RENDER_BORDER + RENDER_MARGIN,
            origin.getHeight() - 0.5 - (TileMonitor.RENDER_BORDER + RENDER_MARGIN),
            0.5
        );
        double xSize = origin.getWidth() - 2.0 * (RENDER_MARGIN + TileMonitor.RENDER_BORDER);
        double ySize = origin.getHeight() - 2.0 * (RENDER_MARGIN + TileMonitor.RENDER_BORDER);

        // Get renderers
        Minecraft mc = Minecraft.getMinecraft();

        // Set up render state for monitors. We disable writing to the depth buffer (we draw a "blocker" later),
        // and setup lighting so that we render with a glow.
        GlStateManager.depthMask( false );
        OpenGlHelper.setLightmapTextureCoords( OpenGlHelper.lightmapTexUnit, 0xFF, 0xFF );
        GlStateManager.disableLighting();
        mc.entityRenderer.disableLightmap();

        Terminal terminal = originTerminal.getTerminal();
        if( terminal != null )
        {
            // Draw a terminal
            double xScale = xSize / (terminal.getWidth() * FONT_WIDTH);
            double yScale = ySize / (terminal.getHeight() * FONT_HEIGHT);

            GlStateManager.pushMatrix();
            GlStateManager.scale( (float) xScale, (float) -yScale, 1.0f );

            renderTerminal( originTerminal, (float) (MARGIN / xScale), (float) (MARGIN / yScale) );

            GlStateManager.popMatrix();
        }
        else
        {
            FixedWidthFontRenderer.drawEmptyTerminal(
                -MARGIN, MARGIN,
                (float) (xSize + 2 * MARGIN), (float) -(ySize + MARGIN * 2)
            );
        }

        // Tear down render state for monitors.
        GlStateManager.depthMask( true );
        mc.entityRenderer.enableLightmap();
        GlStateManager.enableLighting();

        // Draw the depth blocker
        GlStateManager.colorMask( false, false, false, false );
        FixedWidthFontRenderer.drawBlocker(
            (float) -TileMonitor.RENDER_MARGIN, (float) TileMonitor.RENDER_MARGIN,
            (float) (xSize + 2 * TileMonitor.RENDER_MARGIN), (float) -(ySize + TileMonitor.RENDER_MARGIN * 2)
        );
        GlStateManager.colorMask( true, true, true, true );

        GlStateManager.popMatrix();
    }

    private static void renderTerminal( ClientMonitor monitor, float xMargin, float yMargin )
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        boolean redraw = monitor.pollTerminalChanged();

        // Setup the buffers if needed. We get the renderer here, to avoid the (unlikely) race condition between
        // creating the buffers and rendering.
        MonitorRenderer renderer = MonitorRenderer.current();
        if( monitor.createBuffer( renderer ) ) redraw = true;

        FixedWidthFontRenderer.bindFont();

        switch( renderer )
        {
            case TBO:
            {
                if( !MonitorTextureBufferShader.use() ) return;

                Terminal terminal = monitor.getTerminal();
                int width = terminal.getWidth(), height = terminal.getHeight();
                int pixelWidth = width * FONT_WIDTH, pixelHeight = height * FONT_HEIGHT;

                if( redraw )
                {
                    GL15.glBindBuffer( GL31.GL_TEXTURE_BUFFER, monitor.tboBuffer );
                    ByteBuffer monitorBuffer;
                    boolean resize;

                    if( monitor.monitorBuffer == null || monitor.monitorBuffer.capacity() != width * height * 3 )
                    {
                        monitorBuffer = BufferUtils.createByteBuffer( width * height * 3 );
                        resize = true;
                    } else
                    {
                        monitorBuffer = GL15.glMapBuffer( GL31.GL_TEXTURE_BUFFER, GL15.GL_WRITE_ONLY, monitor.monitorBuffer );
                        resize = false;
                    }

                    monitor.monitorBuffer = monitorBuffer;

                    for( int y = 0; y < height; y++ )
                    {
                        TextBuffer text = terminal.getLine( y ), textColour = terminal.getTextColourLine( y ), background = terminal.getBackgroundColourLine( y );
                        for( int x = 0; x < width; x++ )
                        {
                            monitorBuffer.put( (byte) (text.charAt( x ) & 0xFF) );
                            monitorBuffer.put( (byte) getColour( textColour.charAt( x ), Colour.White ) );
                            monitorBuffer.put( (byte) getColour( background.charAt( x ), Colour.Black ) );
                        }
                    }
                    monitorBuffer.flip();

                    if( resize )
                    {
                        OpenGlHelper.glBufferData( GL31.GL_TEXTURE_BUFFER, monitorBuffer, GL15.GL_STATIC_DRAW );
                    } else
                    {
                        GL15.glUnmapBuffer( GL31.GL_TEXTURE_BUFFER );
                    }
                }

                // Bind TBO texture and set up the uniforms. We've already set up the main font above.
                GlStateManager.setActiveTexture( MonitorTextureBufferShader.TEXTURE_INDEX );
                GL11.glBindTexture( GL31.GL_TEXTURE_BUFFER, monitor.tboTexture );
                GlStateManager.setActiveTexture( GL13.GL_TEXTURE0 );

                MonitorTextureBufferShader.setupUniform( width, height, terminal.getPalette(), !monitor.isColour(), redraw );

                buffer.begin( GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION );
                buffer.pos( -xMargin, -yMargin, 0 ).endVertex();
                buffer.pos( -xMargin, pixelHeight + yMargin, 0 ).endVertex();
                buffer.pos( pixelWidth + xMargin, -yMargin, 0 ).endVertex();
                buffer.pos( pixelWidth + xMargin, pixelHeight + yMargin, 0 ).endVertex();
                tessellator.draw();

                OpenGlHelper.glUseProgram( 0 );
                break;
            }

            case VBO:
            {
                VertexBuffer vbo = monitor.buffer;
                if( redraw )
                {
                    renderTerminalTo( monitor, buffer, xMargin, yMargin );
                    buffer.finishDrawing();
                    buffer.reset();
                    vbo.bufferData( buffer.getByteBuffer() );
                }

                vbo.bindBuffer();
                setupBufferFormat();
                vbo.drawArrays( GL11.GL_TRIANGLES );
                vbo.unbindBuffer();

                break;
            }

            case DISPLAY_LIST:
                if( redraw )
                {
                    GlStateManager.glNewList( monitor.displayList, GL11.GL_COMPILE );
                    renderTerminalTo( monitor, buffer, xMargin, yMargin );
                    tessellator.draw();
                    GlStateManager.glEndList();
                }

                GlStateManager.callList( monitor.displayList );
                break;
        }

        // We don't draw the cursor with a buffer, as it's dynamic and so we'll end up refreshing far more than is
        // reasonable.
        FixedWidthFontRenderer.begin( buffer );
        FixedWidthFontRenderer.drawCursor( buffer, 0, 0, monitor.getTerminal(), !monitor.isColour() );
        tessellator.draw();
    }

    private static void renderTerminalTo( ClientMonitor monitor, BufferBuilder buffer, float xMargin, float yMargin )
    {
        FixedWidthFontRenderer.begin( buffer );
        FixedWidthFontRenderer.drawTerminalWithoutCursor(
            buffer, 0, 0,
            monitor.getTerminal(), !monitor.isColour(), yMargin, yMargin, xMargin, xMargin
        );
    }

    public static void setupBufferFormat()
    {
        int stride = FixedWidthFontRenderer.POSITION_COLOR_TEX.getSize();
        GlStateManager.glVertexPointer( 3, GL11.GL_FLOAT, stride, 0 );
        GlStateManager.glEnableClientState( GL11.GL_VERTEX_ARRAY );

        GlStateManager.glColorPointer( 4, GL11.GL_UNSIGNED_BYTE, stride, 12 );
        GlStateManager.glEnableClientState( GL11.GL_COLOR_ARRAY );

        GlStateManager.glTexCoordPointer( 2, GL11.GL_FLOAT, stride, 16 );
        GlStateManager.glEnableClientState( GL11.GL_TEXTURE_COORD_ARRAY );
    }
}
