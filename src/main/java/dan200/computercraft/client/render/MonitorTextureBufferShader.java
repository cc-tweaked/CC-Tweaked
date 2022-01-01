/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.shared.util.Palette;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL13;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.FloatBuffer;

public class MonitorTextureBufferShader extends ShaderInstance
{
    static final int TEXTURE_INDEX = GL13.GL_TEXTURE3;

    private static final Logger LOGGER = LogManager.getLogger();

    private final Uniform palette;
    private final Uniform width;
    private final Uniform height;

    public MonitorTextureBufferShader( ResourceProvider provider, ResourceLocation location, VertexFormat format ) throws IOException
    {
        super( provider, location, format );

        width = getUniformChecked( "Width" );
        height = getUniformChecked( "Height" );
        palette = new Uniform( "Palette", Uniform.UT_FLOAT3, 16 * 3, this );
        updateUniformLocation( palette );

        Uniform tbo = getUniformChecked( "Tbo" );
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

        FloatBuffer paletteBuffer = this.palette.getFloatBuffer();
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
    public void apply()
    {
        super.apply();
        palette.upload();
    }

    @Override
    public void close()
    {
        palette.close();
        super.close();
    }

    private void updateUniformLocation( Uniform uniform )
    {
        int id = Uniform.glGetUniformLocation( getId(), uniform.getName() );
        if( id == -1 )
        {
            LOGGER.warn( "Shader {} could not find uniform named {} in the specified shader program.", getName(), uniform.getName() );
        }
        else
        {
            uniform.setLocation( id );
        }
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
}
