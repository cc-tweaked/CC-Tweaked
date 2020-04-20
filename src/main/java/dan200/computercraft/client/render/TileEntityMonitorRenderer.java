/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

import static dan200.computercraft.shared.peripheral.monitor.TileMonitor.RENDER_MARGIN;

public class TileEntityMonitorRenderer extends TileEntitySpecialRenderer<TileMonitor>
{
    private static final float MARGIN = (float) (TileMonitor.RENDER_MARGIN * 1.1);

    @Override
    public void render( TileMonitor tileEntity, double posX, double posY, double posZ, float f, int i, float f2 )
    {
        if( tileEntity != null )
        {
            renderMonitorAt( tileEntity, posX, posY, posZ, f, i );
        }
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
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        // Set up render state for monitors. We disable writing to the depth buffer (we draw a "blocker" later),
        // and setup lighting so that we render with a glow.
        GlStateManager.depthMask( false );
        OpenGlHelper.setLightmapTextureCoords( OpenGlHelper.lightmapTexUnit, 0xFF, 0xFF );
        GlStateManager.disableLighting();
        mc.entityRenderer.disableLightmap();

        Terminal terminal = originTerminal.getTerminal();
        if( terminal != null )
        {
            boolean redraw = originTerminal.pollTerminalChanged();
            if( originTerminal.buffer == null )
            {
                originTerminal.createLists();
                redraw = true;
            }
            VertexBuffer vbo = originTerminal.buffer;

            // Draw a terminal
            double xScale = xSize / (terminal.getWidth() * FixedWidthFontRenderer.FONT_WIDTH);
            double yScale = ySize / (terminal.getHeight() * FixedWidthFontRenderer.FONT_HEIGHT);

            GlStateManager.pushMatrix();
            GlStateManager.scale( (float) xScale, (float) -yScale, 1.0f );

            float xMargin = (float) (MARGIN / xScale);
            float yMargin = (float) (MARGIN / yScale);

            if( redraw )
            {
                FixedWidthFontRenderer.begin( buffer );
                FixedWidthFontRenderer.drawTerminalWithoutCursor(
                    buffer, 0, 0,
                    terminal, !originTerminal.isColour(), yMargin, yMargin, xMargin, xMargin
                );

                buffer.finishDrawing();
                buffer.reset();
                vbo.bufferData( buffer.getByteBuffer() );
            }

            FixedWidthFontRenderer.bindFont();

            vbo.bindBuffer();
            setupBufferFormat();
            vbo.drawArrays( GL11.GL_TRIANGLES );
            vbo.unbindBuffer();

            // We don't draw the cursor with the VBO, as it's dynamic and so we'll end up refreshing far more than is
            // reasonable.
            FixedWidthFontRenderer.begin( buffer );
            FixedWidthFontRenderer.drawCursor( buffer, 0, 0, terminal, !originTerminal.isColour() );
            tessellator.draw();

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
