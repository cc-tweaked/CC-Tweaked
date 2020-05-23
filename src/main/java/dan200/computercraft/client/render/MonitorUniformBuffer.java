package dan200.computercraft.client.render;

import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.shared.util.Palette;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import java.io.Closeable;
import java.nio.ByteBuffer;

class MonitorUniformBuffer implements Closeable
{
    private final int uniformBuffer;

    private int width, height;
    private Palette palette;
    private boolean greyscale;

    private static final int UBO_SIZE =
        2 * 4 + // width, height (int, int)
        3 * 4 * 16; // palette (vec3[16])

    private static final ByteBuffer UBO_BUFFER = BufferUtils.createByteBuffer( UBO_SIZE );

    MonitorUniformBuffer()
    {
        uniformBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer( GL31.GL_UNIFORM_BUFFER, uniformBuffer );
        GL15.glBufferData( GL31.GL_UNIFORM_BUFFER, UBO_SIZE, GL15.GL_DYNAMIC_DRAW );
    }

    int getHandle()
    {
        return uniformBuffer;
    }

    void set( int width, int height, Palette palette, boolean greyscale )
    {
        if ( width != this.width || height != this.height || !palette.equals( this.palette ) || greyscale != this.greyscale )
        {
            this.width = width;
            this.height = height;
            this.palette = palette;
            this.greyscale = greyscale;

            GL15.glBindBuffer( GL31.GL_UNIFORM_BUFFER, uniformBuffer );
            GL15.glMapBuffer( GL31.GL_UNIFORM_BUFFER, GL15.GL_WRITE_ONLY, UBO_BUFFER );
            UBO_BUFFER.rewind();

            UBO_BUFFER.putInt( width ).putInt( height );

            for( int i = 0; i < 16; i++ )
            {
                double[] colour = palette.getColour( i );
                if( greyscale )
                {
                    float f = FixedWidthFontRenderer.toGreyscale( colour );
                    UBO_BUFFER.putFloat( f ).putFloat( f ).putFloat( f );
                } else
                {
                    UBO_BUFFER.putFloat( (float) colour[0] ).putFloat( (float) colour[1] ).putFloat( (float) colour[2] );
                }
            }

            UBO_BUFFER.flip();
            GL15.glUnmapBuffer( uniformBuffer );
        }
    }

    @Override
    public void close()
    {
        GL15.glDeleteBuffers( uniformBuffer );
    }
}
