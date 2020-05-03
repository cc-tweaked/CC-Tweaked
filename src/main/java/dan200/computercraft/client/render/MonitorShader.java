/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.io.InputStream;
import java.nio.FloatBuffer;

class MonitorShader
{
    static final int TEXTURE_INDEX = GL13.GL_TEXTURE3;

    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer( 16 );
    private static final FloatBuffer PALETTE_BUFFER = BufferUtils.createFloatBuffer( 16 * 3 );

    private static final int UNIFORM_MV = 0;
    private static final int UNIFORM_P = 1;

    private static final int UNIFORM_FONT = 2;
    private static final int UNIFORM_WIDTH = 3;
    private static final int UNIFORM_HEIGHT = 4;
    private static final int UNIFORM_TBO = 5;
    private static final int UNIFORM_PALETTE = 6;

    private static boolean initialised;
    private static boolean ok;
    private static int program;

    static void setupUniform( Matrix4f transform, int width, int height, Palette palette, boolean greyscale )
    {
        MATRIX_BUFFER.rewind();
        transform.write( MATRIX_BUFFER );
        MATRIX_BUFFER.rewind();
        RenderSystem.glUniformMatrix4( UNIFORM_MV, false, MATRIX_BUFFER );

        // TODO: Cache this?
        MATRIX_BUFFER.rewind();
        GL11.glGetFloatv( GL11.GL_PROJECTION_MATRIX, MATRIX_BUFFER );
        MATRIX_BUFFER.rewind();
        RenderSystem.glUniformMatrix4( UNIFORM_P, false, MATRIX_BUFFER );

        RenderSystem.glUniform1i( UNIFORM_WIDTH, width );
        RenderSystem.glUniform1i( UNIFORM_HEIGHT, height );

        // TODO: Cache this? Maybe??
        PALETTE_BUFFER.rewind();
        for( int i = 0; i < 16; i++ )
        {
            double[] colour = palette.getColour( i );
            if( greyscale )
            {
                float f = FixedWidthFontRenderer.toGreyscale( colour );
                PALETTE_BUFFER.put( f ).put( f ).put( f );
            }
            else
            {
                PALETTE_BUFFER.put( (float) colour[0] ).put( (float) colour[1] ).put( (float) colour[2] );
            }
        }
        PALETTE_BUFFER.flip();
        RenderSystem.glUniform3( UNIFORM_PALETTE, PALETTE_BUFFER );
    }

    static boolean use()
    {
        if( initialised )
        {
            if( ok ) GlStateManager.useProgram( program );
            return ok;
        }

        if( ok = load() )
        {
            GL20.glUseProgram( program );
            RenderSystem.glUniform1i( MonitorShader.UNIFORM_FONT, 0 );
            RenderSystem.glUniform1i( MonitorShader.UNIFORM_TBO, TEXTURE_INDEX - GL13.GL_TEXTURE0 );
        }

        return ok;
    }

    private static boolean load()
    {
        initialised = true;

        try
        {
            int vertexShader = loadShader( GL20.GL_VERTEX_SHADER, "assets/computercraft/shaders/monitor.vert" );
            int fragmentShader = loadShader( GL20.GL_FRAGMENT_SHADER, "assets/computercraft/shaders/monitor.frag" );

            program = GlStateManager.createProgram();
            GlStateManager.attachShader( program, vertexShader );
            GlStateManager.attachShader( program, fragmentShader );
            GlStateManager.linkProgram( program );

            int i = GlStateManager.getProgram( program, GL20.GL_LINK_STATUS );
            if( i == 0 )
            {
                ComputerCraft.log.warn( "Error encountered when linking monitor shaders." );
                ComputerCraft.log.warn( GlStateManager.getProgramInfoLog( program, Short.MAX_VALUE ) );
            }

            return true;
        }
        catch( Exception e )
        {
            ComputerCraft.log.error( "Cannot load monitor shaders", e );
            return false;
        }

        /*
        GL20.glDetachShader( vertexShader, program );
        GL20.glDetachShader( fragmentShader, program );
        GlStateManager.deleteShader( vertexShader );
        GlStateManager.deleteShader( fragmentShader );
        */
    }

    private static int loadShader( int kind, String path )
    {
        InputStream stream = TileEntityMonitorRenderer.class.getClassLoader().getResourceAsStream( path );
        if( stream == null ) throw new IllegalArgumentException( "Cannot find " + path );
        String contents = TextureUtil.readResourceAsString( stream );

        int shader = GlStateManager.createShader( kind );

        GlStateManager.shaderSource( shader, contents );
        GlStateManager.compileShader( shader );
        if( GlStateManager.getShader( shader, GL20.GL_COMPILE_STATUS ) == 0 )
        {
            String s = StringUtils.trim( GlStateManager.getShaderInfoLog( shader, Short.MAX_VALUE ) );
            ComputerCraft.log.error( "Could not compile shader {}: {}", path, s );
            throw new IllegalStateException( "Cannot compile shader " + path );
        }

        return shader;
    }

}
