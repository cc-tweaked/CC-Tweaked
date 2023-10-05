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
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.teavm.metaprogramming.ReflectClass;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

/**
 * The underlying generator for {@link LuaFunction}-annotated methods.
 * <p>
 * The constructor {@link StaticGenerator#StaticGenerator(Class, List, Function)} takes in the type of interface to generate (i.e.
 * {@link LuaMethod}) and the context arguments for this function (in the case of {@link LuaMethod}, this will just be
 * {@link ILuaContext}).
 * <p>
 * The generated class then implements this interface - the {@code apply} method calls the appropriate methods on
 * {@link IArguments} to extract the arguments, and then calls the original method.
 *
 * @param <T> The type of the interface the generated classes implement.
 */
public final class StaticGenerator<T> {
    private static final String METHOD_NAME = "apply";
    private static final String[] EXCEPTIONS = new String[]{Type.getInternalName(LuaException.class)};

    private static final String INTERNAL_METHOD_RESULT = Type.getInternalName(MethodResult.class);
    private static final String DESC_METHOD_RESULT = Type.getDescriptor(MethodResult.class);

    private static final String INTERNAL_ARGUMENTS = Type.getInternalName(IArguments.class);
    private static final String DESC_ARGUMENTS = Type.getDescriptor(IArguments.class);

    private static final String INTERNAL_COERCED = Type.getInternalName(Coerced.class);

    private final Class<T> base;
    private final List<Class<?>> context;

    private final String[] interfaces;
    private final String methodDesc;
    private final String classPrefix;

    private final Function<byte[], ReflectClass<?>> createClass;

    private final LoadingCache<Method, Optional<ReflectClass<T>>> methodCache = CacheBuilder
        .newBuilder()
        .build(CacheLoader.from(catching(this::build, Optional.empty())));

    public StaticGenerator(Class<T> base, List<Class<?>> context, Function<byte[], ReflectClass<?>> createClass) {
        this.base = base;
        this.context = context;
        this.createClass = createClass;

        interfaces = new String[]{Type.getInternalName(base)};

        var methodDesc = new StringBuilder().append("(Ljava/lang/Object;");
        for (var klass : context) methodDesc.append(Type.getDescriptor(klass));
        methodDesc.append(DESC_ARGUMENTS).append(")").append(DESC_METHOD_RESULT);
        this.methodDesc = methodDesc.toString();

        classPrefix = StaticGenerator.class.getPackageName() + "." + base.getSimpleName() + "$";
    }

    public Optional<ReflectClass<T>> getMethod(Method method) {
        return methodCache.getUnchecked(method);
    }

    private Optional<ReflectClass<T>> build(Method method) {
        var name = method.getDeclaringClass().getName() + "." + method.getName();
        var modifiers = method.getModifiers();

        // Instance methods must be final - this prevents them being overridden and potentially exposed twice.
        if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
            System.err.printf("Lua Method %s should be final.\n", name);
        }

        if (!Modifier.isPublic(modifiers)) {
            System.err.printf("Lua Method %s should be a public method.\n", name);
            return Optional.empty();
        }

        if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            System.err.printf("Lua Method %s should be on a public class.\n", name);
            return Optional.empty();
        }

        var exceptions = method.getExceptionTypes();
        for (var exception : exceptions) {
            if (exception != LuaException.class) {
                System.err.printf("Lua Method %s cannot throw %s.\n", name, exception.getName());
                return Optional.empty();
            }
        }

        var annotation = method.getAnnotation(LuaFunction.class);
        if (annotation.unsafe() && annotation.mainThread()) {
            System.err.printf("Lua Method %s cannot use unsafe and mainThread.\n", name);
            return Optional.empty();
        }

        // We have some rather ugly handling of static methods in both here and the main generate function. Static methods
        // only come from generic sources, so this should be safe.
        var target = Modifier.isStatic(modifiers) ? method.getParameterTypes()[0] : method.getDeclaringClass();

        try {
            var bytes = generate(classPrefix + method.getDeclaringClass().getSimpleName() + "$" + method.getName(), target, method, annotation.unsafe());
            if (bytes == null) return Optional.empty();

            return Optional.of(createClass.apply(bytes).asSubclass(base));
        } catch (ClassFormatError | RuntimeException e) {
            System.err.printf("Error generating %s\n", name);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Nullable
    private byte[] generate(String className, Class<?> target, Method targetMethod, boolean unsafe) {
        var internalName = className.replace(".", "/");

        // Construct a public final class which extends Object and implements MethodInstance.Delegate
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, internalName, null, "java/lang/Object", interfaces);
        cw.visitSource("CC generated method", null);

        cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "INSTANCE", "L" + internalName + ";", null, null).visitEnd();

        { // Constructor just invokes super.
            var mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mw.visitCode();
            mw.visitVarInsn(ALOAD, 0);
            mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mw.visitInsn(RETURN);
            mw.visitMaxs(0, 0);
            mw.visitEnd();
        }

        // Static initialiser sets the INSTANCE field.
        {
            var mw = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mw.visitCode();
            mw.visitTypeInsn(NEW, internalName);
            mw.visitInsn(DUP);
            mw.visitMethodInsn(INVOKESPECIAL, internalName, "<init>", "()V", false);
            mw.visitFieldInsn(PUTSTATIC, internalName, "INSTANCE", "L" + internalName + ";");
            mw.visitInsn(RETURN);
            mw.visitMaxs(0, 0);
            mw.visitEnd();
        }

        {
            var mw = cw.visitMethod(ACC_PUBLIC, METHOD_NAME, methodDesc, null, EXCEPTIONS);
            mw.visitCode();

            // If we're an instance method, load the target as the first argument.
            if (!Modifier.isStatic(targetMethod.getModifiers())) {
                mw.visitVarInsn(ALOAD, 1);
                mw.visitTypeInsn(CHECKCAST, Type.getInternalName(target));
            }

            var argIndex = 0;
            for (var genericArg : targetMethod.getGenericParameterTypes()) {
                var loadedArg = loadArg(mw, target, targetMethod, unsafe, genericArg, argIndex);
                if (loadedArg == null) return null;
                if (loadedArg) argIndex++;
            }

            mw.visitMethodInsn(
                Modifier.isStatic(targetMethod.getModifiers()) ? INVOKESTATIC : INVOKEVIRTUAL,
                Type.getInternalName(targetMethod.getDeclaringClass()), targetMethod.getName(),
                Type.getMethodDescriptor(targetMethod), false
            );

            // We allow a reasonable amount of flexibility on the return value's type. Alongside the obvious MethodResult,
            // we convert basic types into an immediate result.
            var ret = targetMethod.getReturnType();
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

    @Nullable
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

        System.err.printf("Unknown parameter type %s for method %s.%s.\n",
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
                e.printStackTrace();
                return def;
            }
        };
    }
}
