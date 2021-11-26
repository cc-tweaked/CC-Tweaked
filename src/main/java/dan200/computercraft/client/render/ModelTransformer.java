/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import java.util.List;

/**
 * Transforms vertices of a model, remaining aware of winding order, and rearranging vertices if needed.
 */
@Environment( EnvType.CLIENT )
public final class ModelTransformer
{
    private static final Matrix4f identity;

    static
    {
        identity = new Matrix4f();
        identity.setIdentity();
    }

    private ModelTransformer()
    {
    }

    public static void transformQuadsTo( List<BakedQuad> output, List<BakedQuad> input, Matrix4f transform )
    {
        transformQuadsTo( DefaultVertexFormat.BLOCK, output, input, transform );
    }

    public static void transformQuadsTo( VertexFormat format, List<BakedQuad> output, List<BakedQuad> input, Matrix4f transform )
    {
        if( transform == null || transform.equals( identity ) )
        {
            output.addAll( input );
        }
        else
        {
            for( BakedQuad quad : input )
            {
                output.add( doTransformQuad( format, quad, transform ) );
            }
        }
    }

    private static BakedQuad doTransformQuad( VertexFormat format, BakedQuad quad, Matrix4f transform )
    {
        int[] vertexData = quad.getVertices().clone();
        BakedQuad copy = new BakedQuad( vertexData, -1, quad.getDirection(), quad.getSprite(), true );

        int offsetBytes = 0;
        for( int v = 0; v < 4; ++v )
        {
            for( VertexFormatElement element : format.getElements() ) // For each vertex element
            {
                int start = offsetBytes / Integer.BYTES;
                if( element.getUsage() == VertexFormatElement.Usage.POSITION && element.getType() == VertexFormatElement.Type.FLOAT ) // When we find a position element
                {
                    Vector4f pos = new Vector4f( Float.intBitsToFloat( vertexData[start] ),
                        Float.intBitsToFloat( vertexData[start + 1] ),
                        Float.intBitsToFloat( vertexData[start + 2] ),
                        1 );

                    // Transform the position
                    pos.transform( transform );

                    // Insert the position
                    vertexData[start] = Float.floatToRawIntBits( pos.x() );
                    vertexData[start + 1] = Float.floatToRawIntBits( pos.y() );
                    vertexData[start + 2] = Float.floatToRawIntBits( pos.z() );
                }
                offsetBytes += element.getByteSize();
            }
        }
        return copy;
    }

    public static BakedQuad transformQuad( VertexFormat format, BakedQuad input, Matrix4f transform )
    {
        if( transform == null || transform.equals( identity ) )
        {
            return input;
        }
        return doTransformQuad( format, input, transform );
    }
}
