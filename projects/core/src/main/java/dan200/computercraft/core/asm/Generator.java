// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.methods.LuaMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;

/**
 * The underlying generator for {@link LuaFunction}-annotated methods.
 * <p>
 * The constructor {@link Generator#Generator(List, Function, Function)} takes in the type of interface to generate
 * (i.e. {@link LuaMethod}), the context arguments for this function (in the case of {@link LuaMethod}, this will just
 * be {@link ILuaContext}), a factory function (which invokes a method handle), and a "wrapper" function to lift a
 * function to execute on the main thread.
 * <p>
 * For each input function, the generator then fabricates a {@link MethodHandle} which performs the argument validation,
 * and then calls the factory function to convert it to the desired interface.
 *
 * @param <T> The type of the interface the generated classes implement.
 */
final class Generator<T> {
    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodHandle METHOD_RESULT_OF_VOID, METHOD_RESULT_OF_ONE, METHOD_RESULT_OF_MANY;

    private static final Map<Class<?>, ArgMethods> argMethods;
    private static final ArgMethods ARG_TABLE_UNSAFE;
    private static final MethodHandle ARG_GET_OBJECT, ARG_GET_ENUM, ARG_OPT_ENUM, ARG_GET_STRING_COERCED, ARG_GET_BYTES_COERCED;

    private record ArgMethods(MethodHandle get, MethodHandle opt) {
        public static ArgMethods of(Class<?> type, String name) throws ReflectiveOperationException {
            return new ArgMethods(
                LOOKUP.findVirtual(IArguments.class, "get" + name, MethodType.methodType(type, int.class)),
                LOOKUP.findVirtual(IArguments.class, "opt" + name, MethodType.methodType(Optional.class, int.class))
            );
        }
    }

    static void addArgType(Map<Class<?>, ArgMethods> types, Class<?> type, String name) throws ReflectiveOperationException {
        types.put(type, ArgMethods.of(type, name));
    }

