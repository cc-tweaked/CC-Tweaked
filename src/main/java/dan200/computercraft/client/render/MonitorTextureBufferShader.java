/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import com.google.common.base.Strings;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.renderer.OpenGlHelper;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class MonitorTextureBufferShader
{
    static final int TEXTURE_INDEX = GL13.GL_TEXTURE3;

    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer( 16 );

    private static int uniformMv;
    private static int uniformP;

    private static int uniformFont;
    private static int uniformTbo;

    private static boolean initialised;
    private static boolean ok;
    private static int program;

    private static int uniformBuffer = -1;

    private static ByteBuffer uboBuffer = null;

    private static final String[] UNIFORM_NAMES = new String[] { "u_width", "u_height", "u_palette" };
    private static final IntBuffer UNIFORM_OFFSETS = BufferUtils.createIntBuffer( UNIFORM_NAMES.length );
    private static final IntBuffer UNIFORM_STRIDES = BufferUtils.createIntBuffer( UNIFORM_NAMES.length );

    static void setupUniform( int width, int height, Palette palette, boolean greyscale )
    {
        MATRIX_BUFFER.rewind();
        GL11.glGetFloat( GL11.GL_MODELVIEW_MATRIX, MATRIX_BUFFER );
        MATRIX_BUFFER.rewind();
        OpenGlHelper.glUniformMatrix4( uniformMv, false, MATRIX_BUFFER );

        MATRIX_BUFFER.rewind();
        GL11.glGetFloat( GL11.GL_PROJECTION_MATRIX, MATRIX_BUFFER );
        MATRIX_BUFFER.rewind();
        OpenGlHelper.glUniformMatrix4( uniformP, false, MATRIX_BUFFER );

        GL15.glBindBuffer( GL31.GL_UNIFORM_BUFFER, uniformBuffer );
        uboBuffer = GL15.glMapBuffer( GL31.GL_UNIFORM_BUFFER, GL15.GL_WRITE_ONLY, uboBuffer );
        uboBuffer.putInt( UNIFORM_OFFSETS.get( 0 ), width );
        uboBuffer.putInt( UNIFORM_OFFSETS.get( 1 ), height );

        for( int i = 0; i < 16; i++ )
        {
            uboBuffer.position( UNIFORM_OFFSETS.get( 2 ) + UNIFORM_STRIDES.get( 2 ) * i );

            double[] colour = palette.getColour( i );
            if( greyscale )
            {
                float f = FixedWidthFontRenderer.toGreyscale( colour );
                uboBuffer.putFloat( f ).putFloat( f ).putFloat( f );
            }
            else
            {
                uboBuffer.putFloat( (float) colour[0] ).putFloat( (float) colour[1] ).putFloat( (float) colour[2] );
            }
        }

        uboBuffer.flip();
        GL15.glUnmapBuffer( GL31.GL_UNIFORM_BUFFER );
    }

    static boolean use()
    {
        if( initialised )
        {
            if( ok ) OpenGlHelper.glUseProgram( program );
            return ok;
        }

        if( ok = load() )
        {
            OpenGlHelper.glUseProgram( program );
            OpenGlHelper.glUniform1i( uniformFont, 0 );
            OpenGlHelper.glUniform1i( uniformTbo, TEXTURE_INDEX - GL13.GL_TEXTURE0 );
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

            program = OpenGlHelper.glCreateProgram();
            OpenGlHelper.glAttachShader( program, vertexShader );
            OpenGlHelper.glAttachShader( program, fragmentShader );
            GL20.glBindAttribLocation( program, 0, "v_pos" );

            OpenGlHelper.glLinkProgram( program );
            boolean ok = OpenGlHelper.glGetProgrami( program, GL20.GL_LINK_STATUS ) != 0;
            String log = OpenGlHelper.glGetProgramInfoLog( program, Short.MAX_VALUE ).trim();
            if( !Strings.isNullOrEmpty( log ) )
            {
                ComputerCraft.log.warn( "Problems when linking monitor shader: {}", log );
            }

            GL20.glDetachShader( program, vertexShader );
            GL20.glDetachShader( program, fragmentShader );
            OpenGlHelper.glDeleteShader( vertexShader );
            OpenGlHelper.glDeleteShader( fragmentShader );

            if( !ok ) return false;

            uniformMv = getUniformLocation( program, "u_mv" );
            uniformP = getUniformLocation( program, "u_p" );

            uniformFont = getUniformLocation( program, "u_font" );
            uniformTbo = getUniformLocation( program, "u_tbo" );

            int monitorDataIndex = GL31.glGetUniformBlockIndex( program, "MonitorData" );
            int uniformBufferSize = GL31.glGetActiveUniformBlocki( program, monitorDataIndex, GL31.GL_UNIFORM_BLOCK_DATA_SIZE );
            uboBuffer = BufferUtils.createByteBuffer( uniformBufferSize );

            uniformBuffer = GL15.glGenBuffers();
            GL15.glBindBuffer( GL31.GL_UNIFORM_BUFFER, uniformBuffer );
            GL15.glBufferData( GL31.GL_UNIFORM_BUFFER, uniformBufferSize, GL15.GL_DYNAMIC_DRAW );

            IntBuffer indices = BufferUtils.createIntBuffer( UNIFORM_NAMES.length );
            GL31.glGetUniformIndices( program, UNIFORM_NAMES, indices );

            GL31.glGetActiveUniforms( program, indices, GL31.GL_UNIFORM_OFFSET, UNIFORM_OFFSETS );
            GL31.glGetActiveUniforms( program, indices, GL31.GL_UNIFORM_ARRAY_STRIDE, UNIFORM_STRIDES );

            GL30.glBindBufferBase( GL31.GL_UNIFORM_BUFFER, monitorDataIndex, uniformBuffer );

            ComputerCraft.log.info( "Loaded monitor shader." );
            return true;
        }
        catch( Exception e )
        {
            ComputerCraft.log.error( "Cannot load monitor shaders", e );
            return false;
        }
    }

    private static int loadShader( int kind, String path ) throws IOException
    {
        InputStream stream = TileEntityMonitorRenderer.class.getClassLoader().getResourceAsStream( path );
        if( stream == null ) throw new IllegalArgumentException( "Cannot find " + path );
        byte[] contents = IOUtils.toByteArray( new BufferedInputStream( stream ) );
        ByteBuffer buffer = BufferUtils.createByteBuffer( contents.length );
        buffer.put( contents );
        buffer.position( 0 );

        int shader = OpenGlHelper.glCreateShader( kind );

        OpenGlHelper.glShaderSource( shader, buffer );
        OpenGlHelper.glCompileShader( shader );

        boolean ok = OpenGlHelper.glGetShaderi( shader, GL20.GL_COMPILE_STATUS ) != 0;
        String log = OpenGlHelper.glGetShaderInfoLog( shader, Short.MAX_VALUE ).trim();
        if( !Strings.isNullOrEmpty( log ) )
        {
            ComputerCraft.log.warn( "Problems when loading monitor shader {}: {}", path, log );
        }

        if( !ok ) throw new IllegalStateException( "Cannot compile shader " + path );
        return shader;
    }

    private static int getUniformLocation( int program, String name )
    {
        int uniform = OpenGlHelper.glGetUniformLocation( program, name );
        if( uniform == -1 ) throw new IllegalStateException( "Cannot find uniform " + name );
        return uniform;
    }

    public static void deleteUBO()
    {
        GL15.glDeleteBuffers( uniformBuffer );
    }
}
