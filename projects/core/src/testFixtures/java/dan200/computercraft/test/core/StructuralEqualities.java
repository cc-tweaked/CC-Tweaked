// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Concrete implementations for {@link StructuralEquality}.
 */
final class StructuralEqualities {
    static final DefaultEquality DEFAULT = new DefaultEquality();

    private StructuralEqualities() {
    }

    static <T> void describeNullable(Description description, StructuralEquality<T> equality, @Nullable T value) {
        if (value == null) {
            description.appendText("null");
        } else {
            equality.describe(description, value);
        }
    }

    static final class DefaultEquality implements StructuralEquality<Object> {
        private DefaultEquality() {
        }

        @Override
        public boolean equals(Object left, Object right) {
            return Objects.equals(left, right);
        }

        @Override
        public void describe(Description description, Object object) {
            description.appendValue(object);
        }
    }

    static final class AllEquality<T> implements StructuralEquality<T> {
        private final List<StructuralEquality<T>> equalities;

        AllEquality(List<StructuralEquality<T>> equalities) {
            this.equalities = equalities;
        }

        @Override
        public boolean equals(T left, T right) {
            return equalities.stream().allMatch(x -> x.equals(left, right));
        }

        @Override
        public void describe(Description description, T object) {
            description.appendText("{");
            var separator = false;
            for (var equality : equalities) {
                if (separator) description.appendText(", ");
                separator = true;
                equality.describe(description, object);
            }
            description.appendText("}");
        }
    }

    static final class FeatureEquality<T, U> implements StructuralEquality<T> {
        private final String desc;
        private final Function<T, U> get;
        private final StructuralEquality<? super U> inner;

        FeatureEquality(String desc, Function<T, U> get, StructuralEquality<? super U> inner) {
            this.desc = desc;
            this.inner = inner;
            this.get = get;
        }

        private @Nullable U get(T value) {
            return get.apply(value);
        }

        @Override
        public boolean equals(T left, T right) {
            var leftInner = get(left);
            var rightInner = get(right);
            if (leftInner == null) return rightInner == null;
            if (rightInner == null) return false;
            return inner.equals(leftInner, rightInner);
        }

        @Override
        public void describe(Description description, T object) {
            description.appendText(desc).appendText("=>");
            describeNullable(description, inner, get.apply(object));
        }
    }

    record ListEquality<T>(StructuralEquality<T> equality) implements StructuralEquality<List<T>> {
        @Override
        public boolean equals(List<T> left, List<T> right) {
            if (left.size() != right.size()) return false;
            for (var i = 0; i < left.size(); i++) {
                if (!equality.equals(left.get(i), right.get(i))) return false;
            }
            return true;
        }

        @Override
        public void describe(Description description, List<T> object) {
            description.appendText("[");
            var separator = false;
            for (var value : object) {
                if (separator) description.appendText(", ");
                separator = true;
                equality.describe(description, value);
            }
            description.appendText("]");
        }
    }

    static final class EqualityMatcher<T> extends TypeSafeMatcher<T> {
        private final StructuralEquality<T> equality;
        private final T equalTo;

        EqualityMatcher(Class<T> klass, StructuralEquality<T> equality, T equalTo) {
            super(klass);
            this.equality = equality;
            this.equalTo = equalTo;
        }

        @Override
        public boolean matchesSafely(T actual) {
            if (actual == null) return false;
            return equality.equals(actual, equalTo);
        }

        @Override
        public void describeTo(Description description) {
            equality.describe(description, equalTo);
        }

        @Override
        protected void describeMismatchSafely(T value, Description description) {
            description.appendText("was ");
            describeNullable(description, equality, value);
        }
    }
}
