/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.RenderTypes;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.renderer.MultiBufferSource;
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
    public static final float WIDTH = 256.0f;

    public static final float BACKGROUND_START = (WIDTH - 6.0f) / WIDTH;
    public static final float BACKGROUND_END = (WIDTH - 4.0f) / WIDTH;

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

    private static void drawChar( VertexEmitter buffer, float x, float y, int index, float r, float g, float b, int light )
    {
        // Short circuit to avoid the common case - the texture should be blank here after all.
        if( index == '\0' || index == ' ' ) return;

        int column = index % 16;
        int row = index / 16;

        int xStart = 1 + column * (FONT_WIDTH + 2);
        int yStart = 1 + row * (FONT_HEIGHT + 2);

        buffer.vertex( x, y, 0f, r, g, b, xStart / WIDTH, yStart / WIDTH, light );
        buffer.vertex( x, y + FONT_HEIGHT, 0f, r, g, b, xStart / WIDTH, (yStart + FONT_HEIGHT) / WIDTH, light );
        buffer.vertex( x + FONT_WIDTH, y + FONT_HEIGHT, 0f, r, g, b, (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH, light );
        buffer.vertex( x + FONT_WIDTH, y, 0f, r, g, b, (xStart + FONT_WIDTH) / WIDTH, yStart / WIDTH, light );
    }

    private static void drawQuad( VertexEmitter buffer, float x, float y, float width, float height, float r, float g, float b, int light )
    {
        buffer.vertex( x, y, 0, r, g, b, BACKGROUND_START, BACKGROUND_START, light );
        buffer.vertex( x, y + height, 0, r, g, b, BACKGROUND_START, BACKGROUND_END, light );
        buffer.vertex( x + width, y + height, 0, r, g, b, BACKGROUND_END, BACKGROUND_END, light );
        buffer.vertex( x + width, y, 0, r, g, b, BACKGROUND_END, BACKGROUND_START, light );
    }

    private static void drawQuad( VertexEmitter buffer, float x, float y, float width, float height, Palette palette, boolean greyscale, char colourIndex, int light )
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

        drawQuad( buffer, x, y, width, height, r, g, b, light );
    }

    private static void drawBackground(
        @Nonnull VertexEmitter buffer, float x, float y,
        @Nonnull TextBuffer backgroundColour, @Nonnull Palette palette, boolean greyscale,
        float leftMarginSize, float rightMarginSize, float height, int light
    )
    {
        if( leftMarginSize > 0 )
        {
            drawQuad( buffer, x - leftMarginSize, y, leftMarginSize, height, palette, greyscale, backgroundColour.charAt( 0 ), light );
        }

        if( rightMarginSize > 0 )
        {
            drawQuad( buffer, x + backgroundColour.length() * FONT_WIDTH, y, rightMarginSize, height, palette, greyscale, backgroundColour.charAt( backgroundColour.length() - 1 ), light );
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
                drawQuad( buffer, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (i - blockStart), height, palette, greyscale, blockColour, light );
            }

            blockColour = colourIndex;
            blockStart = i;
        }

        if( blockColour != '\0' )
        {
            drawQuad( buffer, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (backgroundColour.length() - blockStart), height, palette, greyscale, blockColour, light );
        }
    }

    public static void drawString(
        @Nonnull VertexEmitter buffer, float x, float y,
        @Nonnull TextBuffer text, @Nonnull TextBuffer textColour, @Nullable TextBuffer backgroundColour,
        @Nonnull Palette palette, boolean greyscale, float leftMarginSize, float rightMarginSize, int light
    )
    {
        if( backgroundColour != null )
        {
            drawBackground( buffer, x, y, backgroundColour, palette, greyscale, leftMarginSize, rightMarginSize, FONT_HEIGHT, light );
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
            drawChar( buffer, x + i * FONT_WIDTH, y, index, r, g, b, light );
        }

    }

    public static void drawTerminalWithoutCursor(
        @Nonnull VertexEmitter buffer, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        Palette palette = terminal.getPalette();
        int height = terminal.getHeight();

        // Top and bottom margins
        drawBackground(
            buffer, x, y - topMarginSize,
            terminal.getBackgroundColourLine( 0 ), palette, greyscale,
            leftMarginSize, rightMarginSize, topMarginSize, FULL_BRIGHT_LIGHTMAP
        );

        drawBackground(
            buffer, x, y + height * FONT_HEIGHT,
            terminal.getBackgroundColourLine( height - 1 ), palette, greyscale,
            leftMarginSize, rightMarginSize, bottomMarginSize, FULL_BRIGHT_LIGHTMAP
        );

        // The main text
        for( int i = 0; i < height; i++ )
        {
            drawString(
                buffer, x, y + FixedWidthFontRenderer.FONT_HEIGHT * i,
                terminal.getLine( i ), terminal.getTextColourLine( i ), terminal.getBackgroundColourLine( i ),
                palette, greyscale, leftMarginSize, rightMarginSize, FULL_BRIGHT_LIGHTMAP
            );
        }
    }

    public static void drawCursor(
        @Nonnull VertexEmitter buffer, float x, float y,
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

            drawChar( buffer, x + cursorX * FONT_WIDTH, y + cursorY * FONT_HEIGHT, '_', r, g, b, FULL_BRIGHT_LIGHTMAP );
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

    public static void drawTerminal(
        @Nonnull Matrix4f transform, float x, float y, @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        MultiBufferSource.BufferSource renderer = MultiBufferSource.immediate( Tesselator.getInstance().getBuilder() );
        VertexConsumer buffer = renderer.getBuffer( RenderTypes.TERMINAL_WITH_DEPTH );
        drawTerminal(
            toVertexConsumer( transform, buffer ),
            x, y, terminal, greyscale, topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize
        );
        renderer.endBatch();
    }

    public static void drawEmptyTerminal( @Nonnull Matrix4f transform, @Nonnull MultiBufferSource bufferSource, float x, float y, float width, float height )
    {
        Colour colour = Colour.BLACK;
        drawQuad(
            toVertexConsumer( transform, bufferSource.getBuffer( RenderTypes.TERMINAL_WITH_DEPTH ) ),
            x, y, width, height, colour.getR(), colour.getG(), colour.getB(), FULL_BRIGHT_LIGHTMAP
        );
    }

    public static void drawEmptyTerminal( @Nonnull Matrix4f transform, float x, float y, float width, float height )
    {
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate( Tesselator.getInstance().getBuilder() );
        drawEmptyTerminal( transform, bufferSource, x, y, width, height );
        bufferSource.endBatch();
    }

    public static void drawBlocker( @Nonnull Matrix4f transform, @Nonnull MultiBufferSource source, float x, float y, float width, float height )
    {
        Colour colour = Colour.BLACK;
        drawQuad(
            toVertexConsumer( transform, source.getBuffer( RenderTypes.TERMINAL_BLOCKER ) ),
            x, y, width, height, colour.getR(), colour.getG(), colour.getB(), FULL_BRIGHT_LIGHTMAP
        );
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
     * Emit a single vertex.
     */
    @FunctionalInterface
    public interface VertexEmitter
    {
        void vertex( float x, float y, float z, float r, float g, float b, float u, float v, int light );
    }

    public static VertexEmitter toVertexConsumer( Matrix4f matrix, VertexConsumer consumer )
    {
        return ( float x, float y, float z, float r, float g, float b, float u, float v, int light ) ->
            consumer.vertex( matrix, x, y, z ).color( r, g, b, 1.0f ).uv( u, v ).uv2( light ).endVertex();
    }

    /**
     * An optimised vertex emitter which bypasses {@link VertexConsumer}. This allows us to emit vertices very quickly,
     * when using the VBO renderer with some limitations:
     * <ul>
     *     <li>No transformation matrix (not needed for VBOs).</li>
     *     <li>Only works with {@link DefaultVertexFormat#POSITION_COLOR_TEX}.</li>
     * </ul>
     *
     * @param buffer The buffer to emit to. This must have space for at least {@link #getVertexCount(Terminal)} vertices.
     * @return The emitter, ot be passed to the rendering functions.
     */
    public static VertexEmitter toByteBuffer( ByteBuffer buffer )
    {
        return ( float x, float y, float z, float r, float g, float b, float u, float v, int light ) -> buffer
            .putFloat( x ).putFloat( y ).putFloat( z )
            .put( (byte) (int) (r * 255) ).put( (byte) (int) (g * 255) ).put( (byte) (int) (b * 255) ).put( (byte) 255 )
            .putFloat( u ).putFloat( v );
    }
}
