/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.client.render.RenderTypes.FULL_BRIGHT_LIGHTMAP;

/**
 * Handles rendering fixed width text and computer terminals.
 *
 * This class has several modes of usage:
 * <ul>
 * <li>{@link #drawString}: Drawing basic text without a terminal (such as for printouts). Unlike the other methods,
 * this accepts a lightmap coordinate as, unlike terminals, printed pages render fullbright.</li>
 * <li>{@link #drawTerminalWithoutCursor}/{@link #drawCursor}: Draw a terminal without a cursor and then draw the cursor
 * separately. This is used by the monitor renderer to render the terminal to a VBO and draw the cursor dynamically.
 * </li>
 * <li>{@link #drawTerminal}: Draw a terminal with a cursor. This is used by the various computer GUIs to render the
 * whole term.</li>
 * </ul>
 */
public final class FixedWidthFontRenderer
{
    public static final int FONT_HEIGHT = 9;
    public static final int FONT_WIDTH = 6;
    public static final float WIDTH = 256.0f;

    public static final float BACKGROUND_START = (WIDTH - 6.0f) / WIDTH;
    public static final float BACKGROUND_END = (WIDTH - 4.0f) / WIDTH;

    public static final float Z_EPSILON = 0.001f;

    private FixedWidthFontRenderer()
    {
    }

    public static float toGreyscale( double[] rgb )
    {
        return (float) ((rgb[0] + rgb[1] + rgb[2]) / 3);
    }

    public static int getColour( char c, Colour def )
    {
        return 15 - Terminal.getColour( c, def );
    }

