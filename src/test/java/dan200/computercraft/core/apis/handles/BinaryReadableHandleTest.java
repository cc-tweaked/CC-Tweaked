/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ObjectWrapper;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

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
        assertEquals( 5, wrapper.<byte[]>callOf( "read", 5 ).length );
    }

    @Test
    public void testReadShortPartial() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 5 );
        assertEquals( 5, wrapper.<byte[]>callOf( "read", 10 ).length );
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
        assertEquals( 1000, wrapper.<byte[]>callOf( "read", 11000 ).length );
    }

    private static ObjectWrapper fromLength( int length )
    {
        byte[] input = new byte[length];
        Arrays.fill( input, (byte) 'A' );
        return new ObjectWrapper( new BinaryReadableHandle( new ArrayByteChannel( input ) ) );
    }
}
