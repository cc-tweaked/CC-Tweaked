/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;

public class ComputerBorderRenderer
{
    public static final Identifier BACKGROUND_NORMAL = new Identifier( ComputerCraft.MOD_ID, "textures/gui/corners_normal.png" );
    public static final Identifier BACKGROUND_ADVANCED = new Identifier( ComputerCraft.MOD_ID, "textures/gui/corners_advanced.png" );
    public static final Identifier BACKGROUND_COMMAND = new Identifier( ComputerCraft.MOD_ID, "textures/gui/corners_command.png" );
    public static final Identifier BACKGROUND_COLOUR = new Identifier( ComputerCraft.MOD_ID, "textures/gui/corners_colour.png" );
    /**
     * The margin between the terminal and its border.
     */
    public static final int MARGIN = 2;
    /**
     * The width of the terminal border.
     */
    public static final int BORDER = 12;
    private static final Matrix4f IDENTITY = new Matrix4f();
    private static final int CORNER_TOP_Y = 28;
    private static final int CORNER_BOTTOM_Y = CORNER_TOP_Y + BORDER;
    private static final int CORNER_LEFT_X = BORDER;
    private static final int CORNER_RIGHT_X = CORNER_LEFT_X + BORDER;
    private static final int BORDER_RIGHT_X = 36;
    private static final int LIGHT_BORDER_Y = 56;
    private static final int LIGHT_CORNER_Y = 80;

    public static final int LIGHT_HEIGHT = 8;
    private static final float TEX_SCALE = 1 / 256.0f;

    static
    {
        IDENTITY.loadIdentity();
    }

    private final Matrix4f transform;
    private final VertexConsumer builder;
    private final int z;
    private final float r, g, b;

    public ComputerBorderRenderer( Matrix4f transform, VertexConsumer builder, int z, float r, float g, float b )
    {
        this.transform = transform;
        this.builder = builder;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
    }


    @Nonnull
    public static Identifier getTexture( @Nonnull ComputerFamily family )
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
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin( GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE );

        render( IDENTITY, buffer, x, y, z, width, height );

        RenderSystem.enableAlphaTest();
        tessellator.draw();
    }

    public static void render( Matrix4f transform, VertexConsumer buffer, int x, int y, int z, int width, int height )
    {
        render( transform, buffer, x, y, z, width, height, 1, 1, 1 );
    }

    public static void render( Matrix4f transform, VertexConsumer buffer, int x, int y, int z, int width, int height, float r, float g, float b )
    {
        render( transform, buffer, x, y, z, width, height, false, r, g, b );
    }

    public static void render( Matrix4f transform, VertexConsumer buffer, int x, int y, int z, int width, int height, boolean withLight, float r, float g, float b )
    {
        new ComputerBorderRenderer( transform, buffer, z, r, g, b ).doRender( x, y, width, height, withLight );
    }

    public void doRender( int x, int y, int width, int height, boolean withLight )
    {
        int endX = x + width;
        int endY = y + height;

        // Vertical bars
        this.renderLine( x - BORDER, y, 0, CORNER_TOP_Y, BORDER, endY - y );
        this.renderLine( endX, y, BORDER_RIGHT_X, CORNER_TOP_Y, BORDER, endY - y );

        // Top bar
        this.renderLine( x, y - BORDER, 0, 0, endX - x, BORDER );
        this.renderCorner( x - BORDER, y - BORDER, CORNER_LEFT_X, CORNER_TOP_Y );
        this.renderCorner( endX, y - BORDER, CORNER_RIGHT_X, CORNER_TOP_Y );

        // Bottom bar. We allow for drawing a stretched version, which allows for additional elements (such as the
        // pocket computer's lights).
        if( withLight )
        {
            renderTexture( x, endY, 0, LIGHT_BORDER_Y, endX - x, BORDER + LIGHT_HEIGHT, BORDER, BORDER + LIGHT_HEIGHT );
            renderTexture( x - BORDER, endY, CORNER_LEFT_X, LIGHT_CORNER_Y, BORDER, BORDER + LIGHT_HEIGHT );
            renderTexture( endX, endY, CORNER_RIGHT_X, LIGHT_CORNER_Y, BORDER, BORDER + LIGHT_HEIGHT );
        }
        else
        {
            this.renderLine( x, endY, 0, BORDER, endX - x, BORDER );
            this.renderCorner( x - BORDER, endY, CORNER_LEFT_X, CORNER_BOTTOM_Y );
            this.renderCorner( endX, endY, CORNER_RIGHT_X, CORNER_BOTTOM_Y );
        }
    }

    private void renderLine( int x, int y, int u, int v, int width, int height )
    {
        this.renderTexture( x, y, u, v, width, height, BORDER, BORDER );
    }

    private void renderCorner( int x, int y, int u, int v )
    {
        this.renderTexture( x, y, u, v, BORDER, BORDER, BORDER, BORDER );
    }

    private void renderTexture( int x, int y, int u, int v, int width, int height )
    {
        this.renderTexture( x, y, u, v, width, height, width, height );
    }

    private void renderTexture( int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight )
    {
        this.builder.vertex( this.transform, x, y + height, this.z )
            .color( this.r, this.g, this.b, 1.0f )
            .texture( u * TEX_SCALE, (v + textureHeight) * TEX_SCALE )
            .next();
        this.builder.vertex( this.transform, x + width, y + height, this.z )
            .color( this.r, this.g, this.b, 1.0f )
            .texture( (u + textureWidth) * TEX_SCALE, (v + textureHeight) * TEX_SCALE )
            .next();
        this.builder.vertex( this.transform, x + width, y, this.z )
            .color( this.r, this.g, this.b, 1.0f )
            .texture( (u + textureWidth) * TEX_SCALE, v * TEX_SCALE )
            .next();
        this.builder.vertex( this.transform, x, y, this.z )
            .color( this.r, this.g, this.b, 1.0f )
            .texture( u * TEX_SCALE, v * TEX_SCALE )
            .next();
    }
}
