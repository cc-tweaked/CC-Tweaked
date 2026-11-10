// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import dan200.computercraft.api.lua.*;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

final class Generator<T> {
    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);

    private static final AtomicInteger METHOD_ID = new AtomicInteger();

    private static final String METHOD_NAME = "apply";
    private static final String[] EXCEPTIONS = new String[]{ Type.getInternalName(LuaException.class) };

    private static final String INTERNAL_METHOD_RESULT = Type.getInternalName(MethodResult.class);
    private static final String DESC_METHOD_RESULT = Type.getDescriptor(MethodResult.class);

    private static final String INTERNAL_ARGUMENTS = Type.getInternalName(IArguments.class);
    private static final String DESC_ARGUMENTS = Type.getDescriptor(IArguments.class);

    private static final String INTERNAL_COERCED = Type.getInternalName(Coerced.class);

    private final Class<T> base;
    private final List<Class<?>> context;

    private final String[] interfaces;
    private final String methodDesc;

    private final Map<Method, Optional<T>> methodCache = new ConcurrentHashMap<>();

    Generator(Class<T> base, List<Class<?>> context) {
        this.base = base;
        this.context = context;
        interfaces = new String[]{ Type.getInternalName(base) };

        StringBuilder methodDesc = new StringBuilder().append("(Ljava/lang/Object;");
        for (Class<?> klass : context) methodDesc.append(Type.getDescriptor(klass));
        methodDesc.append(DESC_ARGUMENTS).append(")").append(DESC_METHOD_RESULT);
        this.methodDesc = methodDesc.toString();
    }

    public Optional<T> getInstanceMethod(Method method) {
        return methodCache.computeIfAbsent(method, this::build);
    }

    /**
     * Check if a {@link LuaFunction}-annotated method can be used in this context.
     *
     * @param method The method to check.
     * @return Whether the method is valid.
     */
    private boolean checkMethod(Method method) {
        if (method.isBridge()) {
            LOG.debug("Skipping bridge Lua Method {}.{}", method.getDeclaringClass().getName(), method.getName());
            return false;
        }

        // Check we don't throw additional exceptions.
        var exceptions = method.getExceptionTypes();
        for (var exception : exceptions) {
            if (exception != LuaException.class) {
                LOG.error("Lua Method {}.{} cannot throw {}.", method.getDeclaringClass().getName(), method.getName(), exception.getName());
                return false;
            }
        }

        // Instance methods must be final - this prevents them being overridden and potentially exposed twice.
        var modifiers = method.getModifiers();
        if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers) && !Modifier.isFinal(method.getDeclaringClass().getModifiers())) {
            LOG.warn("Lua Method {}.{} should be final.", method.getDeclaringClass().getName(), method.getName());
        }

        return true;
    }

    private Optional<T> build(Method method) {
        if (!checkMethod(method)) return Optional.empty();

        LOG.debug("Generating method wrapper for {}.{}.", method.getDeclaringClass().getName(), method.getName());

        LuaFunction annotation = method.getAnnotation(LuaFunction.class);
        Class<?> target = method.getDeclaringClass();

        try {
            String className = method.getDeclaringClass().getName() + "$cc$" + method.getName() + METHOD_ID.getAndIncrement();
            byte[] bytes = generate(className, target, method, annotation.unsafe());
            if (bytes == null) return Optional.empty();

            Class<?> klass = DeclaringClassLoader.INSTANCE.define(className, bytes, method.getDeclaringClass().getProtectionDomain());

            T instance = klass.asSubclass(base).getDeclaredConstructor().newInstance();
            return Optional.of(instance);
        } catch (ReflectiveOperationException | ClassFormatError | RuntimeException e) {
            LOG.error("Error generating wrapper for {}.{}.", method.getDeclaringClass().getName(), method.getName());
            return Optional.empty();
        }

    }

    private byte[] generate(String className, Class<?> target, Method method, boolean unsafe) {
        String internalName = className.replace(".", "/");

        // Construct a public final class which extends Object and implements MethodInstance.Delegate
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_6, ACC_PUBLIC | ACC_FINAL, internalName, null, "java/lang/Object", interfaces);
        cw.visitSource("CC generated method", null);

        { // Constructor just invokes super.
            MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mw.visitCode();
            mw.visitVarInsn(ALOAD, 0);
            mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            mw.visitInsn(RETURN);
            mw.visitMaxs(0, 0);
            mw.visitEnd();
        }

        {
            MethodVisitor mw = cw.visitMethod(ACC_PUBLIC, METHOD_NAME, methodDesc, null, EXCEPTIONS);
            mw.visitCode();

            // If we're an instance method, load the this parameter.
            mw.visitVarInsn(ALOAD, 1);
            mw.visitTypeInsn(CHECKCAST, Type.getInternalName(target));

            int argIndex = 0;
            for (java.lang.reflect.Type genericArg : method.getGenericParameterTypes()) {
                Boolean loadedArg = loadArg(mw, target, method, unsafe, genericArg, argIndex);
                if (loadedArg == null) return null;
                if (loadedArg) argIndex++;
            }

            mw.visitMethodInsn(
                Modifier.isStatic(method.getModifiers()) ? INVOKESTATIC : INVOKEVIRTUAL,
                Type.getInternalName(method.getDeclaringClass()), method.getName(),
                Type.getMethodDescriptor(method)
            );

            // We allow a reasonable amount of flexibility on the return value's type. Alongside the obvious MethodResult,
            // we convert basic types into an immediate result.
            Class<?> ret = method.getReturnType();
            if (ret != MethodResult.class) {
                if (ret == void.class) {
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "()" + DESC_METHOD_RESULT);
                } else if (ret.isPrimitive()) {
                    var boxed = Primitives.wrap(ret);
                    mw.visitMethodInsn(INVOKESTATIC, Type.getInternalName(boxed), "valueOf", "(" + Type.getDescriptor(ret) + ")" + Type.getDescriptor(boxed));
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "(Ljava/lang/Object;)" + DESC_METHOD_RESULT);
                } else if (ret == Object[].class) {
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "([Ljava/lang/Object;)" + DESC_METHOD_RESULT);
                } else {
                    mw.visitMethodInsn(INVOKESTATIC, INTERNAL_METHOD_RESULT, "of", "(Ljava/lang/Object;)" + DESC_METHOD_RESULT);
                }
            }

            mw.visitInsn(ARETURN);

            mw.visitMaxs(0, 0);
            mw.visitEnd();
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

    private @Nullable Boolean loadArg(MethodVisitor mw, Class<?> target, Method method, boolean unsafe, java.lang.reflect.Type genericArg, int argIndex) {
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
                loadCoerced(mw, argIndex, "getStringCoerced", "(I)Ljava/lang/String;");
                return true;
            } else if (klass == ByteBuffer.class) {
                loadCoerced(mw, argIndex, "getBytesCoerced", "(I)Ljava/nio/ByteBuffer;");
                return true;
            }
        }

        if (arg == Optional.class) {
            var klass = Reflect.getRawType(method, TypeToken.of(genericArg).resolveType(Reflect.OPTIONAL_IN).getType(), false);
            if (klass == null) return null;

            if (Enum.class.isAssignableFrom(klass) && klass != Enum.class) {
                mw.visitVarInsn(ALOAD, 2 + context.size());
                loadInt(mw, argIndex);
                mw.visitLdcInsn(Type.getType(klass));
                mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "optEnum", "(ILjava/lang/Class;)Ljava/util/Optional;");
                return true;
            }

            var name = getLuaName(Primitives.unwrap(klass), unsafe);
            if (name != null) {
                mw.visitVarInsn(ALOAD, 2 + context.size());
                loadInt(mw, argIndex);
                mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "opt" + name, "(I)Ljava/util/Optional;");
                return true;
            }
        }

        if (Enum.class.isAssignableFrom(arg) && arg != Enum.class) {
            mw.visitVarInsn(ALOAD, 2 + context.size());
            loadInt(mw, argIndex);
            mw.visitLdcInsn(Type.getType(arg));
            mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "getEnum", "(ILjava/lang/Class;)Ljava/lang/Enum;");
            mw.visitTypeInsn(CHECKCAST, Type.getInternalName(arg));
            return true;
        }

        var name = arg == Object.class ? "" : getLuaName(arg, unsafe);
        if (name != null) {
            if (Reflect.getRawType(method, genericArg, false) == null) return null;

            mw.visitVarInsn(ALOAD, 2 + context.size());
            loadInt(mw, argIndex);
            mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, "get" + name, "(I)" + Type.getDescriptor(arg));
            return true;
        }

        LOG.error("Unknown parameter type {} for method {}.{}.", arg.getName(), method.getDeclaringClass().getName(), method.getName());
        return null;
    }

    private static @Nullable String getLuaName(Class<?> klass, boolean unsafe) {
        if (klass.isPrimitive()) {
            if (klass == int.class) return "Int";
            if (klass == boolean.class) return "Boolean";
            if (klass == double.class) return "Double";
            if (klass == long.class) return "Long";
        } else {
            if (klass == Map.class) return "Table";
            if (klass == String.class) return "String";
            if (klass == ByteBuffer.class) return "Bytes";
            if (klass == LuaTable.class && unsafe) return "TableUnsafe";
        }

        return null;
    }

    private static void loadInt(MethodVisitor visitor, int value) {
        if (value >= -1 && value <= 5) {
            visitor.visitInsn(ICONST_0 + value);
        } else {
            visitor.visitLdcInsn(value);
        }
    }

    private void loadCoerced(MethodVisitor mw, int argIndex, String getter, String getterType) {
        mw.visitTypeInsn(NEW, INTERNAL_COERCED);
        mw.visitInsn(DUP);
        mw.visitVarInsn(ALOAD, 2 + context.size());
        loadInt(mw, argIndex);
        mw.visitMethodInsn(INVOKEINTERFACE, INTERNAL_ARGUMENTS, getter, getterType);
        mw.visitMethodInsn(INVOKESPECIAL, INTERNAL_COERCED, "<init>", "(Ljava/lang/Object;)V");
    }

}
