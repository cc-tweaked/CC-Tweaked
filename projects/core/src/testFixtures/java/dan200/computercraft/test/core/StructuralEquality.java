// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;

/**
 * A basic mechanism for checking equality of two objects, suitable for use with Hamcrest {@linkplain Matcher matchers}.
 * <p>
 * This is intended for when an object does not override {@link Object#equals(Object)} itself,
 *
 * @param <T> The type of the value to check.
 */
public interface StructuralEquality<T> {
    /**
     * Check if two non-null values are equal.
     *
     * @param left  The first value to check.
     * @param right The second value to check.
     * @return If these values are equal.
     */
    boolean equals(T left, T right);

    /**
     * Describe this value.
     *
     * @param description The description to write to.
     * @param object      The object to describe.
     */
    void describe(Description description, T object);

    /**
     * Lift this equality to a list of values.
     *
     * @return A equality for a list of values.
     */
    default StructuralEquality<List<T>> list() {
        return new StructuralEqualities.ListEquality<>(this);
    }

    /**
     * Convert this equality instance to a {@link Matcher}.
     *
     * @param klass    The expected type of this object.
     * @param expected The expected value.
     * @return A matcher which checks if its input is equal to the given value.
     */
    default Matcher<T> asMatcher(Class<T> klass, T expected) {
        return new StructuralEqualities.EqualityMatcher<>(klass, this, expected);
    }

    /**
     * The default {@link StructuralEquality} implementation, which just uses {@link Object#equals(Object)}.
     *
     * @return The default equality.
     */
    static StructuralEquality<Object> defaultEquality() {
        return StructuralEqualities.DEFAULT;
    }

    /**
     * Checks all equalities match. This is intended for use with {@link #at(String, Function, StructuralEquality)}, to
     * check the structure of an object.
     *
     * @param equalities The equalities which should match.
     * @param <T>        The type of the object to match.
     * @return The newly created {@link StructuralEquality} object.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> StructuralEquality<T> all(StructuralEquality<T>... equalities) {
        return new StructuralEqualities.AllEquality<>(List.of(equalities));
    }

    /**
     * Create an equality which checks if {@code f(x) = f(y)}, where {@code f} is some projection function (such as
     * reading a field).
     *
     * @param desc    The description of this projection.
     * @param project The projection function.
     * @param inner   The inner equality,
     * @param <T>     The type of the object to check.
     * @param <U>     The type of the "inner" object, projected out by {@code f}
     * @return The newly created {@link StructuralEquality} object.
     */
    static <T, U> StructuralEquality<T> at(String desc, Function<T, U> project, StructuralEquality<? super U> inner) {
        return new StructuralEqualities.FeatureEquality<>(desc, project, inner);
    }

    /**
     * A simple version of {@link #at(String, Function, StructuralEquality)} which uses {@link #defaultEquality()} .
     *
     * @param desc    The description of this projection.
     * @param project The projection function.
     * @param <T>     The type of the object to check.
     * @param <U>     The type of the "inner" object, projected out by {@code f}
     * @return The newly created {@link StructuralEquality} object.
     */
    static <T, U> StructuralEquality<T> at(String desc, Function<T, U> project) {
        return at(desc, project, defaultEquality());
    }

    /**
     * A hacky version of {@link #at(String, Function, StructuralEquality)} which projects out a private field.
     *
     * @param klass     The class where the field is defined.
     * @param fieldName The name of the field.
     * @param inner     The inner equality.
     * @param <T>       The type of the object to check.
     * @param <U>       The type of the field's.
     * @return The newly created {@link StructuralEquality} object.
     */
    @SuppressWarnings("unchecked")
    static <T, U> StructuralEquality<T> field(Class<T> klass, String fieldName, StructuralEquality<U> inner) {
        Field field;
        try {
            field = klass.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot find field", e);
        }

        return new StructuralEqualities.FeatureEquality<>(fieldName, x -> {
            try {
                return (U) field.get(x);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read field", e);
            }
        }, inner);
    }

    /**
     * A hacky version of {@link #at(String, Function)} which projects out a private field.
     *
     * @param klass     The class where the field is defined.
     * @param fieldName The name of the field.
     * @param <T>       The type of the object to check.
     * @return The newly created {@link StructuralEquality} object.
     */
    static <T> StructuralEquality<T> field(Class<T> klass, String fieldName) {
        return field(klass, fieldName, defaultEquality());
    }
}
