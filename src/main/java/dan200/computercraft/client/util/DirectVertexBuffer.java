/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;

/**
 * A version of {@link VertexBuffer} which allows uploading {@link ByteBuffer}s directly.
 */
public class DirectVertexBuffer implements AutoCloseable
{
    private int vertextBufferId;
    private int indexCount;
    private VertexFormat format;

    public DirectVertexBuffer()
    {
        vertextBufferId = DirectBuffers.createBuffer();
    }

    public void upload( int vertexCount, VertexFormat format, ByteBuffer buffer )
    {
        RenderSystem.assertThread( RenderSystem::isOnGameThread );

        DirectBuffers.setBufferData( GL15.GL_ARRAY_BUFFER, vertextBufferId, buffer, GL15.GL_STATIC_DRAW );

        this.format = format;
        indexCount = vertexCount;
    }

    public void draw( Matrix4f matrix, int indexCount )
    {
        bind();
        format.setupBufferState( 0 );

        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.multMatrix( matrix );
        RenderSystem.drawArrays( GL11.GL_QUADS, 0, indexCount );
        RenderSystem.popMatrix();

        unbind();
    }

    public int getIndexCount()
    {
        return indexCount;
    }

    @Override
    public void close()
    {
        if( vertextBufferId >= 0 )
        {
            RenderSystem.glDeleteBuffers( vertextBufferId );
            vertextBufferId = -1;
        }
    }

    private void bind()
    {
        RenderSystem.glBindBuffer( GL15.GL_ARRAY_BUFFER, () -> vertextBufferId );
    }

    private static void unbind()
    {
        RenderSystem.glBindBuffer( GL15.GL_ARRAY_BUFFER, () -> 0 );
    }
}
