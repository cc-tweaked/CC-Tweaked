/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.test.core;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.nio.ByteBuffer;

public final class ByteBufferMatcher extends TypeSafeMatcher<ByteBuffer>
{
    private final ByteBuffer expected;

    private ByteBufferMatcher( ByteBuffer expected )
    {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely( ByteBuffer actual )
    {
        return expected.equals( actual );
    }

    @Override
    public void describeTo( Description description )
    {
        description.appendValue( expected );
    }

    @Override
    protected void describeMismatchSafely( ByteBuffer actual, Description mismatchDescription )
    {
        if( expected.remaining() != actual.remaining() )
        {
            mismatchDescription
                .appendValue( actual ).appendText( " has " ).appendValue( actual.remaining() ).appendText( " bytes remaining" );
            return;
        }

        int remaining = expected.remaining();
        int expectedPos = expected.position();
        int actualPos = actual.position();
        for( int i = 0; i < remaining; i++ )
        {
            if( expected.get( expectedPos + i ) == actual.get( actualPos + i ) ) continue;

            int offset = Math.max( i - 5, 0 );
            int length = Math.min( i + 5, remaining - 1 ) - offset + 1;

            byte[] expectedBytes = new byte[length];
            expected.duplicate().position( expectedPos + offset );
            expected.get( expectedBytes );

            byte[] actualBytes = new byte[length];
            actual.duplicate().position( actualPos + offset );
            actual.get( actualBytes );

            mismatchDescription
                .appendText( "failed at " ).appendValue( i ).appendText( System.lineSeparator() )
                .appendText( "expected " ).appendValue( expectedBytes ).appendText( System.lineSeparator() )
                .appendText( "was " ).appendValue( actual );
            return;
        }
    }

    public static Matcher<ByteBuffer> bufferEqual( ByteBuffer buffer )
    {
        return new ByteBufferMatcher( buffer );
    }
}
