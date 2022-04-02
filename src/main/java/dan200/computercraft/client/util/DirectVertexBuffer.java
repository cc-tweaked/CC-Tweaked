/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;

/**
 * A version of {@link VertexBuffer} which allows uploading {@link ByteBuffer}s directly.
 *
 * This should probably be its own class (rather than subclassing), but I need access to {@link VertexBuffer#drawWithShader}.
 */
public class DirectVertexBuffer extends VertexBuffer
{
    private static final boolean HAS_DSA;

    static
    {
        var capabilities = GL.getCapabilities();
        HAS_DSA = capabilities.OpenGL45 || capabilities.GL_ARB_direct_state_access;
    }

    public DirectVertexBuffer()
    {
        if( HAS_DSA )
        {
            RenderSystem.glDeleteBuffers( vertextBufferId );
            vertextBufferId = GL45C.glCreateBuffers();
        }
    }

    public void upload( int vertexCount, VertexFormat.Mode mode, VertexFormat format, ByteBuffer buffer )
    {
        RenderSystem.assertOnRenderThread();

        if( HAS_DSA )
        {
            GL45C.glNamedBufferData( vertextBufferId, buffer, GL15.GL_STATIC_DRAW );
        }
        else
        {
            BufferUploader.reset();
            bind();
            RenderSystem.glBufferData( GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW );
            unbind();
        }

        this.format = format;
        this.mode = mode;
        indexCount = mode.indexCount( vertexCount );
        indexType = VertexFormat.IndexType.SHORT;
        sequentialIndices = true;
    }
}
