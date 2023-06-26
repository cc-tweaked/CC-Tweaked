// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.methods;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Finds methods available on an object and yields them.
 *
 * @param <T> The type of method, such as {@link LuaMethod} or {@link PeripheralMethod}.
 */
public interface MethodSupplier<T> {
    /**
     * Iterate over methods available on an object, ignoring {@link ObjectSource}s.
     *
     * @param object   The object to find methods for.
     * @param consumer The consumer to call for each method.
     * @return Whether any methods were found.
     */
    boolean forEachSelfMethod(Object object, UntargetedConsumer<T> consumer);

    /**
     * Generate a map of all methods targeting the current object, ignoring {@link ObjectSource}s.
     *
     * @param object The object to find methods for.
     * @return A map of all methods on the object.
     */
    default Map<String, T> getSelfMethods(Object object) {
        var map = new HashMap<String, T>();
        forEachSelfMethod(object, (n, m, i) -> map.put(n, m));
        return map;
    }

    /**
     * Iterate over all methods on an object, including {@link ObjectSource}s.
     *
     * @param object   The object to find methods for.
     * @param consumer The consumer to call for each method.
     * @return Whether any methods were found.
     */
    boolean forEachMethod(Object object, TargetedConsumer<T> consumer);

    /**
     * A function which is called for each method on an object.
     *
     * @param <T> The type of method, such as {@link LuaMethod} or {@link PeripheralMethod}.
     * @see #forEachSelfMethod(Object, UntargetedConsumer)
     */
    @FunctionalInterface
    interface UntargetedConsumer<T> {
        /**
         * Consume a method on an object.
         *
         * @param name   The name of this method.
         * @param method The actual method definition.
         * @param info   Additional information about the method, such as whether it will yield. May be {@code null}.
         */
        void accept(String name, T method, @Nullable NamedMethod<T> info);
    }

    /**
     * A function which is called for each method on an object and possibly nested objects.
     *
     * @param <T> The type of method, such as {@link LuaMethod} or {@link PeripheralMethod}.
     * @see #forEachMethod(Object, TargetedConsumer)
     */
    @FunctionalInterface
    interface TargetedConsumer<T> {
        /**
         * Consume a method on an object.
         *
         * @param object The object this method targets, should be passed to the method's {@code apply(...)} function.
         * @param name   The name of this method.
         * @param method The actual method definition.
         * @param info   Additional information about the method, such as whether it will yield. May be {@code null}.
         */
        void accept(Object object, String name, T method, @Nullable NamedMethod<T> info);
    }
}
