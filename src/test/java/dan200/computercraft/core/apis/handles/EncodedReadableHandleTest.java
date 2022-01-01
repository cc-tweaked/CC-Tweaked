/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.handles;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ObjectWrapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncodedReadableHandleTest
{
    @Test
    public void testReadChar() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 5 );
        assertEquals( "A", wrapper.callOf( "read" ) );
    }

    @Test
    public void testReadShortComplete() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 10 );
        assertEquals( "AAAAA", wrapper.callOf( "read", 5 ) );
    }

    @Test
    public void testReadShortPartial() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 5 );
        assertEquals( "AAAAA", wrapper.callOf( "read", 10 ) );
    }


    @Test
    public void testReadLongComplete() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 10000 );
        assertEquals( 9000, wrapper.<String>callOf( "read", 9000 ).length() );
    }

    @Test
    public void testReadLongPartial() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 10000 );
        assertEquals( 10000, wrapper.<String>callOf( "read", 11000 ).length() );
    }

    @Test
    public void testReadLongPartialSmaller() throws LuaException
    {
        ObjectWrapper wrapper = fromLength( 1000 );
        assertEquals( 1000, wrapper.<String>callOf( "read", 11000 ).length() );
    }

    private static ObjectWrapper fromLength( int length )
    {
        char[] input = new char[length];
        Arrays.fill( input, 'A' );
        return new ObjectWrapper( new EncodedReadableHandle( new BufferedReader( new CharArrayReader( input ) ) ) );
    }
}
