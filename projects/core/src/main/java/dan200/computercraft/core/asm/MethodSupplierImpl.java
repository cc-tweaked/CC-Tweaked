// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.PeripheralType;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.NamedMethod;
import dan200.computercraft.core.methods.ObjectSource;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static dan200.computercraft.core.asm.Generator.catching;

final class MethodSupplierImpl<T> implements MethodSupplier<T> {
    private static final Logger LOG = LoggerFactory.getLogger(MethodSupplierImpl.class);

    private final List<GenericMethod> genericMethods;
    private final Generator<T> generator;
    private final IntCache<T> dynamic;
    private final Function<Object, String[]> dynamicMethods;

    private final LoadingCache<Class<?>, List<NamedMethod<T>>> classCache = CacheBuilder
        .newBuilder()
        .build(CacheLoader.from(catching(this::getMethodsImpl, List.of())));

    MethodSupplierImpl(
        List<GenericMethod> genericMethods,
        Generator<T> generator,
        IntCache<T> dynamic,
        Function<Object, String[]> dynamicMethods
    ) {
        this.genericMethods = genericMethods;
        this.generator = generator;
        this.dynamic = dynamic;
        this.dynamicMethods = dynamicMethods;
    }

    @Override
    public boolean forEachSelfMethod(Object object, UntargetedConsumer<T> consumer) {
        var methods = getMethods(object.getClass());
        for (var method : methods) consumer.accept(method.name(), method.method(), method);

        var dynamicMethods = this.dynamicMethods.apply(object);
        if (dynamicMethods != null) {
            for (var i = 0; i < dynamicMethods.length; i++) consumer.accept(dynamicMethods[i], dynamic.get(i), null);
        }

        return !methods.isEmpty() || dynamicMethods != null;
    }

    @Override
    public boolean forEachMethod(Object object, TargetedConsumer<T> consumer) {
        var methods = getMethods(object.getClass());
        for (var method : methods) consumer.accept(object, method.name(), method.method(), method);

        var hasMethods = !methods.isEmpty();

        if (object instanceof ObjectSource source) {
            for (var extra : source.getExtra()) {
                var extraMethods = getMethods(extra.getClass());
                if (!extraMethods.isEmpty()) hasMethods = true;
                for (var method : extraMethods) consumer.accept(extra, method.name(), method.method(), method);
            }
        }

        var dynamicMethods = this.dynamicMethods.apply(object);
        if (dynamicMethods != null) {
            hasMethods = true;
            for (var i = 0; i < dynamicMethods.length; i++) {
                consumer.accept(object, dynamicMethods[i], dynamic.get(i), null);
            }
        }

        return hasMethods;
    }

    @VisibleForTesting
    List<NamedMethod<T>> getMethods(Class<?> klass) {
        try {
            return classCache.get(klass);
        } catch (ExecutionException e) {
            LOG.error("Error getting methods for {}.", klass.getName(), e.getCause());
            return List.of();
        }
    }

    private List<NamedMethod<T>> getMethodsImpl(Class<?> klass) {
        ArrayList<NamedMethod<T>> methods = null;

        // Find all methods on the current class
        for (var method : klass.getMethods()) {
            var annotation = method.getAnnotation(LuaFunction.class);
            if (annotation == null) continue;

            if (Modifier.isStatic(method.getModifiers())) {
                LOG.warn("LuaFunction method {}.{} should be an instance method.", method.getDeclaringClass(), method.getName());
                continue;
            }

            var instance = generator.getMethod(method).orElse(null);
            if (instance == null) continue;

            if (methods == null) methods = new ArrayList<>();
            addMethod(methods, method, annotation, null, instance);
        }

        // Inject generic methods
        for (var method : genericMethods) {
            if (!method.target.isAssignableFrom(klass)) continue;

            var instance = generator.getMethod(method.method).orElse(null);
            if (instance == null) continue;

            if (methods == null) methods = new ArrayList<>();
            addMethod(methods, method.method, method.annotation, method.peripheralType, instance);
        }

        if (methods == null) return List.of();
        methods.trimToSize();
        return Collections.unmodifiableList(methods);
    }

    private void addMethod(List<NamedMethod<T>> methods, Method method, LuaFunction annotation, @Nullable PeripheralType genericType, T instance) {
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
