/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.TransformationMatrix;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class FixedWidthFontRenderer
{
    private static final Matrix4f IDENTITY = TransformationMatrix.identity().getMatrix();

    private static final ResourceLocation FONT = new ResourceLocation( "computercraft", "textures/gui/term_font.png" );

    public static final int FONT_HEIGHT = 9;
    public static final int FONT_WIDTH = 6;
    public static final float WIDTH = 256.0f;

    public static final float BACKGROUND_START = (WIDTH - 6.0f) / WIDTH;
    public static final float BACKGROUND_END = (WIDTH - 4.0f) / WIDTH;

    public static final RenderType TYPE = Type.MAIN;

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

    private static void drawChar( Matrix4f transform, IVertexBuilder buffer, float x, float y, int index, float r, float g, float b )
    {
        // Short circuit to avoid the common case - the texture should be blank here after all.
        if( index == '\0' || index == ' ' ) return;

        int column = index % 16;
        int row = index / 16;

        int xStart = 1 + column * (FONT_WIDTH + 2);
        int yStart = 1 + row * (FONT_HEIGHT + 2);

        buffer.pos( transform, x, y, 0f ).color( r, g, b, 1.0f ).tex( xStart / WIDTH, yStart / WIDTH ).endVertex();
        buffer.pos( transform, x, y + FONT_HEIGHT, 0f ).color( r, g, b, 1.0f ).tex( xStart / WIDTH, (yStart + FONT_HEIGHT) / WIDTH ).endVertex();
        buffer.pos( transform, x + FONT_WIDTH, y, 0f ).color( r, g, b, 1.0f ).tex( (xStart + FONT_WIDTH) / WIDTH, yStart / WIDTH ).endVertex();
        buffer.pos( transform, x + FONT_WIDTH, y, 0f ).color( r, g, b, 1.0f ).tex( (xStart + FONT_WIDTH) / WIDTH, yStart / WIDTH ).endVertex();
        buffer.pos( transform, x, y + FONT_HEIGHT, 0f ).color( r, g, b, 1.0f ).tex( xStart / WIDTH, (yStart + FONT_HEIGHT) / WIDTH ).endVertex();
        buffer.pos( transform, x + FONT_WIDTH, y + FONT_HEIGHT, 0f ).color( r, g, b, 1.0f ).tex( (xStart + FONT_WIDTH) / WIDTH, (yStart + FONT_HEIGHT) / WIDTH ).endVertex();
    }

    private static void drawQuad( Matrix4f transform, IVertexBuilder buffer, float x, float y, float width, float height, float r, float g, float b )
    {
        buffer.pos( transform, x, y, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_START, BACKGROUND_START ).endVertex();
        buffer.pos( transform, x, y + height, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_START, BACKGROUND_END ).endVertex();
        buffer.pos( transform, x + width, y, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_END, BACKGROUND_START ).endVertex();
        buffer.pos( transform, x + width, y, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_END, BACKGROUND_START ).endVertex();
        buffer.pos( transform, x, y + height, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_START, BACKGROUND_END ).endVertex();
        buffer.pos( transform, x + width, y + height, 0 ).color( r, g, b, 1.0f ).tex( BACKGROUND_END, BACKGROUND_END ).endVertex();
    }

    private static void drawQuad( Matrix4f transform, IVertexBuilder buffer, float x, float y, float width, float height, Palette palette, boolean greyscale, char colourIndex )
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

        drawQuad( transform, buffer, x, y, width, height, r, g, b );
    }

    private static void drawBackground(
        @Nonnull Matrix4f transform, @Nonnull IVertexBuilder renderer, float x, float y,
        @Nonnull TextBuffer backgroundColour, @Nonnull Palette palette, boolean greyscale,
        float leftMarginSize, float rightMarginSize, float height
    )
    {
        if( leftMarginSize > 0 )
        {
            drawQuad( transform, renderer, x - leftMarginSize, y, leftMarginSize, height, palette, greyscale, backgroundColour.charAt( 0 ) );
        }

        if( rightMarginSize > 0 )
        {
            drawQuad( transform, renderer, x + backgroundColour.length() * FONT_WIDTH, y, rightMarginSize, height, palette, greyscale, backgroundColour.charAt( backgroundColour.length() - 1 ) );
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
                drawQuad( transform, renderer, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (i - blockStart), height, palette, greyscale, blockColour );
            }

            blockColour = colourIndex;
            blockStart = i;
        }

        if( blockColour != '\0' )
        {
            drawQuad( transform, renderer, x + blockStart * FONT_WIDTH, y, FONT_WIDTH * (backgroundColour.length() - blockStart), height, palette, greyscale, blockColour );
        }
    }

    public static void drawString(
        @Nonnull Matrix4f transform, @Nonnull IVertexBuilder renderer, float x, float y,
        @Nonnull TextBuffer text, @Nonnull TextBuffer textColour, @Nullable TextBuffer backgroundColour,
        @Nonnull Palette palette, boolean greyscale, float leftMarginSize, float rightMarginSize
    )
    {
        if( backgroundColour != null )
        {
            drawBackground( transform, renderer, x, y, backgroundColour, palette, greyscale, leftMarginSize, rightMarginSize, FONT_HEIGHT );
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
            drawChar( transform, renderer, x + i * FONT_WIDTH, y, index, r, g, b );
        }

    }

    public static void drawString(
        float x, float y, @Nonnull TextBuffer text, @Nonnull TextBuffer textColour, @Nullable TextBuffer backgroundColour,
        @Nonnull Palette palette, boolean greyscale, float leftMarginSize, float rightMarginSize
    )
    {
        bindFont();

        IRenderTypeBuffer.Impl renderer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        drawString( IDENTITY, ((IRenderTypeBuffer) renderer).getBuffer( TYPE ), x, y, text, textColour, backgroundColour, palette, greyscale, leftMarginSize, rightMarginSize );
        renderer.finish();
    }

    public static void drawTerminalWithoutCursor(
        @Nonnull Matrix4f transform, @Nonnull IVertexBuilder buffer, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        Palette palette = terminal.getPalette();
        int height = terminal.getHeight();

        // Top and bottom margins
        drawBackground(
            transform, buffer, x, y - topMarginSize,
            terminal.getBackgroundColourLine( 0 ), palette, greyscale,
            leftMarginSize, rightMarginSize, topMarginSize
        );

        drawBackground(
            transform, buffer, x, y + height * FONT_HEIGHT,
            terminal.getBackgroundColourLine( height - 1 ), palette, greyscale,
            leftMarginSize, rightMarginSize, bottomMarginSize
        );

        // The main text
        for( int i = 0; i < height; i++ )
        {
            drawString(
                transform, buffer, x, y + FixedWidthFontRenderer.FONT_HEIGHT * i,
                terminal.getLine( i ), terminal.getTextColourLine( i ), terminal.getBackgroundColourLine( i ),
                palette, greyscale, leftMarginSize, rightMarginSize
            );
        }
    }

    public static void drawCursor(
        @Nonnull Matrix4f transform, @Nonnull IVertexBuilder buffer, float x, float y,
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

            drawChar( transform, buffer, x + cursorX * FONT_WIDTH, y + cursorY * FONT_HEIGHT, '_', r, g, b );
        }
    }

    public static void drawTerminal(
        @Nonnull Matrix4f transform, @Nonnull IVertexBuilder buffer, float x, float y,
        @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        drawTerminalWithoutCursor( transform, buffer, x, y, terminal, greyscale, topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize );
        drawCursor( transform, buffer, x, y, terminal, greyscale );
    }

    public static void drawTerminal(
        @Nonnull Matrix4f transform, float x, float y, @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        bindFont();

        IRenderTypeBuffer.Impl renderer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        IVertexBuilder buffer = renderer.getBuffer( TYPE );
        drawTerminal( transform, buffer, x, y, terminal, greyscale, topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize );
        renderer.finish( TYPE );
    }

    public static void drawTerminal(
        float x, float y, @Nonnull Terminal terminal, boolean greyscale,
        float topMarginSize, float bottomMarginSize, float leftMarginSize, float rightMarginSize
    )
    {
        drawTerminal( IDENTITY, x, y, terminal, greyscale, topMarginSize, bottomMarginSize, leftMarginSize, rightMarginSize );
    }

    public static void drawEmptyTerminal( @Nonnull Matrix4f transform, @Nonnull IRenderTypeBuffer renderer, float x, float y, float width, float height )
    {
        Colour colour = Colour.BLACK;
        drawQuad( transform, renderer.getBuffer( TYPE ), x, y, width, height, colour.getR(), colour.getG(), colour.getB() );
    }

    public static void drawEmptyTerminal( @Nonnull Matrix4f transform, float x, float y, float width, float height )
    {
        bindFont();

        IRenderTypeBuffer.Impl renderer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
        drawEmptyTerminal( transform, renderer, x, y, width, height );
        renderer.finish();
    }

    public static void drawEmptyTerminal( float x, float y, float width, float height )
    {
        drawEmptyTerminal( IDENTITY, x, y, width, height );
    }

    public static void drawBlocker( @Nonnull Matrix4f transform, @Nonnull IRenderTypeBuffer renderer, float x, float y, float width, float height )
    {
        Colour colour = Colour.BLACK;
        drawQuad( transform, renderer.getBuffer( Type.BLOCKER ), x, y, width, height, colour.getR(), colour.getG(), colour.getB() );
    }

    private static void bindFont()
    {
        Minecraft.getInstance().getTextureManager().bindTexture( FONT );
        RenderSystem.texParameter( GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP );
    }

    private static final class Type extends RenderState
    {
        private static final int GL_MODE = GL11.GL_TRIANGLES;

        private static final VertexFormat FORMAT = DefaultVertexFormats.POSITION_COLOR_TEX;

        static final RenderType MAIN = RenderType.makeType(
            "terminal_font", FORMAT, GL_MODE, 1024,
            false, false, // useDelegate, needsSorting
            RenderType.State.getBuilder()
                .texture( new RenderState.TextureState( FONT, false, false ) ) // blur, minimap
                .alpha( DEFAULT_ALPHA )
                .lightmap( LIGHTMAP_DISABLED )
                .writeMask( COLOR_WRITE )
                .build( false )
        );

        static final RenderType BLOCKER = RenderType.makeType(
            "terminal_blocker", FORMAT, GL_MODE, 256,
            false, false, // useDelegate, needsSorting
            RenderType.State.getBuilder()
                .texture( new RenderState.TextureState( FONT, false, false ) ) // blur, minimap
                .alpha( DEFAULT_ALPHA )
                .writeMask( DEPTH_WRITE )
                .lightmap( LIGHTMAP_DISABLED )
                .build( false )
        );

        private Type( String name, Runnable setup, Runnable destroy )
        {
            super( name, setup, destroy );
        }
    }
}
