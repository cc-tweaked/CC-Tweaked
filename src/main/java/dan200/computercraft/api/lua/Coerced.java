// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.lua;

import java.util.Objects;

/**
 * A wrapper type for "coerced" values.
 * <p>
 * This is designed to be used with {@link LuaFunction} annotated functions, to mark an argument as being coerced to
 * the given type, rather than requiring an exact type.
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * @LuaFunction
 * public final void doSomething(Coerced<String> myString) {
 *   var value = myString.value();
 * }
 * }</pre>
 *
 * @param <T> The type of the underlying value.
 * @see IArguments#getStringCoerced(int)
 */
public final class Coerced<T> {
    private final T value;

    public Coerced(T value) {
        this.value = value;
    }

    public T value() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof Coerced && Objects.equals(value, ((Coerced<?>) obj).value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
