/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.google.common.base.Strings;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.io.InputStream;
import java.nio.FloatBuffer;

class MonitorTextureBufferShader
{
    static final int TEXTURE_INDEX = GL13.GL_TEXTURE3;

    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer( 16 );
    private static final FloatBuffer PALETTE_BUFFER = BufferUtils.createFloatBuffer( 16 * 3 );

    private static int uniformMv;

    private static int uniformFont;
    private static int uniformWidth;
    private static int uniformHeight;
    private static int uniformTbo;
    private static int uniformPalette;

    private static boolean initialised;
    private static boolean ok;
    private static int program;

    static void setupUniform( Matrix4f transform, int width, int height, Palette palette, boolean greyscale )
    {
        MATRIX_BUFFER.rewind();
        transform.write( MATRIX_BUFFER );
        MATRIX_BUFFER.rewind();
        RenderSystem.glUniformMatrix4( uniformMv, false, MATRIX_BUFFER );

        RenderSystem.glUniform1i( uniformWidth, width );
        RenderSystem.glUniform1i( uniformHeight, height );

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
        RenderSystem.glUniform3( uniformPalette, PALETTE_BUFFER );
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
            RenderSystem.glUniform1i( uniformFont, 0 );
            RenderSystem.glUniform1i( uniformTbo, TEXTURE_INDEX - GL13.GL_TEXTURE0 );
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
            GL20.glBindAttribLocation( program, 0, "v_pos" );

            GlStateManager.linkProgram( program );
            boolean ok = GlStateManager.getProgram( program, GL20.GL_LINK_STATUS ) != 0;
            String log = GlStateManager.getProgramInfoLog( program, Short.MAX_VALUE ).trim();
            if( !Strings.isNullOrEmpty( log ) )
            {
                ComputerCraft.log.warn( "Problems when linking monitor shader: {}", log );
            }

            GL20.glDetachShader( program, vertexShader );
            GL20.glDetachShader( program, fragmentShader );
            GlStateManager.deleteShader( vertexShader );
            GlStateManager.deleteShader( fragmentShader );

            if( !ok ) return false;

            uniformMv = getUniformLocation( program, "u_mv" );
            uniformFont = getUniformLocation( program, "u_font" );
            uniformWidth = getUniformLocation( program, "u_width" );
            uniformHeight = getUniformLocation( program, "u_height" );
            uniformTbo = getUniformLocation( program, "u_tbo" );
            uniformPalette = getUniformLocation( program, "u_palette" );

            ComputerCraft.log.info( "Loaded monitor shader." );
            return true;
        }
        catch( Exception e )
        {
            ComputerCraft.log.error( "Cannot load monitor shaders", e );
            return false;
        }
    }

    private static int loadShader( int kind, String path )
    {
        InputStream stream = TileEntityMonitorRenderer.class.getClassLoader().getResourceAsStream( path );
        if( stream == null ) throw new IllegalArgumentException( "Cannot find " + path );
        String contents = TextureUtil.readResourceAsString( stream );

        int shader = GlStateManager.createShader( kind );

        GlStateManager.shaderSource( shader, contents );
        GlStateManager.compileShader( shader );

        boolean ok = GlStateManager.getShader( shader, GL20.GL_COMPILE_STATUS ) != 0;
        String log = GlStateManager.getShaderInfoLog( shader, Short.MAX_VALUE ).trim();
        if( !Strings.isNullOrEmpty( log ) )
        {
            ComputerCraft.log.warn( "Problems when loading monitor shader {}: {}", path, log );
        }

        if( !ok ) throw new IllegalStateException( "Cannot compile shader " + path );
        return shader;
    }

    private static int getUniformLocation( int program, String name )
    {
        int uniform = GlStateManager.getUniformLocation( program, name );
        if( uniform == -1 ) throw new IllegalStateException( "Cannot find uniform " + name );
        return uniform;
    }
}
