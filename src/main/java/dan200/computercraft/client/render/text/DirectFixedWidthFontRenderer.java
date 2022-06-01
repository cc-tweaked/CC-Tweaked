/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render.text;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import dan200.computercraft.client.util.DirectBuffers;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.*;
import static org.lwjgl.system.MemoryUtil.memPutByte;
import static org.lwjgl.system.MemoryUtil.memPutFloat;

/**
 * An optimised copy of {@link FixedWidthFontRenderer} emitter emits directly to a {@link ByteBuffer} rather than
 * emitting to {@link IVertexBuilder}. This allows us to emit vertices very quickly, when using the VBO renderer.
 *
 * There are some limitations here:
 * <ul>
 *   <li>No transformation matrix (not needed for VBOs).</li>
 *   <li>Only works with {@link DefaultVertexFormats#POSITION_COLOR_TEX}.</li>
 *   <li>The buffer <strong>MUST</strong> be allocated with {@link DirectBuffers}, and not through any other means.</li>
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
        byte[] colour = palette.getByteColour( getColour( colourIndex, Colour.BLACK ), greyscale );
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
            byte[] colour = palette.getByteColour( getColour( textColour.charAt( i ), Colour.BLACK ), greyscale );

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

    public static void drawCursor( @Nonnull ByteBuffer buffer, float x, float y, @Nonnull Terminal terminal, boolean greyscale )
    {
        if( isCursorVisible( terminal ) )
        {
            byte[] colour = terminal.getPalette().getByteColour( 15 - terminal.getTextColour(), greyscale );
            drawChar( buffer, x + terminal.getCursorX() * FONT_WIDTH, y + terminal.getCursorY() * FONT_HEIGHT, '_', colour );
        }
    }

    public static int getVertexCount( Terminal terminal )
    {
        return (1 + (terminal.getHeight() + 2) * terminal.getWidth() * 2) * 4;
    }

    private static void quad( ByteBuffer buffer, float x1, float y1, float x2, float y2, byte[] rgba, float u1, float v1, float u2, float v2 )
    {
        // Emit a single quad to our buffer. This uses Unsafe (well, LWJGL's MemoryUtil) to directly blit bytes to the
        // underlying buffer. This allows us to have a single bounds check up-front, rather than one for every write.
        // This provides significant performance gains, at the cost of well, using Unsafe.
        // Each vertex is 24 bytes, giving 96 bytes in total. Vertices are of the form (xyz:FFF)(rgba:BBBB)(uv:FF),
        // which matches the POSITION_COLOR_TEX vertex format.

        int position = buffer.position();
        long addr = MemoryUtil.memAddress( buffer );

        // We're doing terrible unsafe hacks below, so let's be really sure that what we're doing is reasonable.
        if( position < 0 || 96 > buffer.limit() - position ) throw new IndexOutOfBoundsException();
        // Require the pointer to be aligned to a 32-bit boundary.
        if( (addr & 3) != 0 ) throw new IllegalStateException( "Memory is not aligned" );
        // Also assert the length of the array. This appears to help elide bounds checks on the array in some circumstances.
        if( rgba.length != 4 ) throw new IllegalStateException();

        memPutFloat( addr + 0, x1 );
        memPutFloat( addr + 4, y1 );
        memPutFloat( addr + 8, 0 );
        memPutByte( addr + 12, rgba[0] );
        memPutByte( addr + 13, rgba[1] );
        memPutByte( addr + 14, rgba[2] );
        memPutByte( addr + 15, (byte) 255 );
        memPutFloat( addr + 16, u1 );
        memPutFloat( addr + 20, v1 );

        memPutFloat( addr + 24, x1 );
        memPutFloat( addr + 28, y2 );
        memPutFloat( addr + 32, 0 );
        memPutByte( addr + 36, rgba[0] );
        memPutByte( addr + 37, rgba[1] );
        memPutByte( addr + 38, rgba[2] );
        memPutByte( addr + 39, (byte) 255 );
        memPutFloat( addr + 40, u1 );
        memPutFloat( addr + 44, v2 );

        memPutFloat( addr + 48, x2 );
        memPutFloat( addr + 52, y2 );
        memPutFloat( addr + 56, 0 );
        memPutByte( addr + 60, rgba[0] );
        memPutByte( addr + 61, rgba[1] );
        memPutByte( addr + 62, rgba[2] );
        memPutByte( addr + 63, (byte) 255 );
        memPutFloat( addr + 64, u2 );
        memPutFloat( addr + 68, v2 );

        memPutFloat( addr + 72, x2 );
        memPutFloat( addr + 76, y1 );
        memPutFloat( addr + 80, 0 );
        memPutByte( addr + 84, rgba[0] );
        memPutByte( addr + 85, rgba[1] );
        memPutByte( addr + 86, rgba[2] );
        memPutByte( addr + 87, (byte) 255 );
        memPutFloat( addr + 88, u2 );
        memPutFloat( addr + 92, v1 );

        // Finally increment the position.
        buffer.position( position + 96 );

        // Well done for getting to the end of this method. I recommend you take a break and go look at cute puppies.
    }
}