    static {
        try {
            METHOD_RESULT_OF_VOID = LOOKUP.findStatic(MethodResult.class, "of", MethodType.methodType(MethodResult.class));
            METHOD_RESULT_OF_ONE = LOOKUP.findStatic(MethodResult.class, "of", MethodType.methodType(MethodResult.class, Object.class));
            METHOD_RESULT_OF_MANY = LOOKUP.findStatic(MethodResult.class, "of", MethodType.methodType(MethodResult.class, Object[].class));

            Map<Class<?>, ArgMethods> argMethodMap = new HashMap<>();
            addArgType(argMethodMap, int.class, "Int");
            addArgType(argMethodMap, boolean.class, "Boolean");
            addArgType(argMethodMap, double.class, "Double");
            addArgType(argMethodMap, long.class, "Long");
            addArgType(argMethodMap, Map.class, "Table");
            addArgType(argMethodMap, String.class, "String");
            addArgType(argMethodMap, ByteBuffer.class, "Bytes");
            argMethods = Map.copyOf(argMethodMap);

            ARG_TABLE_UNSAFE = ArgMethods.of(LuaTable.class, "TableUnsafe");
            ARG_GET_OBJECT = LOOKUP.findVirtual(IArguments.class, "get", MethodType.methodType(Object.class, int.class));
            ARG_GET_ENUM = LOOKUP.findVirtual(IArguments.class, "getEnum", MethodType.methodType(Enum.class, int.class, Class.class));
            ARG_OPT_ENUM = LOOKUP.findVirtual(IArguments.class, "optEnum", MethodType.methodType(Optional.class, int.class, Class.class));

            // Create a new Coerced<>(args.getStringCoerced(_)) function.
            var mkCoerced = LOOKUP.findConstructor(Coerced.class, MethodType.methodType(void.class, Object.class));
            ARG_GET_STRING_COERCED = MethodHandles.filterReturnValue(
                setReturn(LOOKUP.findVirtual(IArguments.class, "getStringCoerced", MethodType.methodType(String.class, int.class)), Object.class),
                mkCoerced
            );
            ARG_GET_BYTES_COERCED = MethodHandles.filterReturnValue(
                setReturn(LOOKUP.findVirtual(IArguments.class, "getBytesCoerced", MethodType.methodType(ByteBuffer.class, int.class)), Object.class),
                mkCoerced
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private final List<Class<?>> context;
    private final List<Class<?>> contextWithArguments;
    private final MethodHandle argumentGetter;
    private final List<MethodHandle> contextGetters;

    private final Function<MethodHandle, T> factory;
    private final Function<T, T> wrap;

    private final LoadingCache<Method, Optional<T>> methodCache = CacheBuilder
        .newBuilder()
        .build(CacheLoader.from(catching(this::build, Optional.empty())));

    Generator(List<Class<?>> context, Function<MethodHandle, T> factory, Function<T, T> wrap) {
        this.context = context;
        this.factory = factory;
        this.wrap = wrap;

        var contextWithArguments = this.contextWithArguments = new ArrayList<>(context.size() + 1);
        contextWithArguments.addAll(context);
        contextWithArguments.add(IArguments.class);

        // Prepare a series of getters of the type (context..., IArguments) -> _ (or some prefix of this), for
        // extracting a single context value.
        argumentGetter = MethodHandles.dropArguments(MethodHandles.identity(IArguments.class), 0, context);

        var contextGetters = this.contextGetters = new ArrayList<>(context.size());
        for (var i = 0; i < context.size(); i++) {
            var getter = MethodHandles.identity(context.get(i));
            if (i > 0) getter = MethodHandles.dropArguments(getter, 0, contextWithArguments.subList(0, i));
            contextGetters.add(getter);
        }
    }

    Optional<T> getMethod(Method method) {
        return methodCache.getUnchecked(method);
    }

    private Optional<T> build(Method method) {
        var name = method.getDeclaringClass().getName() + "." + method.getName();
        var modifiers = method.getModifiers();

        // Instance methods must be final - this prevents them being overridden and potentially exposed twice.
        if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
            LOG.warn("Lua Method {} should be final.", name);
        }

        if (!Modifier.isPublic(modifiers)) {
            LOG.error("Lua Method {} should be a public method.", name);
            return Optional.empty();
        }

        if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            LOG.error("Lua Method {} should be on a public class.", name);
            return Optional.empty();
        }

        LOG.debug("Generating method wrapper for {}.", name);

        var exceptions = method.getExceptionTypes();
        for (var exception : exceptions) {
            if (exception != LuaException.class) {
                LOG.error("Lua Method {} cannot throw {}.", name, exception.getName());
                return Optional.empty();
            }
        }

        var annotation = method.getAnnotation(LuaFunction.class);
        if (annotation.unsafe() && annotation.mainThread()) {
            LOG.error("Lua Method {} cannot use unsafe and mainThread", name);
            return Optional.empty();
        }

        try {
            var originalHandle = LOOKUP.unreflect(method);

            List<Type> parameters;
            if (Modifier.isStatic(modifiers)) {
                var allParameters = method.getGenericParameterTypes();
                parameters = Arrays.asList(allParameters).subList(1, allParameters.length);
            } else {
                parameters = Arrays.asList(method.getGenericParameterTypes());
            }

            var handle = buildMethodHandle(method, originalHandle, parameters, annotation.unsafe());
            if (handle == null) return Optional.empty();

            var instance = factory.apply(handle);
            return Optional.of(annotation.mainThread() ? wrap.apply(instance) : instance);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.error("Error generating wrapper for {}.", name, e);
            return Optional.empty();
        }
    }

    /**
     * Convert the given handle from type {@code (target, args...) -> ret} to {@code (Object, context..., IArguments) -> MethodResult},
     * inserting calls to {@link IArguments}'s getters, and wrapping the result with {@link MethodResult#of()}.
     *
     * @param method         The original method, for error reporting.
     * @param handle         The method handle to wrap.
     * @param parameterTypes The generic parameter types to this method. This should have the same type as the {@code handle}.
     * @param unsafe         Whether to allow unsafe argument getters.
     * @return The wrapped method handle.
     */
    @Nullable
    private MethodHandle buildMethodHandle(Member method, MethodHandle handle, List<Type> parameterTypes, boolean unsafe) {
        if (handle.type().parameterCount() != parameterTypes.size() + 1) {
            throw new IllegalArgumentException("Argument lists are mismatched");
        }

        // We start off with a method handle of type (target, args...) -> _. We then append the context and IArguments
        // to the end, leaving a handle with type (target, args..., context..., IArguments) -> _.
        handle = MethodHandles.dropArguments(handle, handle.type().parameterCount(), contextWithArguments);

        // Then for each argument, generate a method handle of type (context..., IArguments) -> _, which is used to
        // extract this argument.
        var argCount = 0;
        List<MethodHandle> argSelectors = new ArrayList<>(parameterTypes.size());
        for (var paramType : parameterTypes) {
            var paramClass = Reflect.getRawType(method, paramType, true);
            if (paramClass == null) return null;

            // We first generate a method handle of type (context..., IArguments) -> _, which is used to extract this
            // argument.
            MethodHandle argSelector;
            if (paramClass == IArguments.class) {
                argSelector = argumentGetter;
            } else {
                var idx = context.indexOf(paramClass);
                if (idx >= 0) {
                    argSelector = contextGetters.get(idx);
                } else {
                    var selector = loadArg(method, unsafe, paramClass, paramType, argCount++);
                    if (selector == null) return null;
                    argSelector = MethodHandles.filterReturnValue(argumentGetter, selector);
                }
            }

            argSelectors.add(argSelector);
        }

        // Fold over the original method's arguments, excluding the target in reverse. For each argument, we reduce
        // a method of type type (target, args..., arg_n, context..., IArguments) -> _ to (target, args..., context..., IArguments) -> _
        // until eventually we've flattened the whole list.
        for (var i = parameterTypes.size() - 1; i >= 0; i--) {
            handle = MethodHandles.foldArguments(handle, i + 1, argSelectors.get(i));
        }

        // Then cast the target to Object, so it's compatible with the desired type.
        handle = handle.asType(handle.type().changeParameterType(0, Object.class));

        // Finally wrap the returned value into a MethodResult.
        var type = handle.type();
        var ret = type.returnType();
        if (ret == MethodResult.class) {
            return handle;
        } else if (ret == void.class) {
            return MethodHandles.filterReturnValue(handle, METHOD_RESULT_OF_VOID);
        } else if (ret == Object[].class) {
            return MethodHandles.filterReturnValue(handle, METHOD_RESULT_OF_MANY);
        } else {
            return MethodHandles.filterReturnValue(handle.asType(type.changeReturnType(Object.class)), METHOD_RESULT_OF_ONE);
        }
    }

    @Nullable
    private static MethodHandle loadArg(Member method, boolean unsafe, Class<?> argType, Type genericArg, int argIndex) {
        if (argType == Coerced.class) {
            var klass = Reflect.getRawType(method, TypeToken.of(genericArg).resolveType(Reflect.COERCED_IN).getType(), false);
            if (klass == null) return null;

            if (klass == String.class) return MethodHandles.insertArguments(ARG_GET_STRING_COERCED, 1, argIndex);
            if (klass == ByteBuffer.class) return MethodHandles.insertArguments(ARG_GET_BYTES_COERCED, 1, argIndex);
        }

        if (argType == Optional.class) {
            var optType = Reflect.getRawType(method, TypeToken.of(genericArg).resolveType(Reflect.OPTIONAL_IN).getType(), false);
            if (optType == null) return null;

            if (Enum.class.isAssignableFrom(optType) && optType != Enum.class) {
                return MethodHandles.insertArguments(ARG_OPT_ENUM, 1, argIndex, optType);
            }

            var getter = getArgMethods(Primitives.unwrap(optType), unsafe);
            if (getter != null) return MethodHandles.insertArguments(getter.opt(), 1, argIndex);
        }

        if (Enum.class.isAssignableFrom(argType) && argType != Enum.class) {
            return setReturn(MethodHandles.insertArguments(ARG_GET_ENUM, 1, argIndex, argType), argType);
        }

        if (argType == Object.class) return MethodHandles.insertArguments(ARG_GET_OBJECT, 1, argIndex);

        // Check we don't have a non-wildcard generic.
        if (Reflect.getRawType(method, genericArg, false) == null) return null;

        var getter = getArgMethods(argType, unsafe);
        if (getter != null) return MethodHandles.insertArguments(getter.get(), 1, argIndex);

        LOG.error("Unknown parameter type {} for method {}.{}.", argType.getName(), method.getDeclaringClass().getName(), method.getName());
        return null;
    }

    private static MethodHandle setReturn(MethodHandle handle, Class<?> retTy) {
        return handle.asType(handle.type().changeReturnType(retTy));
    }

    private static @Nullable ArgMethods getArgMethods(Class<?> type, boolean unsafe) {
        var getter = argMethods.get(type);
        if (getter != null) return getter;
        if (type == LuaTable.class && unsafe) return ARG_TABLE_UNSAFE;
        return null;
    }

    @SuppressWarnings("Guava")
    static <T, U> com.google.common.base.Function<T, U> catching(Function<T, U> function, U def) {
        return x -> {
            try {
                return function.apply(x);
            } catch (Exception | LinkageError e) {
                // LinkageError due to possible codegen bugs and NoClassDefFoundError. The latter occurs when fetching
                // methods on a class which references non-existent (i.e. client-only) types.
                LOG.error("Error generating @LuaFunctions", e);
                return def;
            }
        };
    }
}
