/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render.text;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.*;

/**
 * An optimised copy of {@link FixedWidthFontRenderer} emitter emits direclty to a {@link ByteBuffer} rather than
 * emitting to {@link VertexConsumer}. This allows us to emit vertices very quickly, when using the VBO renderer.
 *
 * There are some limitations here:
 * <ul>
 *   <li>No transformation matrix (not needed for VBOs).</li>
 *   <li>Only works with {@link DefaultVertexFormat#POSITION_COLOR_TEX_LIGHTMAP}.</li>
 * </ul>
 *
 * Note this is almost an exact copy of {@link FixedWidthFontRenderer}. While the code duplication is unfortunate,
 * it is measurably faster than introducing polymorphism into {@link FixedWidthFontRenderer}.
 *
 * <strong>IMPORTANT: </strong> When making changes to this class, please check if you need to make the same changes to
 * {@link FixedWidthFontRenderer}.
 */
public final class DirectFixedWidthFontRenderer
{
    private DirectFixedWidthFontRenderer()
    {
    }

    private static void drawChar( ByteBuffer buffer, float x, float y, int index, byte[] colour )
    {
        // Short circuit to avoid the common case - the texture should be blank here after all.
        if( index == '\0' || index == ' ' ) return;

        int column = index % 16;
        int row = index / 16;

        int xStart = 1 + column * (FONT_WIDTH + 2);
        int yStart = 1 + row * (FONT_HEIGHT + 2);

        quad(
            buffer, x, y, x + FONT_WIDTH, y + FONT_HEIGHT, colour,
            xStart / WIDTH, yStart / WIDTH, (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH
        );
    }

    private static void drawQuad( ByteBuffer emitter, float x, float y, float width, float height, Palette palette, boolean greyscale, char colourIndex )
    {
        var colour = palette.getByteColour( getColour( colourIndex, Colour.BLACK ), greyscale );
        quad( emitter, x, y, x + width, y + height, colour, BACKGROUND_START, BACKGROUND_START, BACKGROUND_END, BACKGROUND_END );
    }

    private static void drawBackground(
        @Nonnull ByteBuffer buffer, float x, float y, @Nonnull TextBuffer backgroundColour, @Nonnull Palette palette, boolean greyscale,
        float leftMarginSize, float rightMarginSize, float height
    )
    {
        if( leftMarginSize > 0 )
        {
            drawQuad( buffer, x - leftMarginSize, y, leftMarginSize, height, palette, greyscale, backgroundColour.charAt( 0 ) );
        }

        if( rightMarginSize > 0 )
        {
            drawQuad( buffer, x + backgroundColour.length() * FONT_WIDTH, y, rightMarginSize, height, palette, greyscale, backgroundColour.charAt( backgroundColour.length() - 1 ) );
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
                drawQuad( buffer, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (i - blockStart), height, palette, greyscale, blockColour );
            }

            blockColour = colourIndex;
            blockStart = i;
        }

        if( blockColour != '\0' )
        {
            drawQuad( buffer, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (backgroundColour.length() - blockStart), height, palette, greyscale, blockColour );
        }
    }

    private static void drawString( @Nonnull ByteBuffer buffer, float x, float y, @Nonnull TextBuffer text, @Nonnull TextBuffer textColour, @Nonnull Palette palette, boolean greyscale )
    {
        for( int i = 0; i < text.length(); i++ )
        {
            var colour = palette.getByteColour( getColour( textColour.charAt( i ), Colour.BLACK ), greyscale );

            int index = text.charAt( i );
            if( index > 255 ) index = '?';
            drawChar( buffer, x + i * FONT_WIDTH, y, index, colour );
        }
    }

    public static void drawTerminalWithoutCursor(
        @Nonnull ByteBuffer buffer, float x, float y, @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        Palette palette = terminal.getPalette();
        int height = terminal.getHeight();

        // Top and bottom margins
        drawBackground(
            buffer, x, y - topMarginSize, terminal.getBackgroundColourLine( 0 ), palette, greyscale,
            leftMarginSize, rightMarginSize, topMarginSize
        );

        drawBackground(
            buffer, x, y + height * FONT_HEIGHT, terminal.getBackgroundColourLine( height - 1 ), palette, greyscale,
            leftMarginSize, rightMarginSize, bottomMarginSize
        );

        // The main text
        for( int i = 0; i < height; i++ )
        {
            float rowY = y + FONT_HEIGHT * i;
            drawBackground(
                buffer, x, rowY, terminal.getBackgroundColourLine( i ), palette, greyscale,
                leftMarginSize, rightMarginSize, FONT_HEIGHT
            );
            drawString(
                buffer, x, rowY, terminal.getLine( i ), terminal.getTextColourLine( i ),
                palette, greyscale
            );
        }
    }

    public static int getVertexCount( Terminal terminal )
    {
        return (terminal.getHeight() + 2) * terminal.getWidth() * 2 * 4;
    }

    private static void quad( ByteBuffer buffer, float x1, float y1, float x2, float y2, byte[] rgba, float u1, float v1, float u2, float v2 )
    {
        int position = buffer.position();

        // Check we've got enough space to write all our points. In an ideal world this'd get the JIT to eliminate
        // bounds checks. It doesn't, but it does cause the JITted method to be marginally smaller (unclear why, diffing
        // asm is nigh impossible as the register allocator is non-deterministic),
        if( position < 0 || 96 > buffer.limit() - position ) throw new IndexOutOfBoundsException();
        // Also assert the length of the array. This does appear to help, though cannot get into the nitty-gritty of why
        // (again, see above comment on diffing asm).
        if( rgba.length != 4 ) throw new IllegalStateException();

        buffer.putFloat( position + 0, x1 ).putFloat( position + 4, y1 ).putFloat( position + 8, 0 ).put( position + 12, rgba, 0, 4 ).putFloat( position + 16, u1 ).putFloat( position + 20, v1 );
        buffer.putFloat( position + 24, x1 ).putFloat( position + 28, y2 ).putFloat( position + 32, 0 ).put( position + 36, rgba, 0, 4 ).putFloat( position + 40, u1 ).putFloat( position + 44, v2 );
        buffer.putFloat( position + 48, x2 ).putFloat( position + 52, y2 ).putFloat( position + 56, 0 ).put( position + 60, rgba, 0, 4 ).putFloat( position + 64, u2 ).putFloat( position + 68, v2 );
        buffer.putFloat( position + 72, x2 ).putFloat( position + 76, y1 ).putFloat( position + 80, 0 ).put( position + 84, rgba, 0, 4 ).putFloat( position + 88, u2 ).putFloat( position + 92, v1 );

        buffer.position( position + 96 );
    }
}
