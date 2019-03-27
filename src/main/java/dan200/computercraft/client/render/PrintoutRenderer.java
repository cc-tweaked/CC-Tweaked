/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.shared.media.items.ItemPrintout.LINES_PER_PAGE;

public class PrintoutRenderer
{
    private static final ResourceLocation BG = new ResourceLocation( "computercraft", "textures/gui/printout.png" );
    private static final double BG_SIZE = 256.0;

    /**
     * Width of a page
     */
    public static final int X_SIZE = 172;

    /**
     * Height of a page
     */
    public static final int Y_SIZE = 209;

    /**
     * Padding between the left and right of a page and the text
     */
    public static final int X_TEXT_MARGIN = 13;

    /**
     * Padding between the top and bottom of a page and the text
     */
    public static final int Y_TEXT_MARGIN = 11;

    /**
     * Width of the extra page texture
     */
    private static final int X_FOLD_SIZE = 12;

    /**
     * Size of the leather cover
     */
    public static final int COVER_SIZE = 12;

    private static final int COVER_Y = Y_SIZE;
    private static final int COVER_X = X_SIZE + 4 * X_FOLD_SIZE;

    public static void drawText( int x, int y, int start, TextBuffer[] text, TextBuffer[] colours )
    {
        FixedWidthFontRenderer fontRenderer = FixedWidthFontRenderer.instance();

        for( int line = 0; line < LINES_PER_PAGE && line < text.length; line++ )
        {
            fontRenderer.drawString( text[start + line], x, y + line * FONT_HEIGHT, colours[start + line], null, 0, 0, false, Palette.DEFAULT );
        }
    }

    public static void drawText( int x, int y, int start, String[] text, String[] colours )
    {
        GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate( SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO );

        FixedWidthFontRenderer fontRenderer = FixedWidthFontRenderer.instance();

        for( int line = 0; line < LINES_PER_PAGE && line < text.length; line++ )
        {
            fontRenderer.drawString( new TextBuffer( text[start + line] ), x, y + line * FONT_HEIGHT, new TextBuffer( colours[start + line] ), null, 0, 0, false, Palette.DEFAULT );
        }
    }

    public static void drawBorder( double x, double y, double z, int page, int pages, boolean isBook )
    {
        GlStateManager.color( 1.0f, 1.0f, 1.0f, 1.0f );
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate( SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO );

        Minecraft.getMinecraft().getTextureManager().bindTexture( BG );

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX );

        int leftPages = page;
        int rightPages = pages - page - 1;

        if( isBook )
        {
            // Border
            double offset = offsetAt( pages );
            final double left = x - 4 - offset;
            final double right = x + X_SIZE + offset - 4;

            // Left and right border
            drawTexture( buffer, left - 4, y - 8, z - 0.02, COVER_X, 0, COVER_SIZE, Y_SIZE + COVER_SIZE * 2 );
            drawTexture( buffer, right, y - 8, z - 0.02, COVER_X + COVER_SIZE, 0, COVER_SIZE, Y_SIZE + COVER_SIZE * 2 );

            // Draw centre panel (just stretched texture, sorry).
            drawTexture( buffer,
                x - offset, y, z - 0.02, X_SIZE + offset * 2, Y_SIZE,
                COVER_X + COVER_SIZE / 2.0f, COVER_SIZE, COVER_SIZE, Y_SIZE
            );

            double borderX = left;
            while( borderX < right )
            {
                double thisWidth = Math.min( right - borderX, X_SIZE );
                drawTexture( buffer, borderX, y - 8, z - 0.02, 0, COVER_Y, thisWidth, COVER_SIZE );
                drawTexture( buffer, borderX, y + Y_SIZE - 4, z - 0.02, 0, COVER_Y + COVER_SIZE, thisWidth, COVER_SIZE );
                borderX += thisWidth;
            }
        }

        // Left half
        drawTexture( buffer, x, y, z, X_FOLD_SIZE * 2, 0, X_SIZE / 2.0f, Y_SIZE );
        for( int n = 0; n <= leftPages; n++ )
        {
            drawTexture( buffer,
                x - offsetAt( n ), y, z - 1e-3 * n,
                // Use the left "bold" fold for the outermost page
                n == leftPages ? 0 : X_FOLD_SIZE, 0,
                X_FOLD_SIZE, Y_SIZE
            );
        }

        // Right half
        drawTexture( buffer, x + X_SIZE / 2.0f, y, z, X_FOLD_SIZE * 2 + X_SIZE / 2.0f, 0, X_SIZE / 2.0f, Y_SIZE );
        for( int n = 0; n <= rightPages; n++ )
        {
            drawTexture( buffer,
                x + (X_SIZE - X_FOLD_SIZE) + offsetAt( n ), y, z - 1e-3 * n,
                // Two folds, then the main page. Use the right "bold" fold for the outermost page.
                X_FOLD_SIZE * 2 + X_SIZE + (n == rightPages ? X_FOLD_SIZE : 0), 0,
                X_FOLD_SIZE, Y_SIZE
            );
        }

        tessellator.draw();
    }

    private static void drawTexture( BufferBuilder buffer, double x, double y, double z, double u, double v, double width, double height )
    {
        buffer.pos( x, y + height, z ).tex( u / BG_SIZE, (v + height) / BG_SIZE ).endVertex();
        buffer.pos( x + width, y + height, z ).tex( (u + width) / BG_SIZE, (v + height) / BG_SIZE ).endVertex();
        buffer.pos( x + width, y, z ).tex( (u + width) / BG_SIZE, v / BG_SIZE ).endVertex();
        buffer.pos( x, y, z ).tex( u / BG_SIZE, v / BG_SIZE ).endVertex();
    }

    private static void drawTexture( BufferBuilder buffer, double x, double y, double z, double width, double height, double u, double v, double tWidth, double tHeight )
    {
        buffer.pos( x, y + height, z ).tex( u / BG_SIZE, (v + tHeight) / BG_SIZE ).endVertex();
        buffer.pos( x + width, y + height, z ).tex( (u + tWidth) / BG_SIZE, (v + tHeight) / BG_SIZE ).endVertex();
        buffer.pos( x + width, y, z ).tex( (u + tWidth) / BG_SIZE, v / BG_SIZE ).endVertex();
        buffer.pos( x, y, z ).tex( u / BG_SIZE, v / BG_SIZE ).endVertex();
    }

    public static double offsetAt( int page )
    {
        return 32 * (1 - Math.pow( 1.2, -page ));
    }
}
