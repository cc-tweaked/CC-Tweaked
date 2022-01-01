/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.function.Function;

public class ContramapMatcher<T, U> extends TypeSafeDiagnosingMatcher<T>
{
    private final String desc;
    private final Function<T, U> convert;
    private final Matcher<U> matcher;

    public ContramapMatcher( String desc, Function<T, U> convert, Matcher<U> matcher )
    {
        this.desc = desc;
        this.convert = convert;
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely( T item, Description mismatchDescription )
    {
        U converted = convert.apply( item );
        if( matcher.matches( converted ) ) return true;

        mismatchDescription.appendText( desc ).appendText( " " );
        matcher.describeMismatch( converted, mismatchDescription );
        return false;
    }

    @Override
    public void describeTo( Description description )
    {
        description.appendText( desc ).appendText( " " ).appendDescriptionOf( matcher );
    }

    public static <T, U> Matcher<T> contramap( Matcher<U> matcher, String desc, Function<T, U> convert )
    {
        return new ContramapMatcher<>( desc, convert, matcher );
    }

    public static <T, U> Matcher<T> contramap( Matcher<U> matcher, Function<T, U> convert )
    {
        return new ContramapMatcher<>( "-f(_)->", convert, matcher );
    }
}
