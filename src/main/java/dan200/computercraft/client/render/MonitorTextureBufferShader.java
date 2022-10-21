/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.google.common.base.Strings;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.getColour;

class MonitorTextureBufferShader
{
    public static final int UNIFORM_SIZE = 4 * 4 * 16 + 4 + 4 + 2 * 4 + 4;

    static final int TEXTURE_INDEX = GL13.GL_TEXTURE3;

    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer( 16 );

    private static int uniformMv;

    private static int uniformFont;
    private static int uniformTbo;
    private static int uniformMonitor;
    private static int uniformCursorBlink;

    private static boolean initialised;
    private static boolean ok;
    private static int program;

    static void setupUniform( Matrix4f transform, int tboUniform )
    {
        MATRIX_BUFFER.rewind();
        transform.store( MATRIX_BUFFER );
        MATRIX_BUFFER.rewind();
        RenderSystem.glUniformMatrix4( uniformMv, false, MATRIX_BUFFER );

        GL31.glBindBufferBase( GL31.GL_UNIFORM_BUFFER, uniformMonitor, tboUniform );

        int cursorAlpha = FrameInfo.getGlobalCursorBlink() ? 1 : 0;
        RenderSystem.glUniform1i( uniformCursorBlink, cursorAlpha );
    }

    static boolean use()
    {
        if( initialised )
        {
            if( ok ) GlStateManager._glUseProgram( program );
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

            program = GlStateManager.glCreateProgram();
            GlStateManager.glAttachShader( program, vertexShader );
            GlStateManager.glAttachShader( program, fragmentShader );
            GL20.glBindAttribLocation( program, 0, "v_pos" );

            GlStateManager.glLinkProgram( program );
            boolean ok = GlStateManager.glGetProgrami( program, GL20.GL_LINK_STATUS ) != 0;
            String log = GlStateManager.glGetProgramInfoLog( program, Short.MAX_VALUE ).trim();
            if( !Strings.isNullOrEmpty( log ) )
            {
                ComputerCraft.log.warn( "Problems when linking monitor shader: {}", log );
            }

            GL20.glDetachShader( program, vertexShader );
            GL20.glDetachShader( program, fragmentShader );
            GlStateManager.glDeleteShader( vertexShader );
            GlStateManager.glDeleteShader( fragmentShader );

            if( !ok ) return false;

            uniformMv = getUniformLocation( program, "u_mv" );
            uniformFont = getUniformLocation( program, "u_font" );
            uniformTbo = getUniformLocation( program, "u_tbo" );
            uniformMonitor = GL31.glGetUniformBlockIndex( program, "u_monitor" );
            if( uniformMonitor == -1 ) throw new IllegalStateException( "Could not find uniformMonitor uniform." );
            uniformCursorBlink = getUniformLocation( program, "u_cursorBlink" );

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

        int shader = GlStateManager.glCreateShader( kind );

        GlStateManager.glShaderSource( shader, contents );
        GlStateManager.glCompileShader( shader );

        boolean ok = GlStateManager.glGetShaderi( shader, GL20.GL_COMPILE_STATUS ) != 0;
        String log = GlStateManager.glGetShaderInfoLog( shader, Short.MAX_VALUE ).trim();
        if( !Strings.isNullOrEmpty( log ) )
        {
            ComputerCraft.log.warn( "Problems when loading monitor shader {}: {}", path, log );
        }

        if( !ok ) throw new IllegalStateException( "Cannot compile shader " + path );
        return shader;
    }

    private static int getUniformLocation( int program, String name )
    {
        int uniform = GlStateManager._glGetUniformLocation( program, name );
        if( uniform == -1 ) throw new IllegalStateException( "Cannot find uniform " + name );
        return uniform;
    }

    public static void setTerminalData( ByteBuffer buffer, Terminal terminal )
    {
        int width = terminal.getWidth(), height = terminal.getHeight();

        int pos = 0;
        for( int y = 0; y < height; y++ )
        {
            TextBuffer text = terminal.getLine( y ), textColour = terminal.getTextColourLine( y ), background = terminal.getBackgroundColourLine( y );
            for( int x = 0; x < width; x++ )
            {
                buffer.put( pos, (byte) (text.charAt( x ) & 0xFF) );
                buffer.put( pos + 1, (byte) getColour( textColour.charAt( x ), Colour.WHITE ) );
                buffer.put( pos + 2, (byte) getColour( background.charAt( x ), Colour.BLACK ) );
                pos += 3;
            }
        }

        buffer.limit( pos );
    }

    public static void setUniformData( ByteBuffer buffer, Terminal terminal )
    {
        int pos = 0;
        Palette palette = terminal.getPalette();
        for( int i = 0; i < 16; i++ )
        {
            double[] colour = palette.getColour( i );
            if( !terminal.isColour() )
            {
                float f = FixedWidthFontRenderer.toGreyscale( colour );
                buffer.putFloat( pos, f ).putFloat( pos + 4, f ).putFloat( pos + 8, f );
            }
            else
            {
                buffer.putFloat( pos, (float) colour[0] ).putFloat( pos + 4, (float) colour[1] ).putFloat( pos + 8, (float) colour[2] );
            }

            pos += 4 * 4; // std140 requires these are 4-wide
        }

        boolean showCursor = FixedWidthFontRenderer.isCursorVisible( terminal );
        buffer
            .putInt( pos, terminal.getWidth() ).putInt( pos + 4, terminal.getHeight() )
            .putInt( pos + 8, showCursor ? terminal.getCursorX() : -2 )
            .putInt( pos + 12, showCursor ? terminal.getCursorY() : -2 )
            .putInt( pos + 16, 15 - terminal.getTextColour() );

        buffer.limit( UNIFORM_SIZE );
    }
}
