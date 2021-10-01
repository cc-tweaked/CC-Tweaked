/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import net.minecraft.client.render.*;
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
    public static final int TEX_SIZE = 256;
    private static final float TEX_SCALE = 1 / (float) TEX_SIZE;

    static
    {
        IDENTITY.loadIdentity();
    }

    private final Matrix4f transform;
    private final VertexConsumer builder;
    private final int z;
    private final float r, g, b;

    private final int light;

    public ComputerBorderRenderer( Matrix4f transform, VertexConsumer builder, int z, int light, float r, float g, float b )
    {
        this.transform = transform;
        this.builder = builder;
        this.z = z;
        this.light = light;
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

    public static void render(Identifier location,  int x, int y, int z, int light, int width, int height )
    {
        VertexConsumerProvider.Immediate source = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        render( IDENTITY, source.getBuffer(RenderLayer.getText(location)), x, y, z, light, width, height, false, 1, 1, 1 );
        source.draw();
    }


    public static void render( Matrix4f transform, VertexConsumer buffer, int x, int y, int z, int light, int width, int height, boolean withLight, float r, float g, float b )
    {
        new ComputerBorderRenderer( transform, buffer, z, light, r, g, b ).doRender( x, y, width, height, withLight );
    }

    public void doRender( int x, int y, int width, int height, boolean withLight )
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
        if( withLight )
        {
            renderTexture( x, endY, 0, LIGHT_BORDER_Y, endX - x, BORDER + LIGHT_HEIGHT, BORDER, BORDER + LIGHT_HEIGHT );
            renderTexture( x - BORDER, endY, CORNER_LEFT_X, LIGHT_CORNER_Y, BORDER, BORDER + LIGHT_HEIGHT );
            renderTexture( endX, endY, CORNER_RIGHT_X, LIGHT_CORNER_Y, BORDER, BORDER + LIGHT_HEIGHT );
        }
        else
        {
            renderLine( x, endY, 0, BORDER, endX - x, BORDER );
            renderCorner( x - BORDER, endY, CORNER_LEFT_X, CORNER_BOTTOM_Y );
            renderCorner( endX, endY, CORNER_RIGHT_X, CORNER_BOTTOM_Y );
        }
    }

    private void renderLine( int x, int y, int u, int v, int width, int height )
    {
        renderTexture( x, y, u, v, width, height, BORDER, BORDER );
    }

    private void renderCorner( int x, int y, int u, int v )
    {
        renderTexture( x, y, u, v, BORDER, BORDER, BORDER, BORDER );
    }

    private void renderTexture( int x, int y, int u, int v, int width, int height )
    {
        renderTexture( x, y, u, v, width, height, width, height );
    }

    private void renderTexture( int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight )
    {
        builder.vertex( transform, x, y + height, z )
            .color( r, g, b, 1.0f )
            .texture( u * TEX_SCALE, (v + textureHeight) * TEX_SCALE )
            .light(light)
            .next();
        builder.vertex( transform, x + width, y + height, z )
            .color( r, g, b, 1.0f )
            .texture( (u + textureWidth) * TEX_SCALE, (v + textureHeight) * TEX_SCALE )
            .light(light)
            .next();
        builder.vertex( transform, x + width, y, z )
            .color( r, g, b, 1.0f )
            .texture( (u + textureWidth) * TEX_SCALE, v * TEX_SCALE )
            .light(light)
            .next();
        builder.vertex( transform, x, y, z )
            .color( r, g, b, 1.0f )
            .texture( u * TEX_SCALE, v * TEX_SCALE )
            .light(light)
            .next();
    }
}
