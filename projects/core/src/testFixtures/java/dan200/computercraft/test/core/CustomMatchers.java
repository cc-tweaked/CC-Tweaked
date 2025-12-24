// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

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

    /**
     * An alternative to {@link org.hamcrest.Matchers#hasEntry(Object, Object)}, that acts as a projection, rather than
     * searching the map.
     *
     * @param key   The key to extract.
     * @param value The expected value.
     * @param <K>   The type of keys in the map.
     * @param <V>   The type of values in the map.
     * @return A matcher that projects out of a map.
     */
    public static <K, V> Matcher<Map<? extends K, ? extends V>> containsEntry(K key, V value) {
        return containsEntryWith(key, is(value));
    }

    /**
     * An alternative to {@link org.hamcrest.Matchers#hasEntry(Matcher, Matcher)}, that acts as a projection, rather
     * than searching the map.
     *
     * @param key   The key to extract.
     * @param value The expected value.
     * @param <K>   The type of keys in the map.
     * @param <V>   The type of values in the map.
     * @return A matcher that projects out of a map.
     */
    public static <K, V> Matcher<Map<? extends K, ? extends V>> containsEntryWith(K key, Matcher<? super V> value) {
        return ContramapMatcher.contramap(value, new StringDescription().appendValue(key).toString(), x -> x.get(key));
    }
}
