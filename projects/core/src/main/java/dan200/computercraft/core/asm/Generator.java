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
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

/**
 * The underlying generator for {@link LuaFunction}-annotated methods.
 * <p>
 * The constructor {@link Generator#Generator(Class, List, Function)} takes in the type of interface to generate (i.e.
 * {@link LuaMethod}), the context arguments for this function (in the case of {@link LuaMethod}, this will just be
 * {@link ILuaContext}) and a "wrapper" function to lift a function to execute on the main thread.
 * <p>
 * The generated class then implements this interface - the {@code apply} method calls the appropriate methods on
 * {@link IArguments} to extract the arguments, and then calls the original method.
 * <p>
 * As the method is not guaranteed to come from the same classloader, we cannot call the method directly, as that may
 * result in linkage errors. We instead inject a {@link MethodHandle} into the class as a dynamic constant, and then
 * call the method with {@link MethodHandle#invokeExact(Object...)}. The method handle is constant, and so this has
 * equivalent performance to the direct call.
 *
 * @param <T> The type of the interface the generated classes implement.
 */
final class Generator<T> {
    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final String METHOD_NAME = "apply";
    private static final String[] EXCEPTIONS = new String[]{ Type.getInternalName(LuaException.class) };

    private static final String INTERNAL_METHOD_RESULT = Type.getInternalName(MethodResult.class);
    private static final String DESC_METHOD_RESULT = Type.getDescriptor(MethodResult.class);

    private static final String INTERNAL_ARGUMENTS = Type.getInternalName(IArguments.class);
    private static final String DESC_ARGUMENTS = Type.getDescriptor(IArguments.class);

    private static final String INTERNAL_COERCED = Type.getInternalName(Coerced.class);

    private static final ConstantDynamic METHOD_CONSTANT = new ConstantDynamic(ConstantDescs.DEFAULT_NAME, MethodHandle.class.descriptorString(), new Handle(
        H_INVOKESTATIC, Type.getInternalName(MethodHandles.class), "classData",
        MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class).descriptorString(), false
    ));

    private final Class<T> base;
    private final List<Class<?>> context;

    private final String[] interfaces;
    private final String methodDesc;
    private final String classPrefix;

    private final Function<T, T> wrap;

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

        classPrefix = Generator.class.getPackageName() + "." + base.getSimpleName() + "$";
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

        // We have some rather ugly handling of static methods in both here and the main generate function. Static methods
        // only come from generic sources, so this should be safe.
        var target = Modifier.isStatic(modifiers) ? method.getParameterTypes()[0] : method.getDeclaringClass();

        try {
            var handle = LOOKUP.unreflect(method);

            // Convert the handle from one of the form (target, ...) -> ret type to (Object, ...) -> Object. This both
            // handles the boxing of primitives for us, and ensures our bytecode does not reference any external types.
            // We could handle the conversion to MethodResult here too, but it doesn't feel worth it.
            var widenedHandle = handle.asType(widenMethodType(handle.type(), target));

            var bytes = generate(classPrefix + method.getName(), target, method, widenedHandle.type().descriptorString(), annotation.unsafe());
            if (bytes == null) return Optional.empty();

            var klass = LOOKUP.defineHiddenClassWithClassData(bytes, widenedHandle, true).lookupClass();

            var instance = klass.asSubclass(base).getDeclaredConstructor().newInstance();
            return Optional.of(annotation.mainThread() ? wrap.apply(instance) : instance);
        } catch (ReflectiveOperationException | ClassFormatError | RuntimeException e) {
            LOG.error("Error generating wrapper for {}.", name, e);
            return Optional.empty();
        }
    }

    private static MethodType widenMethodType(MethodType source, Class<?> target) {
        // Treat the target argument as just Object - we'll do the cast in the method handle.
        var args = source.parameterArray();
        for (var i = 0; i < args.length; i++) {
            if (args[i] == target) args[i] = Object.class;
        }

        // And convert the return value to Object if needed.
        var ret = source.returnType();
        return ret == void.class || ret == MethodResult.class || ret == Object[].class
            ? MethodType.methodType(ret, args)
            : MethodType.methodType(Object.class, args);
    }

    @Nullable
    private byte[] generate(String className, Class<?> target, Method targetMethod, String targetDescriptor, boolean unsafe) {
        var internalName = className.replace(".", "/");

        // Construct a public final class which extends Object and implements MethodInstance.Delegate
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, internalName, null, "java/lang/Object", interfaces);
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

            mw.visitLdcInsn(METHOD_CONSTANT);

            // If we're an instance method, load the target as the first argument.
            if (!Modifier.isStatic(targetMethod.getModifiers())) mw.visitVarInsn(ALOAD, 1);

            var argIndex = 0;
            for (var genericArg : targetMethod.getGenericParameterTypes()) {
                var loadedArg = loadArg(mw, target, targetMethod, unsafe, genericArg, argIndex);
                if (loadedArg == null) return null;
                if (loadedArg) argIndex++;
            }

            mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", targetDescriptor, false);

            // We allow a reasonable amount of flexibility on the return value's type. Alongside the obvious MethodResult,
            // we convert basic types into an immediate result.
            var ret = targetMethod.getReturnType();
            if (ret != MethodResult.class) {
                if (ret == void.class) {
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "()" + DESC_METHOD_RESULT, false);
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

    @Nullable
    private Boolean loadArg(MethodVisitor mw, Class<?> target, Method method, boolean unsafe, java.lang.reflect.Type genericArg, int argIndex) {
        if (genericArg == target) {
            mw.visitVarInsn(ALOAD, 1);
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

        if (arg == Coerced.class) {
            var klass = Reflect.getRawType(method, TypeToken.of(genericArg).resolveType(Reflect.COERCED_IN).getType(), false);
            if (klass == null) return null;

            if (klass == String.class) {
                mw.visitTypeInsn(NEW, INTERNAL_COERCED);
                mw.visitInsn(DUP);
                mw.visitVarInsn(ALOAD, 2 + context.size());
                Reflect.loadInt(mw, argIndex);
                mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "getStringCoerced", "(I)Ljava/lang/String;", true);
                mw.visitMethodInsn(INVOKESPECIAL, INTERNAL_COERCED, "<init>", "(Ljava/lang/Object;)V", false);
                return true;
            }
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
