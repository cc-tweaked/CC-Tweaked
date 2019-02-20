/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import org.junit.Assert;
import org.junit.Test;

public class ComputerTest
{
    @Test( timeout = 20_000 )
    public void testTimeout()
    {
        try
        {
            ComputerBootstrap.run( "print('Hello') while true do end" );
        }
        catch( AssertionError e )
        {
            if( e.getMessage().equals( "test.lua:1: Too long without yielding" ) ) return;
            throw e;
        }

        Assert.fail( "Expected computer to timeout" );
    }
}
