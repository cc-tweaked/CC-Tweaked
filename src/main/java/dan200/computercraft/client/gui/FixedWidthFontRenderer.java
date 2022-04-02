/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;

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
 * <li>{@link #drawBlocker}: When rendering a terminal using {@link RenderTypes#TERMINAL_WITHOUT_DEPTH} you need to
 * render an additional "depth blocker" on top of the monitor.</li>
 * </ul>
 */
public final class FixedWidthFontRenderer
{
    public static final ResourceLocation FONT = new ResourceLocation( "computercraft", "textures/gui/term_font.png" );

    public static final int FONT_HEIGHT = 9;
    public static final int FONT_WIDTH = 6;
    private static final float WIDTH = 256.0f;

    private static final float BACKGROUND_START = (WIDTH - 6.0f) / WIDTH;
    private static final float BACKGROUND_END = (WIDTH - 4.0f) / WIDTH;

    private static final byte[] BLACK = new byte[] { byteColour( Colour.BLACK.getR() ), byteColour( Colour.BLACK.getR() ), byteColour( Colour.BLACK.getR() ), (byte) 255 };

    private FixedWidthFontRenderer()
    {
    }

    private static byte byteColour( float c )
    {
        return (byte) (int) (c * 255);
    }

    public static float toGreyscale( double[] rgb )
    {
        return (float) ((rgb[0] + rgb[1] + rgb[2]) / 3);
    }

    public static int getColour( char c, Colour def )
    {
        return 15 - Terminal.getColour( c, def );
    }

    private static void drawChar( VertexEmitter emitter, float x, float y, int index, byte[] colour, int light )
    {
        // Short circuit to avoid the common case - the texture should be blank here after all.
        if( index == '\0' || index == ' ' ) return;

        int column = index % 16;
        int row = index / 16;

        int xStart = 1 + column * (FONT_WIDTH + 2);
        int yStart = 1 + row * (FONT_HEIGHT + 2);

        emitter.vertex( x, y, (float) 0, colour, xStart / WIDTH, yStart / WIDTH, light );
        emitter.vertex( x, y + FONT_HEIGHT, (float) 0, colour, xStart / WIDTH, (yStart + FONT_HEIGHT) / WIDTH, light );
        emitter.vertex( x + FONT_WIDTH, y + FONT_HEIGHT, (float) 0, colour, (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH, light );
        emitter.vertex( x + FONT_WIDTH, y, (float) 0, colour, (xStart + FONT_WIDTH) / WIDTH, yStart / WIDTH, light );
    }

    public static void drawQuad( VertexEmitter emitter, float x, float y, float z, float width, float height, byte[] colour, int light )
    {
        emitter.vertex( x, y, z, colour, BACKGROUND_START, BACKGROUND_START, light );
        emitter.vertex( x, y + height, z, colour, BACKGROUND_START, BACKGROUND_END, light );
        emitter.vertex( x + width, y + height, z, colour, BACKGROUND_END, BACKGROUND_END, light );
        emitter.vertex( x + width, y, z, colour, BACKGROUND_END, BACKGROUND_START, light );
    }

    private static void drawQuad( VertexEmitter emitter, float x, float y, float width, float height, Palette palette, boolean greyscale, char colourIndex, int light )
    {
        var colour = palette.getByteColour( getColour( colourIndex, Colour.BLACK ), greyscale );
        drawQuad( emitter, x, y, 0, width, height, colour, light );
    }

    private static void drawBackground(
        @Nonnull VertexEmitter emitter, float x, float y,
        @Nonnull TextBuffer backgroundColour, @Nonnull Palette palette, boolean greyscale,
        float leftMarginSize, float rightMarginSize, float height, int light
    )
    {
        if( leftMarginSize > 0 )
        {
            drawQuad( emitter, x - leftMarginSize, y, leftMarginSize, height, palette, greyscale, backgroundColour.charAt( 0 ), light );
        }

        if( rightMarginSize > 0 )
        {
            drawQuad( emitter, x + backgroundColour.length() * FONT_WIDTH, y, rightMarginSize, height, palette, greyscale, backgroundColour.charAt( backgroundColour.length() - 1 ), light );
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
                drawQuad( emitter, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (i - blockStart), height, palette, greyscale, blockColour, light );
            }

            blockColour = colourIndex;
            blockStart = i;
        }

        if( blockColour != '\0' )
        {
            drawQuad( emitter, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (backgroundColour.length() - blockStart), height, palette, greyscale, blockColour, light );
        }
    }

    public static void drawString(
        @Nonnull VertexEmitter emitter, float x, float y,
        @Nonnull TextBuffer text, @Nonnull TextBuffer textColour, @Nullable TextBuffer backgroundColour,
        @Nonnull Palette palette, boolean greyscale, float leftMarginSize, float rightMarginSize, int light
    )
    {
        if( backgroundColour != null )
        {
            drawBackground( emitter, x, y, backgroundColour, palette, greyscale, leftMarginSize, rightMarginSize, FONT_HEIGHT, light );
        }

        for( int i = 0; i < text.length(); i++ )
        {
            var colour = palette.getByteColour( getColour( textColour.charAt( i ), Colour.BLACK ), greyscale );

            // Draw char
            int index = text.charAt( i );
            if( index > 255 ) index = '?';
            drawChar( emitter, x + i * FONT_WIDTH, y, index, colour, light );
        }

    }

    public static void drawTerminalWithoutCursor(
        @Nonnull VertexEmitter emitter, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        Palette palette = terminal.getPalette();
        int height = terminal.getHeight();

        // Top and bottom margins
        drawBackground(
            emitter, x, y - topMarginSize,
            terminal.getBackgroundColourLine( 0 ), palette, greyscale,
            leftMarginSize, rightMarginSize, topMarginSize, FULL_BRIGHT_LIGHTMAP
        );

        drawBackground(
            emitter, x, y + height * FONT_HEIGHT,
            terminal.getBackgroundColourLine( height - 1 ), palette, greyscale,
            leftMarginSize, rightMarginSize, bottomMarginSize, FULL_BRIGHT_LIGHTMAP
        );

        // The main text
        for( int i = 0; i < height; i++ )
        {
            drawString(
                emitter, x, y + FixedWidthFontRenderer.FONT_HEIGHT * i,
                terminal.getLine( i ), terminal.getTextColourLine( i ), terminal.getBackgroundColourLine( i ),
                palette, greyscale, leftMarginSize, rightMarginSize, FULL_BRIGHT_LIGHTMAP
            );
        }
    }

    public static void drawCursor( @Nonnull VertexEmitter emitter, float x, float y, @Nonnull Terminal terminal, boolean greyscale )
    {
        Palette palette = terminal.getPalette();
        int width = terminal.getWidth();
        int height = terminal.getHeight();

        int cursorX = terminal.getCursorX();
        int cursorY = terminal.getCursorY();
        if( terminal.getCursorBlink() && cursorX >= 0 && cursorX < width && cursorY >= 0 && cursorY < height && FrameInfo.getGlobalCursorBlink() )
        {
            var colour = palette.getByteColour( 15 - terminal.getTextColour(), greyscale );
            drawChar( emitter, x + cursorX * FONT_WIDTH, y + cursorY * FONT_HEIGHT, '_', colour, FULL_BRIGHT_LIGHTMAP );
        }
    }

    public static void drawTerminal(
        @Nonnull VertexEmitter buffer, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        drawTerminalWithoutCursor( buffer, x, y, terminal, greyscale, topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize );
        drawCursor( buffer, x, y, terminal, greyscale );
    }

    public static void drawEmptyTerminal( @Nonnull VertexEmitter emitter, float x, float y, float width, float height )
    {
        drawQuad( emitter, x, y, 0, width, height, BLACK, FULL_BRIGHT_LIGHTMAP );
    }

    public static void drawBlocker( @Nonnull VertexEmitter emitter, float x, float y, float width, float height )
    {
        drawQuad( emitter, x, y, 0, width, height, BLACK, FULL_BRIGHT_LIGHTMAP );
    }

    public static int getVertexCount( Terminal terminal )
    {
        int height = terminal.getHeight();
        int count = 0;

        for( int y = 0; y < height; y++ )
        {
            // We compress runs of adjacent characters, so we need to do that calculation here too :/.
            int background = 2;
            TextBuffer backgroundColour = terminal.getBackgroundColourLine( y );
            char blockColour = '\0';
            for( int x = 0; x < backgroundColour.length(); x++ )
            {
                char colourIndex = backgroundColour.charAt( x );
                if( colourIndex == blockColour ) continue;

                if( blockColour != '\0' ) background++;
                blockColour = colourIndex;
            }
            if( blockColour != '\0' ) background++;

            count += background;
            if( y == 0 ) count += background;
            if( y == height - 1 ) count += background;

            // Thankfully the normal characters are much easier!
            TextBuffer foreground = terminal.getLine( y );
            for( int x = 0; x < foreground.length(); x++ )
            {
                char c = foreground.charAt( x );
                if( c != '\0' && c != ' ' ) count++;
            }
        }

        return count * 4;
    }

    /**
     * Emit a single vertex to some buffer.
     *
     * @see #toVertexConsumer(Matrix4f, VertexConsumer) Emits to a {@link VertexConsumer}.
     * @see #toByteBuffer(ByteBuffer) Emits to a {@link ByteBuffer}.
     */
    @FunctionalInterface
    public interface VertexEmitter
    {
        void vertex( float x, float y, float z, byte[] rgba, float u, float v, int light );
    }

    public static VertexEmitter toVertexConsumer( Matrix4f matrix, VertexConsumer consumer )
    {
        return ( float x, float y, float z, byte[] rgba, float u, float v, int light ) ->
            consumer.vertex( matrix, x, y, z ).color( rgba[0], rgba[1], rgba[2], rgba[3] ).uv( u, v ).uv2( light ).endVertex();
    }

    /**
     * An optimised vertex emitter which bypasses {@link VertexConsumer}. This allows us to emit vertices very quickly,
     * when using the VBO renderer with some limitations:
     * <ul>
     *     <li>No transformation matrix (not needed for VBOs).</li>
     *     <li>Only works with {@link DefaultVertexFormat#POSITION_COLOR_TEX_LIGHTMAP}.</li>
     * </ul>
     *
     * @param buffer The buffer to emit to. This must have space for at least {@link #getVertexCount(Terminal)} vertices.
     * @return The emitter, ot be passed to the rendering functions.
     */
    public static VertexEmitter toByteBuffer( ByteBuffer buffer )
    {
        return ( float x, float y, float z, byte[] rgba, float u, float v, int light ) -> buffer
            .putFloat( x ).putFloat( y ).putFloat( z ).put( rgba ).putFloat( u ).putFloat( v );
    }
}
