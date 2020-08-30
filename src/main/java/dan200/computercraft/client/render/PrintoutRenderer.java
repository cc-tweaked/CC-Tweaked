/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL11;

import static dan200.computercraft.client.gui.FixedWidthFontRenderer.FONT_HEIGHT;
import static dan200.computercraft.shared.media.items.ItemPrintout.LINES_PER_PAGE;

public final class PrintoutRenderer
{
    private static final Identifier BG = new Identifier( "computercraft", "textures/gui/printout.png" );
    private static final float BG_SIZE = 256.0f;

    /**
     * Width of a page.
     */
    public static final int X_SIZE = 172;

    /**
     * Height of a page.
     */
    public static final int Y_SIZE = 209;

    /**
     * Padding between the left and right of a page and the text.
     */
    public static final int X_TEXT_MARGIN = 13;

    /**
     * Padding between the top and bottom of a page and the text.
     */
    public static final int Y_TEXT_MARGIN = 11;

    /**
     * Width of the extra page texture.
     */
    private static final int X_FOLD_SIZE = 12;

    /**
     * Size of the leather cover.
     */
    public static final int COVER_SIZE = 12;

    private static final int COVER_Y = Y_SIZE;
    private static final int COVER_X = X_SIZE + 4 * X_FOLD_SIZE;

    private PrintoutRenderer() {}

    public static void drawText( Matrix4f transform, VertexConsumerProvider renderer, int x, int y, int start, TextBuffer[] text, TextBuffer[] colours )
    {
        VertexConsumer buffer = renderer.getBuffer( FixedWidthFontRenderer.TYPE );
        for( int line = 0; line < LINES_PER_PAGE && line < text.length; line++ )
        {
            FixedWidthFontRenderer.drawString( transform, buffer,
                x, y + line * FONT_HEIGHT, text[start + line], colours[start + line], null, Palette.DEFAULT,
                false, 0, 0
            );
        }
    }

    public static void drawText( Matrix4f transform, VertexConsumerProvider renderer, int x, int y, int start, String[] text, String[] colours )
    {
        VertexConsumer buffer = renderer.getBuffer( FixedWidthFontRenderer.TYPE );
        for( int line = 0; line < LINES_PER_PAGE && line < text.length; line++ )
        {
            FixedWidthFontRenderer.drawString( transform, buffer,
                x, y + line * FONT_HEIGHT,
                new TextBuffer( text[start + line] ), new TextBuffer( colours[start + line] ),
                null, Palette.DEFAULT, false, 0, 0
            );
        }
    }

    public static void drawBorder( Matrix4f transform, VertexConsumerProvider renderer, float x, float y, float z, int page, int pages, boolean isBook )
    {
        int leftPages = page;
        int rightPages = pages - page - 1;

        VertexConsumer buffer = renderer.getBuffer( Type.TYPE );

        if( isBook )
        {
            // Border
            float offset = offsetAt( pages );
            float left = x - 4 - offset;
            float right = x + X_SIZE + offset - 4;

            // Left and right border
            drawTexture( transform, buffer, left - 4, y - 8, z - 0.02f, COVER_X, 0, COVER_SIZE, Y_SIZE + COVER_SIZE * 2 );
            drawTexture( transform, buffer, right, y - 8, z - 0.02f, COVER_X + COVER_SIZE, 0, COVER_SIZE, Y_SIZE + COVER_SIZE * 2 );

            // Draw centre panel (just stretched texture, sorry).
            drawTexture( transform, buffer,
                x - offset, y, z - 0.02f, X_SIZE + offset * 2, Y_SIZE,
                COVER_X + COVER_SIZE / 2.0f, COVER_SIZE, COVER_SIZE, Y_SIZE
            );

            float borderX = left;
            while( borderX < right )
            {
                double thisWidth = Math.min( right - borderX, X_SIZE );
                drawTexture( transform, buffer, borderX, y - 8, z - 0.02f, 0, COVER_Y, (float) thisWidth, COVER_SIZE );
                drawTexture( transform, buffer, borderX, y + Y_SIZE - 4, z - 0.02f, 0, COVER_Y + COVER_SIZE, (float) thisWidth, COVER_SIZE );
                borderX += thisWidth;
            }
        }

        // Left half
        drawTexture( transform, buffer, x, y, z, X_FOLD_SIZE * 2, 0, X_SIZE / 2.0f, Y_SIZE );
        for( int n = 0; n <= leftPages; n++ )
        {
            drawTexture( transform, buffer,
                x - offsetAt( n ), y, z - 1e-3f * n,
                // Use the left "bold" fold for the outermost page
                n == leftPages ? 0 : X_FOLD_SIZE, 0,
                X_FOLD_SIZE, Y_SIZE
            );
        }

        // Right half
        drawTexture( transform, buffer, x + X_SIZE / 2.0f, y, z, X_FOLD_SIZE * 2 + X_SIZE / 2.0f, 0, X_SIZE / 2.0f, Y_SIZE );
        for( int n = 0; n <= rightPages; n++ )
        {
            drawTexture( transform, buffer,
                x + (X_SIZE - X_FOLD_SIZE) + offsetAt( n ), y, z - 1e-3f * n,
                // Two folds, then the main page. Use the right "bold" fold for the outermost page.
                X_FOLD_SIZE * 2 + X_SIZE + (n == rightPages ? X_FOLD_SIZE : 0), 0,
                X_FOLD_SIZE, Y_SIZE
            );
        }
    }

    private static void drawTexture( Matrix4f matrix, VertexConsumer buffer, float x, float y, float z, float u, float v, float width, float height )
    {
        buffer.vertex( matrix, x, y + height, z ).texture( u / BG_SIZE, (v + height) / BG_SIZE ).next();
        buffer.vertex( matrix, x + width, y + height, z ).texture( (u + width) / BG_SIZE, (v + height) / BG_SIZE ).next();
        buffer.vertex( matrix, x + width, y, z ).texture( (u + width) / BG_SIZE, v / BG_SIZE ).next();
        buffer.vertex( matrix, x, y, z ).texture( u / BG_SIZE, v / BG_SIZE ).next();
    }

    private static void drawTexture( Matrix4f matrix, VertexConsumer buffer, float x, float y, float z, float width, float height, float u, float v, float tWidth, float tHeight )
    {
        buffer.vertex( matrix, x, y + height, z ).texture( u / BG_SIZE, (v + tHeight) / BG_SIZE ).next();
        buffer.vertex( matrix, x + width, y + height, z ).texture( (u + tWidth) / BG_SIZE, (v + tHeight) / BG_SIZE ).next();
        buffer.vertex( matrix, x + width, y, z ).texture( (u + tWidth) / BG_SIZE, v / BG_SIZE ).next();
        buffer.vertex( matrix, x, y, z ).texture( u / BG_SIZE, v / BG_SIZE ).next();
    }

    public static float offsetAt( int page )
    {
        return (float) (32 * (1 - Math.pow( 1.2, -page )));
    }

    private static final class Type extends RenderPhase
    {
        static final RenderLayer TYPE = RenderLayer.of(
            "printout_background", VertexFormats.POSITION_TEXTURE, GL11.GL_QUADS, 1024,
            false, false, // useDelegate, needsSorting
            RenderLayer.MultiPhaseParameters.builder()
                .texture( new RenderPhase.Texture( BG, false, false ) ) // blur, minimap
                .alpha( ONE_TENTH_ALPHA )
                .lightmap( DISABLE_LIGHTMAP )
                .build( false )
        );

        private Type( String name, Runnable setup, Runnable destroy )
        {
            super( name, setup, destroy );
        }
    }
}
