package dan200.computercraft.client.render;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.client.model.pipeline.VertexTransformer;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.List;

/**
 * Transforms vertices of a model, remaining aware of winding order, and rearranging
 * vertices if needed.
 */
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
        if( transform == null || transform.equals( identity ) )
        {
            output.addAll( input );
        }
        else
        {
            Matrix4f normalMatrix = new Matrix4f( transform );
            normalMatrix.invert();
            normalMatrix.transpose();

            for( BakedQuad quad : input ) output.add( doTransformQuad( quad, transform, normalMatrix ) );
        }
    }

    public static BakedQuad transformQuad( BakedQuad input, Matrix4f transform )
    {
        if( transform == null || transform.equals( identity ) ) return input;

        Matrix4f normalMatrix = new Matrix4f( transform );
        normalMatrix.invert();
        normalMatrix.transpose();
        return doTransformQuad( input, transform, normalMatrix );
    }

    private static BakedQuad doTransformQuad( BakedQuad input, Matrix4f positionMatrix, Matrix4f normalMatrix )
    {

        BakedQuadBuilder builder = new BakedQuadBuilder( input.getFormat() );
        NormalAwareTransformer transformer = new NormalAwareTransformer( builder, positionMatrix, normalMatrix );
        input.pipe( transformer );

        if( transformer.areNormalsInverted() )
        {
            builder.swap( 1, 3 );
            transformer.areNormalsInverted();
        }

        return builder.build();
    }

    /**
     * A vertex transformer that tracks whether the normals have been inverted and so the vertices
     * should be reordered so backface culling works as expected.
     */
    private static class NormalAwareTransformer extends VertexTransformer
    {
        private final Matrix4f positionMatrix;
        private final Matrix4f normalMatrix;

        private int vertexIndex = 0, elementIndex = 0;
        private final Point3f[] before = new Point3f[ 4 ];
        private final Point3f[] after = new Point3f[ 4 ];

        public NormalAwareTransformer( IVertexConsumer parent, Matrix4f positionMatrix, Matrix4f normalMatrix )
        {
            super( parent );
            this.positionMatrix = positionMatrix;
            this.normalMatrix = normalMatrix;
        }

        @Override
        public void setQuadOrientation( EnumFacing orientation )
        {
            super.setQuadOrientation( orientation == null ? orientation : TRSRTransformation.rotate( positionMatrix, orientation ) );
        }

        @Override
        public void put( int element, @Nonnull float... data )
        {
            switch( getVertexFormat().getElement( element ).getUsage() )
            {
                case POSITION:
                {
                    Point3f vec = new Point3f( data );
                    Point3f newVec = new Point3f();
                    positionMatrix.transform( vec, newVec );

                    float[] newData = new float[ 4 ];
                    newVec.get( newData );
                    super.put( element, newData );


                    before[ vertexIndex ] = vec;
                    after[ vertexIndex ] = newVec;
                    break;
                }
                case NORMAL:
                {
                    Vector3f vec = new Vector3f( data );
                    normalMatrix.transform( vec );

                    float[] newData = new float[ 4 ];
                    vec.get( newData );
                    super.put( element, newData );
                    break;
                }
                default:
                    super.put( element, data );
                    break;
            }

            elementIndex++;
            if( elementIndex == getVertexFormat().getElementCount() )
            {
                vertexIndex++;
                elementIndex = 0;
            }
        }

        public boolean areNormalsInverted()
        {
            Vector3f temp1 = new Vector3f(), temp2 = new Vector3f();
            Vector3f crossBefore = new Vector3f(), crossAfter = new Vector3f();

            // Determine what cross product we expect to have
            temp1.sub( before[ 1 ], before[ 0 ] );
            temp2.sub( before[ 1 ], before[ 2 ] );
            crossBefore.cross( temp1, temp2 );
            normalMatrix.transform( crossBefore );

            // And determine what cross product we actually have
            temp1.sub( after[ 1 ], after[ 0 ] );
            temp2.sub( after[ 1 ], after[ 2 ] );
            crossAfter.cross( temp1, temp2 );

            // If the angle between expected and actual cross product is greater than 
            // pi/2 radians then we will need to reorder our quads.
            return Math.abs( crossBefore.angle( crossAfter ) ) >= Math.PI / 2;
        }
    }

    /**
     * A vertex consumer which is capable of building {@link BakedQuad}s.
     *
     * Equivalent to {@link net.minecraftforge.client.model.pipeline.UnpackedBakedQuad.Builder} but more memory
     * efficient.
     *
     * This also provides the ability to swap vertices through {@link #swap(int, int)} to allow reordering.
     */
    private static class BakedQuadBuilder implements IVertexConsumer
    {
        private final VertexFormat format;

        private final int[] vertexData;
        private int vertexIndex = 0, elementIndex = 0;

        private EnumFacing orientation;
        private int quadTint;
        private boolean diffuse;
        private TextureAtlasSprite texture;

        private BakedQuadBuilder( VertexFormat format )
        {
            this.format = format;
            this.vertexData = new int[ format.getSize() ];
        }

        @Nonnull
        @Override
        public VertexFormat getVertexFormat()
        {
            return format;
        }

        @Override
        public void setQuadTint( int tint )
        {
            this.quadTint = tint;
        }

        @Override
        public void setQuadOrientation( @Nonnull EnumFacing orientation )
        {
            this.orientation = orientation;
        }

        @Override
        public void setApplyDiffuseLighting( boolean diffuse )
        {
            this.diffuse = diffuse;
        }

        @Override
        public void setTexture( @Nonnull TextureAtlasSprite texture )
        {
            this.texture = texture;
        }

        @Override
        public void put( int element, @Nonnull float... data )
        {
            LightUtil.pack( data, vertexData, format, vertexIndex, element );

            elementIndex++;
            if( elementIndex == getVertexFormat().getElementCount() )
            {
                vertexIndex++;
                elementIndex = 0;
            }
        }

        public void swap( int a, int b )
        {
            int length = vertexData.length / 4;
            for( int i = 0; i < length; i++ )
            {
                int temp = vertexData[ a * length + i ];
                vertexData[ a * length + i ] = vertexData[ b * length + i ];
                vertexData[ b * length + i ] = temp;
            }
        }

        public BakedQuad build()
        {
            if( elementIndex != 0 || vertexIndex != 4 )
            {
                throw new IllegalStateException( "Got an unexpected number of elements/vertices" );
            }
            if( texture == null )
            {
                throw new IllegalStateException( "Texture has not been set" );
            }

            return new BakedQuad( vertexData, quadTint, orientation, texture, diffuse, format );
        }
    }
}
