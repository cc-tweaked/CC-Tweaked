/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.computer;

import com.google.common.io.CharStreams;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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
                ComputerBootstrap.run( "print('Hello') while true do end", ComputerBootstrap.MAX_TIME );
            }
            catch( AssertionError e )
            {
                if( e.getMessage().equals( "test.lua:1: Too long without yielding" ) ) return;
                throw e;
            }

            Assertions.fail( "Expected computer to timeout" );
        } );
    }

    public static void main( String[] args ) throws Exception
    {
        InputStream stream = ComputerTest.class.getClassLoader().getResourceAsStream( "benchmark.lua" );
        try( InputStreamReader reader = new InputStreamReader( Objects.requireNonNull( stream ), StandardCharsets.UTF_8 ) )
        {
            String contents = CharStreams.toString( reader );
            ComputerBootstrap.run( contents, 1000 );
        }
    }
}
