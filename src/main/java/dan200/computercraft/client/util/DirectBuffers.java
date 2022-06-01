/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.util;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Provides utilities to interact with OpenGL's buffer objects, either using direct state access or binding/unbinding
 * it.
 */
public class DirectBuffers
{
    public static final boolean HAS_DSA;

    static
    {
        GLCapabilities capabilities = GL.getCapabilities();
        HAS_DSA = capabilities.OpenGL45 || capabilities.GL_ARB_direct_state_access;
    }

    public static int createBuffer()
    {
        return HAS_DSA ? GL45C.glCreateBuffers() : GL15C.glGenBuffers();
    }

    public static void setBufferData( int type, int id, ByteBuffer buffer, int flags )
    {
        if( HAS_DSA )
        {
            GL45C.glNamedBufferData( id, buffer, flags );
        }
        else
        {
            GlStateManager._glBindBuffer( type, id );
            GlStateManager._glBufferData( type, buffer, GL15C.GL_STATIC_DRAW );
            GlStateManager._glBindBuffer( type, 0 );
        }
    }

    public static void setEmptyBufferData( int type, int id, int flags )
    {
        if( HAS_DSA )
        {
            GL45C.glNamedBufferData( id, 0, flags );
        }
        else
        {
            GlStateManager._glBindBuffer( type, id );
            GL45C.glBufferData( type, 0, GL15C.GL_STATIC_DRAW );
            GlStateManager._glBindBuffer( type, 0 );
        }
    }

    private static final MemoryUtil.MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator( false );

    public static ByteBuffer createByteBuffer( int size )
    {
        long i = ALLOCATOR.malloc( size );
        if( i == 0L ) throw new OutOfMemoryError( "Failed to allocate " + size + " bytes" );
        return MemoryUtil.memByteBuffer( i, size );
    }

    public static ByteBuffer resizeByteBuffer( ByteBuffer buffer, int size )
    {
        long i = ALLOCATOR.realloc( MemoryUtil.memAddress0( buffer ), size );
        if( i == 0L )
        {
            throw new OutOfMemoryError( "Failed to resize buffer from " + buffer.capacity() + " bytes to " + size + " bytes" );
        }

        return MemoryUtil.memByteBuffer( i, size );
    }
}
