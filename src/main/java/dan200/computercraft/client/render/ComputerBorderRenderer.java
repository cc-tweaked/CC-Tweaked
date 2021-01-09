/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

public class ComputerBorderRenderer
{
    public static final ResourceLocation BACKGROUND_NORMAL = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/corners_normal.png" );
    public static final ResourceLocation BACKGROUND_ADVANCED = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/corners_advanced.png" );
    public static final ResourceLocation BACKGROUND_COMMAND = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/corners_command.png" );
    public static final ResourceLocation BACKGROUND_COLOUR = new ResourceLocation( ComputerCraft.MOD_ID, "textures/gui/corners_colour.png" );

    private static final Matrix4f IDENTITY = new Matrix4f();

    static
    {
        IDENTITY.setIdentity();
    }

    /**
     * The margin between the terminal and its border.
     */
    public static final int MARGIN = 2;

    /**
     * The width of the terminal border.
     */
    public static final int BORDER = 12;

    private static final int CORNER_TOP_Y = 28;
    private static final int CORNER_BOTTOM_Y = CORNER_TOP_Y + BORDER;
    private static final int CORNER_LEFT_X = BORDER;
    private static final int CORNER_RIGHT_X = CORNER_LEFT_X + BORDER;
    private static final int BORDER_RIGHT_X = 36;
    private static final int GAP = 4;

    private static final float TEX_SCALE = 1 / 256.0f;

    private final Matrix4f transform;
    private final IVertexBuilder builder;
    private final int z;
    private final float r, g, b;

    public ComputerBorderRenderer( Matrix4f transform, IVertexBuilder builder, int z, float r, float g, float b )
    {
        this.transform = transform;
        this.builder = builder;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
    }


    @Nonnull
    public static ResourceLocation getTexture( @Nonnull ComputerFamily family )
    {
        switch( family )
        {
            case NORMAL:
            default:
                return BACKGROUND_NORMAL;
            case ADVANCED:
                return BACKGROUND_ADVANCED;
            case COMMAND:
                return BACKGROUND_COMMAND;
        }
    }

    public static void render( int x, int y, int z, int width, int height )
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin( GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX );

        render( IDENTITY, buffer, x, y, z, width, height );

        RenderSystem.enableAlphaTest();
        tessellator.end();
    }

    public static void render( Matrix4f transform, IVertexBuilder buffer, int x, int y, int z, int width, int height )
    {
        render( transform, buffer, x, y, z, width, height, 1, 1, 1 );
    }

    public static void render( Matrix4f transform, IVertexBuilder buffer, int x, int y, int z, int width, int height, float r, float g, float b )
    {
        render( transform, buffer, x, y, z, width, height, 0, r, g, b );
    }

    public static void render( Matrix4f transform, IVertexBuilder buffer, int x, int y, int z, int width, int height, int borderHeight, float r, float g, float b )
    {
        new ComputerBorderRenderer( transform, buffer, z, r, g, b ).doRender( x, y, width, height, borderHeight );
    }

    public void doRender( int x, int y, int width, int height, int bottomHeight )
    {
        int endX = x + width;
        int endY = y + height;

        // Vertical bars
        renderLine( x - BORDER, y, 0, CORNER_TOP_Y, BORDER, endY - y );
        renderLine( endX, y, BORDER_RIGHT_X, CORNER_TOP_Y, BORDER, endY - y );

        // Top bar
        renderLine( x, y - BORDER, 0, 0, endX - x, BORDER );
        renderCorner( x - BORDER, y - BORDER, CORNER_LEFT_X, CORNER_TOP_Y );
        renderCorner( endX, y - BORDER, CORNER_RIGHT_X, CORNER_TOP_Y );

        // Bottom bar. We allow for drawing a stretched version, which allows for additional elements (such as the
        // pocket computer's lights).
        if( bottomHeight <= 0 )
        {
            renderLine( x, endY, 0, BORDER, endX - x, BORDER );
            renderCorner( x - BORDER, endY, CORNER_LEFT_X, CORNER_BOTTOM_Y );
            renderCorner( endX, endY, CORNER_RIGHT_X, CORNER_BOTTOM_Y );
        }
        else
        {
            // Bottom left, middle, right. We do this in three portions: the top inner corners, an extended region for
            // lights, and then the bottom outer corners.
            renderTexture( x - BORDER, endY, CORNER_LEFT_X, CORNER_BOTTOM_Y, BORDER, BORDER / 2 );
            renderTexture( x, endY, 0, BORDER, width, BORDER / 2, BORDER, BORDER / 2 );
            renderTexture( endX, endY, CORNER_RIGHT_X, CORNER_BOTTOM_Y, BORDER, BORDER / 2 );

            renderTexture( x - BORDER, endY + BORDER / 2, CORNER_LEFT_X, CORNER_BOTTOM_Y + GAP, BORDER, bottomHeight, BORDER, GAP );
            renderTexture( x, endY + BORDER / 2, 0, BORDER + GAP, width, bottomHeight, BORDER, GAP );
            renderTexture( endX, endY + BORDER / 2, CORNER_RIGHT_X, CORNER_BOTTOM_Y + GAP, BORDER, bottomHeight, BORDER, GAP );

            renderTexture( x - BORDER, endY + bottomHeight + BORDER / 2, CORNER_LEFT_X, CORNER_BOTTOM_Y + BORDER / 2, BORDER, BORDER / 2 );
            renderTexture( x, endY + bottomHeight + BORDER / 2, 0, BORDER + BORDER / 2, width, BORDER / 2 );
            renderTexture( endX, endY + bottomHeight + BORDER / 2, CORNER_RIGHT_X, CORNER_BOTTOM_Y + BORDER / 2, BORDER, BORDER / 2 );
        }
    }

    private void renderCorner( int x, int y, int u, int v )
    {
        renderTexture( x, y, u, v, BORDER, BORDER, BORDER, BORDER );
    }

    private void renderLine( int x, int y, int u, int v, int width, int height )
    {
        renderTexture( x, y, u, v, width, height, BORDER, BORDER );
    }

    private void renderTexture( int x, int y, int u, int v, int width, int height )
    {
        renderTexture( x, y, u, v, width, height, width, height );
    }

    private void renderTexture( int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight )
    {
        builder.vertex( transform, x, y + height, z ).color( r, g, b, 1.0f ).uv( u * TEX_SCALE, (v + textureHeight) * TEX_SCALE ).endVertex();
        builder.vertex( transform, x + width, y + height, z ).color( r, g, b, 1.0f ).uv( (u + textureWidth) * TEX_SCALE, (v + textureHeight) * TEX_SCALE ).endVertex();
        builder.vertex( transform, x + width, y, z ).color( r, g, b, 1.0f ).uv( (u + textureWidth) * TEX_SCALE, v * TEX_SCALE ).endVertex();
        builder.vertex( transform, x, y, z ).color( r, g, b, 1.0f ).uv( u * TEX_SCALE, v * TEX_SCALE ).endVertex();
    }
}
