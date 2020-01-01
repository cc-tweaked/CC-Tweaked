/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class ComputerTest
{
    @Test
    public void testTimeout()
    {
        assertTimeoutPreemptively( ofSeconds( 20 ), () -> {
            try
            {
                ComputerBootstrap.run( "print('Hello') while true do end" );
            }
            catch( AssertionError e )
            {
                if( e.getMessage().equals( "test.lua:1: Too long without yielding" ) ) return;
                throw e;
            }

            Assertions.fail( "Expected computer to timeout" );
        } );
    }
}
