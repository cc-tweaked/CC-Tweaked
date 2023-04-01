// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import org.hamcrest.Matcher;

import java.util.List;
import java.util.function.Function;

import static org.hamcrest.Matchers.contains;

public class CustomMatchers {
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
    public static <T> Matcher<Iterable<? extends T>> containsWith(List<T> items, Function<T, Matcher<? super T>> matcher) {
        return contains(items.stream().map(matcher).toList());
    }
}
