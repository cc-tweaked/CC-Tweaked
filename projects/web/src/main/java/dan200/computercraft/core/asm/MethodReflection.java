// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.PeripheralType;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.NamedMethod;
import org.jspecify.annotations.Nullable;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.Metaprogramming;
import org.teavm.metaprogramming.Value;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Compile-time generation of {@link LuaMethod} methods.
 *
 * @see TLuaMethodSupplier
 * @see StaticGenerator
 */
@CompileTime
public class MethodReflection {
    @Meta
    public static native boolean getMethods(Class<?> type, Consumer<NamedMethod<LuaMethod>> make);

    private static void getMethods(IntrospectClass<?> klass, Value<Consumer<NamedMethod<LuaMethod>>> make) {
        var result = getMethodsImpl(klass, make);
        //  Using "unsupportedCase" here causes us to skip generating any code and just return null. While null isn't
        // a boolean, it's still false-y and thus has the same effect in the generated JS!
        if (!result) Metaprogramming.unsupportedCase();
        Metaprogramming.exit(() -> result);
    }

    private static boolean getMethodsImpl(IntrospectClass<?> klass, Value<Consumer<NamedMethod<LuaMethod>>> make) {
        if (!klass.name().startsWith("dan200.computercraft.") && !klass.name().startsWith("cc.tweaked.web.peripheral")) {
            return false;
        }
        if (klass.name().contains("lambda")) return false;

        Class<?> actualClass;
        try {
            actualClass = Metaprogramming.environment().classLoader().loadClass(klass.name());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        var methods = Internal.getMethods(actualClass);
        for (var method : methods) {
            var name = method.name();
            var nonYielding = method.nonYielding();
            var actualField = Metaprogramming.accessor(method.method().field("INSTANCE"));

            Metaprogramming.emit(() -> make.get().accept(new NamedMethod<>(name, (LuaMethod) actualField.get(null), nonYielding, null)));
        }

        return !methods.isEmpty();
    }

    private static final class Internal {
        private static final LoadingCache<Class<?>, List<NamedMethod<IntrospectClass<LuaMethod>>>> CLASS_CACHE = CacheBuilder
            .newBuilder()
            .build(CacheLoader.from(Internal::getMethodsImpl));

        private static final StaticGenerator<LuaMethod> GENERATOR = new StaticGenerator<>(
            LuaMethod.class, List.of(ILuaContext.class), Internal::createClass
        );

        static List<NamedMethod<IntrospectClass<LuaMethod>>> getMethods(Class<?> klass) {
            try {
                return CLASS_CACHE.get(klass);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        private static IntrospectClass<?> createClass(byte[] bytes) {
            /*
             StaticGenerator is not declared to be @CompileTime, to ensure it loads in the same module/classloader as
             other files in this package. This means it can't call Metaprogramming.createClass directly, as that's
             only available to @CompileTime classes.

             We need to use an explicit call (rather than a MethodReference), as TeaVM doesn't correctly rewrite the
             latter.
            */
            return Metaprogramming.environment().createClass(bytes);
        }

        private static List<NamedMethod<IntrospectClass<LuaMethod>>> getMethodsImpl(Class<?> klass) {
            ArrayList<NamedMethod<IntrospectClass<LuaMethod>>> methods = null;

            // Find all methods on the current class
            for (var method : klass.getMethods()) {
                var annotation = method.getAnnotation(LuaFunction.class);
                if (annotation == null) continue;

                if (Modifier.isStatic(method.getModifiers())) {
                    System.err.printf("LuaFunction method %s.%s should be an instance method.\n", method.getDeclaringClass(), method.getName());
                    continue;
                }

                var instance = GENERATOR.getMethod(method).orElse(null);
                if (instance == null) continue;

                if (methods == null) methods = new ArrayList<>();
                addMethod(methods, method, annotation, null, instance);
            }

            if (methods == null) return List.of();
            methods.trimToSize();
            return Collections.unmodifiableList(methods);
        }

        private static void addMethod(List<NamedMethod<IntrospectClass<LuaMethod>>> methods, Method method, LuaFunction annotation, @Nullable PeripheralType genericType, IntrospectClass<LuaMethod> instance) {
            var names = annotation.value();
            var isSimple = method.getReturnType() != MethodResult.class && !annotation.mainThread();
            if (names.length == 0) {
                methods.add(new NamedMethod<>(method.getName(), instance, isSimple, genericType));
            } else {
                for (var name : names) {
                    methods.add(new NamedMethod<>(name, instance, isSimple, genericType));
                }
            }
        }
    }
}
