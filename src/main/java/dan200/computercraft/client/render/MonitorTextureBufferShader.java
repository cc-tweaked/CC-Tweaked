/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.client.FrameInfo;
import dan200.computercraft.client.render.text.FixedWidthFontRenderer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.core.terminal.TextBuffer;
import dan200.computercraft.shared.util.Colour;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL31;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;

import static dan200.computercraft.client.render.text.FixedWidthFontRenderer.getColour;

public class MonitorTextureBufferShader extends ShaderInstance
{
    public static final int UNIFORM_SIZE = 4 * 4 * 16 + 4 + 4 + 2 * 4 + 4;

    static final int TEXTURE_INDEX = GL13.GL_TEXTURE3;

    private static final Logger LOGGER = LogManager.getLogger();

    private final int monitorData;
    private int uniformBuffer = 0;

    private final Uniform cursorBlink;

    public MonitorTextureBufferShader( ResourceProvider provider, ResourceLocation location, VertexFormat format ) throws IOException
    {
        super( provider, location, format );
        monitorData = GL31.glGetUniformBlockIndex( getId(), "MonitorData" );
        if( monitorData == -1 ) throw new IllegalStateException( "Could not find MonitorData uniform." );

        cursorBlink = getUniformChecked( "CursorBlink" );

        Uniform tbo = getUniformChecked( "Tbo" );
        if( tbo != null ) tbo.set( TEXTURE_INDEX - GL13.GL_TEXTURE0 );
    }

    public void setupUniform( int buffer )
    {
        uniformBuffer = buffer;

        int cursorAlpha = FrameInfo.getGlobalCursorBlink() ? 1 : 0;
        if( cursorBlink != null && cursorBlink.getIntBuffer().get( 0 ) != cursorAlpha ) cursorBlink.set( cursorAlpha );
    }

    @Override
    public void apply()
    {
        super.apply();
        GL31.glBindBufferBase( GL31.GL_UNIFORM_BUFFER, monitorData, uniformBuffer );
    }

    @Nullable
    private Uniform getUniformChecked( String name )
    {
        Uniform uniform = getUniform( name );
        if( uniform == null )
        {
            LOGGER.warn( "Monitor shader {} should have uniform {}, but it was not present.", getName(), name );
        }

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

    public static void setUniformData( ByteBuffer buffer, Terminal terminal, boolean greyscale )
    {
        int pos = 0;
        var palette = terminal.getPalette();
        for( int i = 0; i < 16; i++ )
        {
            {
                double[] colour = palette.getColour( i );
                if( greyscale )
                {
                    float f = FixedWidthFontRenderer.toGreyscale( colour );
                    buffer.putFloat( pos, f ).putFloat( pos + 4, f ).putFloat( pos + 8, f );
                }
                else
                {
                    buffer.putFloat( pos, (float) colour[0] ).putFloat( pos + 4, (float) colour[1] ).putFloat( pos + 8, (float) colour[2] );
                }
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
