// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A generic method is a method belonging to a {@link GenericSource} with a known target.
 */
public final class GenericMethod {
    private static final Logger LOG = LoggerFactory.getLogger(GenericMethod.class);

    final GenericSource source;
    final Method method;
    final LuaFunction annotation;
    final Class<?> target;
    final @Nullable PeripheralType peripheralType;

    private GenericMethod(GenericSource source, Method method, LuaFunction annotation, Class<?> target, @Nullable PeripheralType peripheralType) {
        this.source = source;
        this.method = method;
        this.annotation = annotation;
        this.target = target;
        this.peripheralType = peripheralType;
    }

    public String id() {
        return source.id() + "#" + name();
    }

    public String name() {
        return method.getName();
    }

    /**
     * Find all public static methods annotated with {@link LuaFunction} which belong to a {@link GenericSource}.
     *
     * @param source The given generic source.
     * @return All available generic methods.
     */
    public static Stream<GenericMethod> getMethods(GenericSource source) {
        Class<?> klass = source.getClass();
        var type = source instanceof GenericPeripheral generic ? generic.getType() : null;

        return Arrays.stream(klass.getMethods())
            .map(method -> {
                var annotation = method.getAnnotation(LuaFunction.class);
                if (annotation == null) return null;

                var types = method.getGenericParameterTypes();
                if (types.length == 0) {
                    LOG.error("GenericSource method {}.{} has no parameters.", method.getDeclaringClass(), method.getName());
                    return null;
                }

                var target = Reflect.getRawType(method, types[0], false);
                if (target == null) return null;

                return new GenericMethod(source, method, annotation, target, type);
            })
            .filter(Objects::nonNull);
    }
}
