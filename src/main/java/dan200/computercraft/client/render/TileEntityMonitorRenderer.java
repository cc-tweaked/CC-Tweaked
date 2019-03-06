/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.opengl.GL11;

public class TileEntityMonitorRenderer extends BlockEntityRenderer<TileMonitor>
{
    @Override
    public void render( TileMonitor tileEntity, double posX, double posY, double posZ, float f, int i )
    {
        if( tileEntity != null )
        {
            renderMonitorAt( tileEntity, posX, posY, posZ, f, i );
        }
    }

    private void renderMonitorAt( TileMonitor monitor, double posX, double posY, double posZ, float f, int i )
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
        Direction dir = origin.getDirection();
        Direction front = origin.getFront();
        float yaw = dir.asRotation();
        float pitch = DirectionUtil.toPitchAngle( front );

        GlStateManager.pushMatrix();
        try
        {
            // Setup initial transform
            GlStateManager.translated( posX + 0.5, posY + 0.5, posZ + 0.5 );
            GlStateManager.rotatef( -yaw, 0.0f, 1.0f, 0.0f );
            GlStateManager.rotatef( pitch, 1.0f, 0.0f, 0.0f );
            GlStateManager.translated(
                -0.5 + TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN,
                (origin.getHeight() - 0.5) - (TileMonitor.RENDER_BORDER + TileMonitor.RENDER_MARGIN) + 0,
                0.5
            );
            double xSize = origin.getWidth() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);
            double ySize = origin.getHeight() - 2.0 * (TileMonitor.RENDER_MARGIN + TileMonitor.RENDER_BORDER);

            // Get renderers
            MinecraftClient mc = MinecraftClient.getInstance();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder renderer = tessellator.getBufferBuilder();

            // Get terminal
            boolean redraw = originTerminal.pollTerminalChanged();