    private static void drawChar( PoseStack transform, VertexConsumer buffer, float x, float y, int index, float r, float g, float b, int light )
    {
        // Short circuit to avoid the common case - the texture should be blank here after all.
        if( index == '\0' || index == ' ' ) return;

        int column = index % 16;
        int row = index / 16;

        int xStart = 1 + column * (FONT_WIDTH + 2);
        int yStart = 1 + row * (FONT_HEIGHT + 2);

        Matrix4f matrix = transform.last().pose();
        Matrix3f normalMatrix = transform.last().normal();
        vertex( matrix, normalMatrix, buffer, x, y, Z_EPSILON, r, g, b, xStart / WIDTH, yStart / WIDTH, light );
        vertex( matrix, normalMatrix, buffer, x, y + FONT_HEIGHT, Z_EPSILON, r, g, b, xStart / WIDTH, (yStart + FONT_HEIGHT) / WIDTH, light );
        vertex( matrix, normalMatrix, buffer, x + FONT_WIDTH, y + FONT_HEIGHT, Z_EPSILON, r, g, b, (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH, light );
        vertex( matrix, normalMatrix, buffer, x + FONT_WIDTH, y, Z_EPSILON, r, g, b, (xStart + FONT_WIDTH) / WIDTH, yStart / WIDTH, light );
    }

    private static void drawQuad( PoseStack transform, VertexConsumer buffer, float x, float y, float width, float height, float r, float g, float b, int light )
    {
        Matrix4f matrix = transform.last().pose();
        Matrix3f normalMatrix = transform.last().normal();
        vertex( matrix, normalMatrix, buffer, x, y, 0, r, g, b, BACKGROUND_START, BACKGROUND_START, light );
        vertex( matrix, normalMatrix, buffer, x, y + height, 0, r, g, b, BACKGROUND_START, BACKGROUND_END, light );
        vertex( matrix, normalMatrix, buffer, x + width, y + height, 0, r, g, b, BACKGROUND_END, BACKGROUND_END, light );
        vertex( matrix, normalMatrix, buffer, x + width, y, 0, r, g, b, BACKGROUND_END, BACKGROUND_START, light );
    }

    private static void drawQuad( PoseStack transform, VertexConsumer buffer, float x, float y, float width, float height, Palette palette, boolean greyscale, char colourIndex, int light )
    {
        double[] colour = palette.getColour( getColour( colourIndex, Colour.BLACK ) );
        float r, g, b;
        if( greyscale )
        {
            r = g = b = toGreyscale( colour );
        }
        else
        {
            r = (float) colour[0];
            g = (float) colour[1];
            b = (float) colour[2];
        }

        drawQuad( transform, buffer, x, y, width, height, r, g, b, light );
    }

    private static void drawBackground(
        @Nonnull PoseStack transform, @Nonnull VertexConsumer buffer, float x, float y,
        @Nonnull TextBuffer backgroundColour, @Nonnull Palette palette, boolean greyscale,
        float leftMarginSize, float rightMarginSize, float height, int light
    )
    {
        if( leftMarginSize > 0 )
        {
            drawQuad( transform, buffer, x - leftMarginSize, y, leftMarginSize, height, palette, greyscale, backgroundColour.charAt( 0 ), light );
        }

        if( rightMarginSize > 0 )
        {
            drawQuad( transform, buffer, x + backgroundColour.length() * FONT_WIDTH, y, rightMarginSize, height, palette, greyscale, backgroundColour.charAt( backgroundColour.length() - 1 ), light );
        }

        // Batch together runs of identical background cells.
        int blockStart = 0;
        char blockColour = '\0';
        for( int i = 0; i < backgroundColour.length(); i++ )
        {
            char colourIndex = backgroundColour.charAt( i );
            if( colourIndex == blockColour ) continue;

            if( blockColour != '\0' )
            {
                drawQuad( transform, buffer, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (i - blockStart), height, palette, greyscale, blockColour, light );
            }

            blockColour = colourIndex;
            blockStart = i;
        }

        if( blockColour != '\0' )
        {
            drawQuad( transform, buffer, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (backgroundColour.length() - blockStart), height, palette, greyscale, blockColour, light );
        }
    }

    public static void drawString(
        @Nonnull PoseStack transform, @Nonnull VertexConsumer buffer, float x, float y,
        @Nonnull TextBuffer text, @Nonnull TextBuffer textColour, @Nullable TextBuffer backgroundColour,
        @Nonnull Palette palette, boolean greyscale, float leftMarginSize, float rightMarginSize, int light
    )
    {
        if( backgroundColour != null )
        {
            drawBackground( transform, buffer, x, y, backgroundColour, palette, greyscale, leftMarginSize, rightMarginSize, FONT_HEIGHT, light );
        }

        for( int i = 0; i < text.length(); i++ )
        {
            double[] colour = palette.getColour( getColour( textColour.charAt( i ), Colour.BLACK ) );
            float r, g, b;
            if( greyscale )
            {
                r = g = b = toGreyscale( colour );
            }
            else
            {
                r = (float) colour[0];
                g = (float) colour[1];
                b = (float) colour[2];
            }

            // Draw char
            int index = text.charAt( i );
            if( index > 255 ) index = '?';
            drawChar( transform, buffer, x + i * FONT_WIDTH, y, index, r, g, b, light );
        }

    }

    public static void drawTerminalWithoutCursor(
        @Nonnull PoseStack transform, @Nonnull VertexConsumer buffer, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize, int light
    )
    {
        Palette palette = terminal.getPalette();
        int height = terminal.getHeight();

        // Top and bottom margins
        drawBackground(
            transform, buffer, x, y - topMarginSize,
            terminal.getBackgroundColourLine( 0 ), palette, greyscale,
            leftMarginSize, rightMarginSize, topMarginSize, light
        );

        drawBackground(
            transform, buffer, x, y + height * FONT_HEIGHT,
            terminal.getBackgroundColourLine( height - 1 ), palette, greyscale,
            leftMarginSize, rightMarginSize, bottomMarginSize, light
        );

        // The main text
        for( int i = 0; i < height; i++ )
        {
            drawString(
                transform, buffer, x, y + FixedWidthFontRenderer.FONT_HEIGHT * i,
                terminal.getLine( i ), terminal.getTextColourLine( i ), terminal.getBackgroundColourLine( i ),
                palette, greyscale, leftMarginSize, rightMarginSize, FULL_BRIGHT_LIGHTMAP
            );
        }
    }

    public static void drawCursor(
        @Nonnull PoseStack transform, @Nonnull VertexConsumer buffer, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale
    )
    {
        Palette palette = terminal.getPalette();
        int width = terminal.getWidth();
        int height = terminal.getHeight();

        int cursorX = terminal.getCursorX();
        int cursorY = terminal.getCursorY();
        if( terminal.getCursorBlink() && cursorX >= 0 && cursorX < width && cursorY >= 0 && cursorY < height && FrameInfo.getGlobalCursorBlink() )
        {
            double[] colour = palette.getColour( 15 - terminal.getTextColour() );
            float r, g, b;
            if( greyscale )
            {
                r = g = b = toGreyscale( colour );
            }
            else
            {
                r = (float) colour[0];
                g = (float) colour[1];
                b = (float) colour[2];
            }

            drawChar( transform, buffer, x + cursorX * FONT_WIDTH, y + cursorY * FONT_HEIGHT, '_', r, g, b, FULL_BRIGHT_LIGHTMAP );
        }
    }

    public static void drawTerminal(
        @Nonnull PoseStack transform, @Nonnull VertexConsumer buffer, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize, int light
    )
    {
        drawTerminalWithoutCursor( transform, buffer, x, y, terminal, greyscale, topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize, light );
        drawCursor( transform, buffer, x, y, terminal, greyscale );
    }

    // Called by WidgetTerminal
    public static void drawTerminalImmediate(
        @Nonnull PoseStack transform, float x, float y, @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        MultiBufferSource.BufferSource renderer = MultiBufferSource.immediate( Tesselator.getInstance().getBuilder() );
        VertexConsumer buffer = renderer.getBuffer( RenderTypes.GUI_TERMINAL );
        drawTerminal( transform, buffer, x, y, terminal, greyscale, topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize, FULL_BRIGHT_LIGHTMAP );
        renderer.endBatch();
    }

    public static void drawEmptyTerminal( @Nonnull PoseStack transform, @Nonnull VertexConsumer buffer, float x, float y, float width, float height, int light )
    {
        Colour colour = Colour.BLACK;
        drawQuad( transform, buffer, x, y, width, height, colour.getR(), colour.getG(), colour.getB(), light );
    }

    public static void drawEmptyTerminalImmediate( @Nonnull PoseStack transform, float x, float y, float width, float height )
    {
        MultiBufferSource.BufferSource renderer = MultiBufferSource.immediate( Tesselator.getInstance().getBuilder() );
        VertexConsumer buffer = renderer.getBuffer( RenderTypes.GUI_TERMINAL );
        drawEmptyTerminal( transform, buffer, x, y, width, height, FULL_BRIGHT_LIGHTMAP );
        renderer.endBatch();
    }

    private static void vertex( Matrix4f poseMatrix, Matrix3f normalMatrix, VertexConsumer buffer, float x, float y, float z, float r, float g, float b, float u, float v, int light )
    {
        buffer.vertex( poseMatrix, x, y, z ).color( r, g, b, 1.0f ).uv( u, v ).overlayCoords( OverlayTexture.NO_OVERLAY ).uv2( light ).normal( normalMatrix, 0f, 0f, 1f ).endVertex();
    }

}
