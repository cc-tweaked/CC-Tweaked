/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.PeripheralType;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public final class Generator<T> {
    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);

    private static final AtomicInteger METHOD_ID = new AtomicInteger();

    private static final String METHOD_NAME = "apply";
    private static final String[] EXCEPTIONS = new String[]{ Type.getInternalName(LuaException.class) };

    private static final String INTERNAL_METHOD_RESULT = Type.getInternalName(MethodResult.class);
    private static final String DESC_METHOD_RESULT = Type.getDescriptor(MethodResult.class);

    private static final String INTERNAL_ARGUMENTS = Type.getInternalName(IArguments.class);
    private static final String DESC_ARGUMENTS = Type.getDescriptor(IArguments.class);

    private final Class<T> base;
    private final List<Class<?>> context;

    private final String[] interfaces;
    private final String methodDesc;

    private final Function<T, T> wrap;

    private final LoadingCache<Class<?>, List<NamedMethod<T>>> classCache = CacheBuilder
        .newBuilder()
        .build(CacheLoader.from(catching(this::build, Collections.emptyList())));

    private final LoadingCache<Method, Optional<T>> methodCache = CacheBuilder
        .newBuilder()
        .build(CacheLoader.from(catching(this::build, Optional.empty())));

    Generator(Class<T> base, List<Class<?>> context, Function<T, T> wrap) {
        this.base = base;
        this.context = context;
        interfaces = new String[]{ Type.getInternalName(base) };
        this.wrap = wrap;

        var methodDesc = new StringBuilder().append("(Ljava/lang/Object;");
        for (var klass : context) methodDesc.append(Type.getDescriptor(klass));
        methodDesc.append(DESC_ARGUMENTS).append(")").append(DESC_METHOD_RESULT);
        this.methodDesc = methodDesc.toString();
    }

    @Nonnull
    public List<NamedMethod<T>> getMethods(@Nonnull Class<?> klass) {
        try {
            return classCache.get(klass);
        } catch (ExecutionException e) {
            LOG.error("Error getting methods for {}.", klass.getName(), e.getCause());
            return Collections.emptyList();
        }
    }

    @Nonnull
    private List<NamedMethod<T>> build(Class<?> klass) {
        ArrayList<NamedMethod<T>> methods = null;
        for (var method : klass.getMethods()) {
            var annotation = method.getAnnotation(LuaFunction.class);
            if (annotation == null) continue;

            if (Modifier.isStatic(method.getModifiers())) {
                LOG.warn("LuaFunction method {}.{} should be an instance method.", method.getDeclaringClass(), method.getName());
                continue;
            }

            var instance = methodCache.getUnchecked(method).orElse(null);
            if (instance == null) continue;

            if (methods == null) methods = new ArrayList<>();
            addMethod(methods, method, annotation, null, instance);
        }

        for (var method : GenericMethod.all()) {
            if (!method.target.isAssignableFrom(klass)) continue;

            var instance = methodCache.getUnchecked(method.method).orElse(null);
            if (instance == null) continue;

            if (methods == null) methods = new ArrayList<>();
            addMethod(methods, method.method, method.annotation, method.peripheralType, instance);
        }

        if (methods == null) return Collections.emptyList();
        methods.trimToSize();
        return Collections.unmodifiableList(methods);
    }

    private void addMethod(List<NamedMethod<T>> methods, Method method, LuaFunction annotation, PeripheralType genericType, T instance) {
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

    @Nonnull
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

        // We have some rather ugly handling of static methods in both here and the main generate function. Static methods
        // only come from generic sources, so this should be safe.
        var target = Modifier.isStatic(modifiers) ? method.getParameterTypes()[0] : method.getDeclaringClass();

        try {
            var className = method.getDeclaringClass().getName() + "$cc$" + method.getName() + METHOD_ID.getAndIncrement();
            var bytes = generate(className, target, method, annotation.unsafe());
            if (bytes == null) return Optional.empty();

            var klass = DeclaringClassLoader.INSTANCE.define(className, bytes, method.getDeclaringClass().getProtectionDomain());

            var instance = klass.asSubclass(base).getDeclaredConstructor().newInstance();
            return Optional.of(annotation.mainThread() ? wrap.apply(instance) : instance);
        } catch (ReflectiveOperationException | ClassFormatError | RuntimeException e) {
            LOG.error("Error generating wrapper for {}.", name, e);
            return Optional.empty();
        }

    }

    @Nullable
    private byte[] generate(String className, Class<?> target, Method method, boolean unsafe) {
        var internalName = className.replace(".", "/");

        // Construct a public final class which extends Object and implements MethodInstance.Delegate
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, internalName, null, "java/lang/Object", interfaces);
        cw.visitSource("CC generated method", null);

        { // Constructor just invokes super.
            var mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mw.visitCode();
            mw.visitVarInsn(ALOAD, 0);
            mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mw.visitInsn(RETURN);
            mw.visitMaxs(0, 0);
            mw.visitEnd();
        }

        {
            var mw = cw.visitMethod(ACC_PUBLIC, METHOD_NAME, methodDesc, null, EXCEPTIONS);
            mw.visitCode();

            // If we're an instance method, load the this parameter.
            if (!Modifier.isStatic(method.getModifiers())) {
                mw.visitVarInsn(ALOAD, 1);
                mw.visitTypeInsn(CHECKCAST, Type.getInternalName(target));
            }

            var argIndex = 0;
            for (var genericArg : method.getGenericParameterTypes()) {
                var loadedArg = loadArg(mw, target, method, unsafe, genericArg, argIndex);
                if (loadedArg == null) return null;
                if (loadedArg) argIndex++;
            }

            mw.visitMethodInsn(
                Modifier.isStatic(method.getModifiers()) ? INVOKESTATIC : INVOKEVIRTUAL,
                Type.getInternalName(method.getDeclaringClass()), method.getName(),
                Type.getMethodDescriptor(method), false
            );

            // We allow a reasonable amount of flexibility on the return value's type. Alongside the obvious MethodResult,
            // we convert basic types into an immediate result.
            var ret = method.getReturnType();
            if (ret != MethodResult.class) {
                if (ret == void.class) {
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "()" + DESC_METHOD_RESULT, false);
                } else if (ret.isPrimitive()) {
                    var boxed = Primitives.wrap(ret);
                    mw.visitMethodInsn(INVOKESTATIC, Type.getInternalName(boxed), "valueOf", "(" + Type.getDescriptor(ret) + ")" + Type.getDescriptor(boxed), false);
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "(Ljava/lang/Object;)" + DESC_METHOD_RESULT, false);
                } else if (ret == Object[].class) {
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "([Ljava/lang/Object;)" + DESC_METHOD_RESULT, false);
                } else {
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "(Ljava/lang/Object;)" + DESC_METHOD_RESULT, false);
                }
            }

            mw.visitInsn(ARETURN);

            mw.visitMaxs(0, 0);
            mw.visitEnd();
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

    private Boolean loadArg(MethodVisitor mw, Class<?> target, Method method, boolean unsafe, java.lang.reflect.Type genericArg, int argIndex) {
        if (genericArg == target) {
            mw.visitVarInsn(ALOAD, 1);
            mw.visitTypeInsn(CHECKCAST, Type.getInternalName(target));
            return false;
        }

        var arg = Reflect.getRawType(method, genericArg, true);
        if (arg == null) return null;

        if (arg == IArguments.class) {
            mw.visitVarInsn(ALOAD, 2 + context.size());
            return false;
        }

        var idx = context.indexOf(arg);
        if (idx >= 0) {
            mw.visitVarInsn(ALOAD, 2 + idx);
            return false;
        }

        if (arg == Optional.class) {
            var klass = Reflect.getRawType(method, TypeToken.of(genericArg).resolveType(Reflect.OPTIONAL_IN).getType(), false);
            if (klass == null) return null;

            if (Enum.class.isAssignableFrom(klass) && klass != Enum.class) {
                mw.visitVarInsn(ALOAD, 2 + context.size());
                Reflect.loadInt(mw, argIndex);
                mw.visitLdcInsn(Type.getType(klass));
                mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "optEnum", "(ILjava/lang/Class;)Ljava/util/Optional;", true);
                return true;
            }

            var name = Reflect.getLuaName(Primitives.unwrap(klass), unsafe);
            if (name != null) {
                mw.visitVarInsn(ALOAD, 2 + context.size());
                Reflect.loadInt(mw, argIndex);
                mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "opt" + name, "(I)Ljava/util/Optional;", true);
                return true;
            }
        }

        if (Enum.class.isAssignableFrom(arg) && arg != Enum.class) {
            mw.visitVarInsn(ALOAD, 2 + context.size());
            Reflect.loadInt(mw, argIndex);
            mw.visitLdcInsn(Type.getType(arg));
            mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "getEnum", "(ILjava/lang/Class;)Ljava/lang/Enum;", true);
            mw.visitTypeInsn(CHECKCAST, Type.getInternalName(arg));
            return true;
        }

        var name = arg == Object.class ? "" : Reflect.getLuaName(arg, unsafe);
        if (name != null) {
            if (Reflect.getRawType(method, genericArg, false) == null) return null;

            mw.visitVarInsn(ALOAD, 2 + context.size());
            Reflect.loadInt(mw, argIndex);
            mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "get" + name, "(I)" + Type.getDescriptor(arg), true);
            return true;
        }

        LOG.error("Unknown parameter type {} for method {}.{}.",
            arg.getName(), method.getDeclaringClass().getName(), method.getName());
        return null;
    }

    @SuppressWarnings("Guava")
    private static <T, U> com.google.common.base.Function<T, U> catching(Function<T, U> function, U def) {
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
