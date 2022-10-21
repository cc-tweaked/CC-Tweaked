/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.support;

import org.hamcrest.Matcher;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;

public class CustomMatchers
{
    /**
     * Assert two lists are equal according to some matcher.
     * <p>
     * This method is simple, but helps avoid some issues with generics we'd see otherwise.
     *
     * @param items   The items the matched list should be equal to.
     * @param matcher Generate a matcher for a single item in the list.
     * @param <T>     The type to compare against.
     * @return A matcher which compares against a list of items.
     */
    public static <T> Matcher<Iterable<? extends T>> containsWith( List<T> items, Function<T, Matcher<? super T>> matcher )
    {
        return contains( items.stream().map( matcher ).collect( Collectors.toList() ) );
    }
}
