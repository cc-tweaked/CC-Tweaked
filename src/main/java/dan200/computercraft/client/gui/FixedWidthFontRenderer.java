/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FixedWidthFontRenderer
{
    private static final ResourceLocation FONT = new ResourceLocation( "computercraft", "textures/gui/term_font.png" );

    /**
     * Like {@link DefaultVertexFormats#POSITION_TEX_COLOR}, but flipped. This is backported from 1.15, hence the
     * custom format.
     */
    private static final VertexFormat POSITION_COLOR_TEX = new VertexFormat();

    static
    {
        POSITION_COLOR_TEX.addElement( DefaultVertexFormats.POSITION_3F );
        POSITION_COLOR_TEX.addElement( DefaultVertexFormats.COLOR_4UB );
        POSITION_COLOR_TEX.addElement( DefaultVertexFormats.TEX_2F );
    }

    public static final int FONT_HEIGHT = 9;
    public static final int FONT_WIDTH = 6;
    public static final float WIDTH = 256.0f;

    public static final float BACKGROUND_START = (WIDTH - 6.0f) / WIDTH;
    public static final float BACKGROUND_END = (WIDTH - 4.0f) / WIDTH;

    private FixedWidthFontRenderer()
    {
    }

    private static float toGreyscale( double[] rgb )
    {
        return (float) ((rgb[0] + rgb[1] + rgb[2]) / 3);
    }

    private static void drawChar( BufferBuilder buffer, float x, float y, int index, float r, float g, float b )
    {
        int column = index % 16;
        int row = index / 16;

        int xStart = 1 + column * (FONT_WIDTH + 2);
        int yStart = 1 + row * (FONT_HEIGHT + 2);

        buffer.pos( x, y, 0f ).color( r, g, b, 1.0f ).tex( xStart / WIDTH, yStart / WIDTH ).endVertex();
        buffer.pos( x, y + FONT_HEIGHT, 0f ).color( r, g, b, 1.0f ).tex( xStart / WIDTH, (yStart + FONT_HEIGHT) / WIDTH ).endVertex();
        buffer.pos( x + FONT_WIDTH, y, 0f ).color( r, g, b, 1.0f ).tex( (xStart + FONT_WIDTH) / WIDTH, yStart / WIDTH ).endVertex();
        buffer.pos( x + FONT_WIDTH, y, 0f ).color( r, g, b, 1.0f ).tex( (xStart + FONT_WIDTH) / WIDTH, yStart / WIDTH ).endVertex();
        buffer.pos( x, y + FONT_HEIGHT, 0f ).color( r, g, b, 1.0f ).tex( xStart / WIDTH, (yStart + FONT_HEIGHT) / WIDTH ).endVertex();
        buffer.pos( x + FONT_WIDTH, y + FONT_HEIGHT, 0f ).color( r, g, b, 1.0f ).tex( (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH ).endVertex();
    }

    private static void drawQuad( BufferBuilder buffer, float x, float y, float width, float height, float r, float g, float b )
    {
        buffer.pos( x, y, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_START, BACKGROUND_START ).endVertex();
        buffer.pos( x, y + height, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_START, BACKGROUND_END ).endVertex();
        buffer.pos( x + width, y, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_END, BACKGROUND_START ).endVertex();
        buffer.pos( x + width, y, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_END, BACKGROUND_START ).endVertex();
        buffer.pos( x, y + height, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_START, BACKGROUND_END ).endVertex();
        buffer.pos( x + width, y + height, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_END, BACKGROUND_END ).endVertex();
    }

    private static void drawBackground(
        @Nonnull BufferBuilder renderer, float x, float y,
        @Nonnull TextBuffer backgroundColour, @Nonnull Palette palette, boolean greyscale,
        float leftMarginSize, float rightMarginSize, float height
    )
    {
        if( leftMarginSize > 0 )
        {
            double[] colour = palette.getColour( 15 - "0123456789abcdef".indexOf( backgroundColour.charAt( 0 ) ) );
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

            drawQuad( renderer, x - leftMarginSize, y, leftMarginSize, height, r, g, b );
        }

        if( rightMarginSize > 0 )
        {
            double[] colour = palette.getColour( 15 - "0123456789abcdef".indexOf( backgroundColour.charAt( backgroundColour.length() - 1 ) ) );
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

            drawQuad( renderer, x + backgroundColour.length() * FONT_WIDTH, y, rightMarginSize, height, r, g, b );
        }

        for( int i = 0; i < backgroundColour.length(); i++ )
        {
            double[] colour = palette.getColour( 15 - "0123456789abcdef".indexOf( backgroundColour.charAt( i ) ) );
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

            drawQuad( renderer, x + i * FONT_WIDTH, y, FONT_WIDTH, height, r, g, b );
        }
    }

    public static void drawString(
        @Nonnull BufferBuilder renderer, float x, float y,
        @Nonnull TextBuffer text, @Nonnull TextBuffer textColour, @Nullable TextBuffer backgroundColour,
        @Nonnull Palette palette, boolean greyscale, float leftMarginSize, float rightMarginSize
    )
    {
        if( backgroundColour != null )
        {
            drawBackground( renderer, x, y, backgroundColour, palette, greyscale, leftMarginSize, rightMarginSize, FONT_HEIGHT );
        }

        for( int i = 0; i < text.length(); i++ )
        {
            double[] colour = palette.getColour( 15 - "0123456789abcdef".indexOf( textColour.charAt( i ) ) );
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
            drawChar( renderer, x + i * FONT_WIDTH, y, index, r, g, b );
        }

    }

    public static void drawString(
        float x, float y, @Nonnull TextBuffer text, @Nonnull TextBuffer textColour, @Nullable TextBuffer backgroundColour,
        @Nonnull Palette palette, boolean greyscale, float leftMarginSize, float rightMarginSize
    )
    {
        bindFont();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_TRIANGLES, POSITION_COLOR_TEX );
        drawString( buffer, x, y, text, textColour, backgroundColour, palette, greyscale, leftMarginSize, rightMarginSize );
        tessellator.draw();
    }

    public static void drawTerminalWithoutCursor(
        @Nonnull BufferBuilder buffer, float x, float y,
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
            leftMarginSize, rightMarginSize, topMarginSize
        );

        drawBackground(
            buffer, x, y + height * FONT_HEIGHT,
            terminal.getBackgroundColourLine( height - 1 ), palette, greyscale,
            leftMarginSize, rightMarginSize, bottomMarginSize
        );

        // The main text
        for( int i = 0; i < height; i++ )
        {
            drawString(
                buffer, x, y + FixedWidthFontRenderer.FONT_HEIGHT * i,
                terminal.getLine( i ), terminal.getTextColourLine( i ), terminal.getBackgroundColourLine( i ),
                palette, greyscale, leftMarginSize, rightMarginSize
            );
        }
    }

    public static void drawCursor(
        @Nonnull BufferBuilder buffer, float x, float y,
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

            drawChar( buffer, x + cursorX * FONT_WIDTH, y + cursorY * FONT_HEIGHT, '_', r, g, b );
        }
    }

    public static void drawTerminal(
        @Nonnull BufferBuilder buffer, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        drawTerminalWithoutCursor( buffer, x, y, terminal, greyscale, topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize );
        drawCursor( buffer, x, y, terminal, greyscale );
    }

    public static void drawTerminal(
        float x, float y, @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        bindFont();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_TRIANGLES, POSITION_COLOR_TEX );
        drawTerminal( buffer, x, y, terminal, greyscale, topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize );
        tessellator.draw();
    }

    public static void drawEmptyTerminal( float x, float y, float width, float height )
    {
        bindFont();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_TRIANGLES, POSITION_COLOR_TEX );

        Colour colour = Colour.Black;
        drawQuad( buffer, x, y, width, height, colour.getR(), colour.getG(), colour.getB() );

        tessellator.draw();
    }

    public static void drawBlocker( @Nonnull BufferBuilder buffer, float x, float y, float width, float height )
    {
        Colour colour = Colour.Black;
        drawQuad( buffer, x, y, width, height, colour.getR(), colour.getG(), colour.getB() );
    }

    public static void drawBlocker( float x, float y, float width, float height )
    {
        bindFont();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_TRIANGLES, POSITION_COLOR_TEX );
        drawBlocker( buffer, x, y, width, height );
        tessellator.draw();
    }

    public static void bindFont()
    {
        Minecraft.getMinecraft().getTextureManager().bindTexture( FONT );
        GlStateManager.glTexParameteri( GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP );

        GlStateManager.enableTexture2D();
    }
}
