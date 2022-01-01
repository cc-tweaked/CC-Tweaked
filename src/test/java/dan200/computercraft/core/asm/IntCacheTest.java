/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntCacheTest
{
    @Test
    public void testCache()
    {
        IntCache<Object> c = new IntCache<>( i -> new Object() );
        assertEquals( c.get( 0 ), c.get( 0 ) );
    }

    @Test
    public void testMassive()
    {
        assertEquals( 40, new IntCache<>( i -> i ).get( 40 ) );
    }
}
