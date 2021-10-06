/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL13;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.FloatBuffer;

public class MonitorTextureBufferShader extends Shader
{
    static final int TEXTURE_INDEX = GL13.GL_TEXTURE3;

    private static final Logger LOGGER = LogManager.getLogger();

    private final GlUniform palette;
    private final GlUniform width;
    private final GlUniform height;

    public MonitorTextureBufferShader( ResourceFactory factory, String name, VertexFormat format ) throws IOException
    {
        super( factory, name, format );

        width = getUniformChecked( "Width" );
        height = getUniformChecked( "Height" );
        palette = new GlUniform( "Palette", GlUniform.field_32044 /* UT_FLOAT3 */, 16 * 3, this );
        updateUniformLocation( palette );

        GlUniform tbo = getUniformChecked( "Tbo" );
        if( tbo != null ) tbo.set( TEXTURE_INDEX - GL13.GL_TEXTURE0 );
    }

    void setupUniform( int width, int height, Palette palette, boolean greyscale )
    {
        if( this.width != null ) this.width.set( width );
        if( this.height != null ) this.height.set( height );
        setupPalette( palette, greyscale );
    }

    private void setupPalette( Palette palette, boolean greyscale )
    {
        if( this.palette == null ) return;

        FloatBuffer paletteBuffer = this.palette.getFloatData();
        paletteBuffer.rewind();
        for( int i = 0; i < 16; i++ )
        {
            double[] colour = palette.getColour( i );
            if( greyscale )
            {
                float f = FixedWidthFontRenderer.toGreyscale( colour );
                paletteBuffer.put( f ).put( f ).put( f );
            }
            else
            {
                paletteBuffer.put( (float) colour[0] ).put( (float) colour[1] ).put( (float) colour[2] );
            }
        }
    }

    @Override
    public void bind()
    {
        super.bind();
        palette.upload();
    }

    @Override
    public void close()
    {
        palette.close();
        super.close();
    }

    private void updateUniformLocation( GlUniform uniform )
    {
        int id = GlUniform.getUniformLocation( getProgramRef(), uniform.getName() );
        if( id == -1 )
        {
            LOGGER.warn( "Shader {} could not find uniform named {} in the specified shader program.", getName(), uniform.getName() );
        }
        else
        {
            uniform.setLoc( id );
        }
    }

    @Nullable
    private GlUniform getUniformChecked( String name )
    {
        GlUniform uniform = getUniform( name );
        if( uniform == null )
        {
            LOGGER.warn( "Monitor shader {} should have uniform {}, but it was not present.", getName(), name );
        }

        return uniform;
    }
}
