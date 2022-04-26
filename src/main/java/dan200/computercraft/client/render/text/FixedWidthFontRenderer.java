/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render.text;

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
 *
 * <strong>IMPORTANT: </strong> When making changes to this class, please check if you need to make the same changes to
 * {@link DirectFixedWidthFontRenderer}.
 */
public final class FixedWidthFontRenderer
{
    public static final ResourceLocation FONT = new ResourceLocation( "computercraft", "textures/gui/term_font.png" );

    public static final int FONT_HEIGHT = 9;
    public static final int FONT_WIDTH = 6;
    static final float WIDTH = 256.0f;

    static final float BACKGROUND_START = (WIDTH - 6.0f) / WIDTH;
    static final float BACKGROUND_END = (WIDTH - 4.0f) / WIDTH;

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

    private static void drawChar( QuadEmitter emitter, float x, float y, int index, byte[] colour, int light )
    {
        // Short circuit to avoid the common case - the texture should be blank here after all.
        if( index == '\0' || index == ' ' ) return;

        int column = index % 16;
        int row = index / 16;

        int xStart = 1 + column * (FONT_WIDTH + 2);
        int yStart = 1 + row * (FONT_HEIGHT + 2);

        quad(
            emitter, x, y, x + FONT_WIDTH, y + FONT_HEIGHT, 0, colour,
            xStart / WIDTH, yStart / WIDTH, (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH, light
        );
    }

    public static void drawQuad( QuadEmitter emitter, float x, float y, float z, float width, float height, byte[] colour, int light )
    {
        quad( emitter, x, y, x + width, y + height, z, colour, BACKGROUND_START, BACKGROUND_START, BACKGROUND_END, BACKGROUND_END, light );
    }

    private static void drawQuad( QuadEmitter emitter, float x, float y, float width, float height, Palette palette, boolean greyscale, char colourIndex, int light )
    {
        byte[] colour = palette.getByteColour( getColour( colourIndex, Colour.BLACK ), greyscale );
        drawQuad( emitter, x, y, 0, width, height, colour, light );
    }

    private static void drawBackground(
        @Nonnull QuadEmitter emitter, float x, float y, @Nonnull TextBuffer backgroundColour, @Nonnull Palette palette, boolean greyscale,
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

    public static void drawString( @Nonnull QuadEmitter emitter, float x, float y, @Nonnull TextBuffer text, @Nonnull TextBuffer textColour, @Nonnull Palette palette, boolean greyscale, int light )
    {
        for( int i = 0; i < text.length(); i++ )
        {
            byte[] colour = palette.getByteColour( getColour( textColour.charAt( i ), Colour.BLACK ), greyscale );

            int index = text.charAt( i );
            if( index > 255 ) index = '?';
            drawChar( emitter, x + i * FONT_WIDTH, y, index, colour, light );
        }

    }

    public static void drawTerminalWithoutCursor(
        @Nonnull QuadEmitter emitter, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        Palette palette = terminal.getPalette();
        int height = terminal.getHeight();

        // Top and bottom margins
        drawBackground(
            emitter, x, y - topMarginSize, terminal.getBackgroundColourLine( 0 ), palette, greyscale,
            leftMarginSize, rightMarginSize, topMarginSize, FULL_BRIGHT_LIGHTMAP
        );

        drawBackground(
            emitter, x, y + height * FONT_HEIGHT, terminal.getBackgroundColourLine( height - 1 ), palette, greyscale,
            leftMarginSize, rightMarginSize, bottomMarginSize, FULL_BRIGHT_LIGHTMAP
        );

        // The main text
        for( int i = 0; i < height; i++ )
        {
            float rowY = y + FixedWidthFontRenderer.FONT_HEIGHT * i;
            drawBackground(
                emitter, x, rowY, terminal.getBackgroundColourLine( i ), palette, greyscale,
                leftMarginSize, rightMarginSize, FONT_HEIGHT, FULL_BRIGHT_LIGHTMAP
            );
            drawString(
                emitter, x, rowY, terminal.getLine( i ), terminal.getTextColourLine( i ),
                palette, greyscale, FULL_BRIGHT_LIGHTMAP
            );
        }
    }

    public static boolean isCursorVisible( Terminal terminal )
    {
        if( !terminal.getCursorBlink() ) return false;

        int cursorX = terminal.getCursorX();
        int cursorY = terminal.getCursorY();
        return cursorX >= 0 && cursorX < terminal.getWidth() && cursorY >= 0 && cursorY < terminal.getHeight();
    }

    public static void drawCursor( @Nonnull QuadEmitter emitter, float x, float y, @Nonnull Terminal terminal, boolean greyscale )
    {
        if( isCursorVisible( terminal ) && FrameInfo.getGlobalCursorBlink() )
        {
            byte[] colour = terminal.getPalette().getByteColour( 15 - terminal.getTextColour(), greyscale );
            drawChar( emitter, x + terminal.getCursorX() * FONT_WIDTH, y + terminal.getCursorY() * FONT_HEIGHT, '_', colour, FULL_BRIGHT_LIGHTMAP );
        }
    }

    public static void drawTerminal(
        @Nonnull QuadEmitter emitter, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        drawTerminalWithoutCursor( emitter, x, y, terminal, greyscale, topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize );
        drawCursor( emitter, x, y, terminal, greyscale );
    }

    public static void drawEmptyTerminal( @Nonnull QuadEmitter emitter, float x, float y, float width, float height )
    {
        drawQuad( emitter, x, y, 0, width, height, BLACK, FULL_BRIGHT_LIGHTMAP );
    }

    public static void drawBlocker( @Nonnull QuadEmitter emitter, float x, float y, float width, float height )
    {
        drawQuad( emitter, x, y, 0, width, height, BLACK, FULL_BRIGHT_LIGHTMAP );
    }

    public record QuadEmitter(Matrix4f matrix4f, VertexConsumer consumer) {}

    public static QuadEmitter toVertexConsumer( Matrix4f matrix, VertexConsumer consumer )
    {
        return new QuadEmitter( matrix, consumer );
    }

    private static void quad( QuadEmitter c, float x1, float y1, float x2, float y2, float z, byte[] rgba, float u1, float v1, float u2, float v2, int light )
    {
        var matrix = c.matrix4f();
        var consumer = c.consumer();
        byte r = rgba[0], g = rgba[1], b = rgba[2], a = rgba[3];

        consumer.vertex( matrix, x1, y1, z ).color( r, g, b, a ).uv( u1, v1 ).uv2( light ).endVertex();
        consumer.vertex( matrix, x1, y2, z ).color( r, g, b, a ).uv( u1, v2 ).uv2( light ).endVertex();
        consumer.vertex( matrix, x2, y2, z ).color( r, g, b, a ).uv( u2, v2 ).uv2( light ).endVertex();
        consumer.vertex( matrix, x2, y1, z ).color( r, g, b, a ).uv( u2, v1 ).uv2( light ).endVertex();
    }
}