            // Draw the contents
            GlStateManager.depthMask( false );
            GLX.glMultiTexCoord2f( GLX.GL_TEXTURE1, 0xFFFF, 0xFFFF );
            // TODO: ^ OpenGlHelper.setLightmapTextureCoords( OpenGlHelper.lightmapTexUnit, 0xFF, 0xFF ); ??
            GlStateManager.disableLighting();
            mc.gameRenderer.disableLightmap();
            try
            {
                Terminal terminal = originTerminal.getTerminal();
                if( terminal != null )
                {
                    Palette palette = terminal.getPalette();

                    // Allocate display lists
                    if( originTerminal.renderDisplayLists == null )
                    {
                        originTerminal.createLists();
                        redraw = true;
                    }

                    // Draw a terminal
                    boolean greyscale = !originTerminal.isColour();
                    int width = terminal.getWidth();
                    int height = terminal.getHeight();
                    int cursorX = terminal.getCursorX();
                    int cursorY = terminal.getCursorY();
                    FixedWidthFontRenderer fontRenderer = FixedWidthFontRenderer.instance();

                    GlStateManager.pushMatrix();
                    try
                    {
                        double xScale = xSize / (width * FixedWidthFontRenderer.FONT_WIDTH);
                        double yScale = ySize / (height * FixedWidthFontRenderer.FONT_HEIGHT);
                        GlStateManager.scaled( xScale, -yScale, 1.0 );

                        // Draw background
                        mc.getTextureManager().bindTexture( FixedWidthFontRenderer.BACKGROUND );
                        if( redraw )
                        {
                            // Build background display list
                            GlStateManager.newList( originTerminal.renderDisplayLists[0], GL11.GL_COMPILE );
                            try
                            {
                                double marginXSize = TileMonitor.RENDER_MARGIN / xScale;
                                double marginYSize = TileMonitor.RENDER_MARGIN / yScale;
                                double marginSquash = marginYSize / FixedWidthFontRenderer.FONT_HEIGHT;

                                // Top and bottom margins
                                GlStateManager.pushMatrix();
                                try
                                {
                                    GlStateManager.scaled( 1.0, marginSquash, 1.0 );
                                    GlStateManager.translated( 0.0, -marginYSize / marginSquash, 0.0 );
                                    fontRenderer.drawStringBackgroundPart( 0, 0, terminal.getBackgroundColourLine( 0 ), marginXSize, marginXSize, greyscale, palette );
                                    GlStateManager.translated( 0.0, (marginYSize + height * FixedWidthFontRenderer.FONT_HEIGHT) / marginSquash, 0.0 );
                                    fontRenderer.drawStringBackgroundPart( 0, 0, terminal.getBackgroundColourLine( height - 1 ), marginXSize, marginXSize, greyscale, palette );
                                }
                                finally
                                {
                                    GlStateManager.popMatrix();
                                }

                                // Backgrounds
                                for( int y = 0; y < height; y++ )
                                {
                                    fontRenderer.drawStringBackgroundPart(
                                        0, FixedWidthFontRenderer.FONT_HEIGHT * y,
                                        terminal.getBackgroundColourLine( y ),
                                        marginXSize, marginXSize,
                                        greyscale,
                                        palette
                                    );
                                }
                            }
                            finally
                            {
                                GlStateManager.endList();
                            }
                        }
                        GlStateManager.callList( originTerminal.renderDisplayLists[0] );
                        GlStateManager.clearCurrentColor();

                        // Draw text
                        fontRenderer.bindFont();
                        if( redraw )
                        {
                            // Build text display list
                            GlStateManager.newList( originTerminal.renderDisplayLists[1], GL11.GL_COMPILE );
                            try
                            {
                                // Lines
                                for( int y = 0; y < height; y++ )
                                {
                                    fontRenderer.drawStringTextPart(
                                        0, FixedWidthFontRenderer.FONT_HEIGHT * y,
                                        terminal.getLine( y ),
                                        terminal.getTextColourLine( y ),
                                        greyscale,
                                        palette
                                    );
                                }
                            }
                            finally
                            {
                                GlStateManager.endList();
                            }
                        }
                        GlStateManager.callList( originTerminal.renderDisplayLists[1] );
                        GlStateManager.clearCurrentColor();

                        // Draw cursor
                        fontRenderer.bindFont();
                        if( redraw )
                        {
                            // Build cursor display list
                            GlStateManager.newList( originTerminal.renderDisplayLists[2], GL11.GL_COMPILE );
                            try
                            {
                                // Cursor
                                if( terminal.getCursorBlink() && cursorX >= 0 && cursorX < width && cursorY >= 0 && cursorY < height )
                                {
                                    TextBuffer cursor = new TextBuffer( "_" );
                                    TextBuffer cursorColour = new TextBuffer( "0123456789abcdef".charAt( terminal.getTextColour() ), 1 );
                                    fontRenderer.drawString(
                                        cursor,
                                        FixedWidthFontRenderer.FONT_WIDTH * cursorX,
                                        FixedWidthFontRenderer.FONT_HEIGHT * cursorY,
                                        cursorColour, null,
                                        0, 0,
                                        greyscale,
                                        palette
                                    );
                                }
                            }
                            finally
                            {
                                GlStateManager.endList();
                            }
                        }
                        if( FrameInfo.getGlobalCursorBlink() )
                        {
                            GlStateManager.callList( originTerminal.renderDisplayLists[2] );
                            GlStateManager.clearCurrentColor();
                        }
                    }
                    finally
                    {
                        GlStateManager.popMatrix();
                    }
                }
                else
                {
                    // Draw a big black quad
                    mc.getTextureManager().bindTexture( FixedWidthFontRenderer.BACKGROUND );
                    final Colour colour = Colour.Black;

                    final float r = colour.getR();
                    final float g = colour.getG();
                    final float b = colour.getB();

                    renderer.begin( GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION_UV_COLOR );
                    renderer.vertex( -TileMonitor.RENDER_MARGIN, TileMonitor.RENDER_MARGIN, 0.0D ).texture( 0.0, 0.0 ).color( r, g, b, 1.0f ).next();
                    renderer.vertex( -TileMonitor.RENDER_MARGIN, -ySize - TileMonitor.RENDER_MARGIN, 0.0 ).texture( 0.0, 1.0 ).color( r, g, b, 1.0f ).next();
                    renderer.vertex( xSize + TileMonitor.RENDER_MARGIN, TileMonitor.RENDER_MARGIN, 0.0D ).texture( 1.0, 0.0 ).color( r, g, b, 1.0f ).next();
                    renderer.vertex( xSize + TileMonitor.RENDER_MARGIN, -ySize - TileMonitor.RENDER_MARGIN, 0.0 ).texture( 1.0, 1.0 ).color( r, g, b, 1.0f ).next();
                    tessellator.draw();
                }
            }
            finally
            {
                GlStateManager.depthMask( true );
                mc.gameRenderer.enableLightmap();
                GlStateManager.enableLighting();
            }

            // Draw the depth blocker
            GlStateManager.colorMask( false, false, false, false );
            try
            {
                mc.getTextureManager().bindTexture( FixedWidthFontRenderer.BACKGROUND );
                renderer.begin( GL11.GL_TRIANGLE_STRIP, VertexFormats.POSITION );
                renderer.vertex( -TileMonitor.RENDER_MARGIN, TileMonitor.RENDER_MARGIN, 0.0 ).next();
                renderer.vertex( -TileMonitor.RENDER_MARGIN, -ySize - TileMonitor.RENDER_MARGIN, 0.0 ).next();
                renderer.vertex( xSize + TileMonitor.RENDER_MARGIN, TileMonitor.RENDER_MARGIN, 0.0 ).next();
                renderer.vertex( xSize + TileMonitor.RENDER_MARGIN, -ySize - TileMonitor.RENDER_MARGIN, 0.0 ).next();
                tessellator.draw();
            }
            finally
            {
                GlStateManager.colorMask( true, true, true, true );
            }
        }
        finally
        {
            GlStateManager.color4f( 1.0f, 1.0f, 1.0f, 1.0f );
            GlStateManager.popMatrix();
        }
    }
}
