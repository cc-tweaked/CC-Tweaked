/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ObjectWrapper;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryReadableHandleTest
{
    @Test
    public void testReadChar() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 5 );
        assertEquals( 'A', (int) wrapper.callOf( Integer.class, "read" ) );
    }

    @Test
    public void testReadShortComplete() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 10 );
        assertEquals( 5, wrapper.<ByteBuffer>callOf( "read", 5 ).remaining() );
    }

    @Test
    public void testReadShortPartial() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 5 );
        assertEquals( 5, wrapper.<ByteBuffer>callOf( "read", 10 ).remaining() );
    }

    @Test
    public void testReadLongComplete() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 10000 );
        assertEquals( 9000, wrapper.<byte[]>callOf( "read", 9000 ).length );
    }

    @Test
    public void testReadLongPartial() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 10000 );
        assertEquals( 10000, wrapper.<byte[]>callOf( "read", 11000 ).length );
    }

    @Test
    public void testReadLongPartialSmaller() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 1000 );
        assertEquals( 1000, wrapper.<ByteBuffer>callOf( "read", 11000 ).remaining() );
    }

    @Test
    public void testReadLine() throws LuaException
    {
        ObjectWrapper wrapper = new ObjectWrapper( BinaryReadableHandle.of( new ArrayByteChannel( "hello\r\nworld\r!".getBytes( StandardCharsets.UTF_8 ) ) ) );
        assertArrayEquals( "hello".getBytes( StandardCharsets.UTF_8 ), wrapper.callOf( "readLine" ) );
        assertArrayEquals( "world\r!".getBytes( StandardCharsets.UTF_8 ), wrapper.callOf( "readLine" ) );
        assertNull( wrapper.call( "readLine" ) );
    }

    @Test
    public void testReadLineTrailing() throws LuaException
    {
        ObjectWrapper wrapper = new ObjectWrapper( BinaryReadableHandle.of( new ArrayByteChannel( "hello\r\nworld\r!".getBytes( StandardCharsets.UTF_8 ) ) ) );
        assertArrayEquals( "hello\r\n".getBytes( StandardCharsets.UTF_8 ), wrapper.callOf( "readLine", true ) );
        assertArrayEquals( "world\r!".getBytes( StandardCharsets.UTF_8 ), wrapper.callOf( "readLine", true ) );
        assertNull( wrapper.call( "readLine", true ) );
    }

    private static ObjectWrapper fromLength( int length )
    {
        byte[] input = new byte[length];
        Arrays.fill( input, (byte) 'A' );
        return new ObjectWrapper( BinaryReadableHandle.of( new ArrayByteChannel( input ) ) );
    }
}
