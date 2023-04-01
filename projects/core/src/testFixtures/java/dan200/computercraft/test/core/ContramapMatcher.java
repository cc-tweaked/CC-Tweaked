// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.util.function.Function;

/**
 * Given some function from {@code T} to {@code U}, converts a {@code Matcher<U>} to {@code Matcher<T>}. This is useful
 * when you want to match on a particular field (or some other projection) as part of a larger matcher.
 *
 * @param <T> The type of the object to be matched.
 * @param <U> The type of the projection/field to be matched.
 */
public final class ContramapMatcher<T, U> extends FeatureMatcher<T, U> {
    private final Function<T, U> convert;

    public ContramapMatcher(String desc, Function<T, U> convert, Matcher<U> matcher) {
        super(matcher, desc, desc);
        this.convert = convert;
    }

    @Override
    protected U featureValueOf(T actual) {
        return convert.apply(actual);
    }

    public static <T, U> Matcher<T> contramap(Matcher<U> matcher, String desc, Function<T, U> convert) {
        return new ContramapMatcher<>(desc, convert, matcher);
    }

    public static <T, U> Matcher<T> contramap(Matcher<U> matcher, Function<T, U> convert) {
        return new ContramapMatcher<>("-f(_)->", convert, matcher);
    }
}
