// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import cc.tweaked.CCTweaked;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;

import static org.objectweb.asm.Opcodes.*;

public final class Generator<T> {
    private static final AtomicInteger METHOD_ID = new AtomicInteger();

    private static final String METHOD_NAME = "apply";
    private static final String[] EXCEPTIONS = new String[]{ Type.getInternalName(LuaException.class) };

    private static final String INTERNAL_METHOD_RESULT = Type.getInternalName(Object[].class);
    private static final String DESC_METHOD_RESULT = Type.getDescriptor(Object[].class);

    private static final String INTERNAL_ARGUMENTS = Type.getInternalName(IArguments.class);
    private static final String DESC_ARGUMENTS = Type.getDescriptor(IArguments.class);

    private static final String INTERNAL_COERCED = Type.getInternalName(Coerced.class);

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

        StringBuilder methodDesc = new StringBuilder().append("(Ljava/lang/Object;");
        for (Class<?> klass : context) methodDesc.append(Type.getDescriptor(klass));
        methodDesc.append(DESC_ARGUMENTS).append(")").append(DESC_METHOD_RESULT);
        this.methodDesc = methodDesc.toString();
    }

    public List<NamedMethod<T>> getMethods(Class<?> klass) {
        try {
            return classCache.get(klass);
        } catch (ExecutionException e) {
            CCTweaked.LOG.log(Level.SEVERE, "Error getting methods for " + klass.getName() + ".", e.getCause());
            return Collections.emptyList();
        }
    }

    private List<NamedMethod<T>> build(Class<?> klass) {
        ArrayList<NamedMethod<T>> methods = null;
        for (Method method : klass.getMethods()) {
            LuaFunction annotation = method.getAnnotation(LuaFunction.class);
            if (annotation == null) continue;

            if (Modifier.isStatic(method.getModifiers())) {
                CCTweaked.LOG.warning(String.format("LuaFunction method %s.%s should be an instance method.", method.getDeclaringClass(), method.getName()));
                continue;
            }

            T instance = methodCache.getUnchecked(method).orElse(null);
            if (instance == null) continue;

            if (methods == null) methods = new ArrayList<>();
            addMethod(methods, method, annotation, instance);
        }

        if (methods == null) return Collections.emptyList();
        methods.trimToSize();
        return Collections.unmodifiableList(methods);
    }

    private void addMethod(List<NamedMethod<T>> methods, Method method, LuaFunction annotation, T instance) {
        String[] names = annotation.value();
        boolean isSimple = true;
        if (names.length == 0) {
            methods.add(new NamedMethod<>(method.getName(), instance, isSimple));
        } else {
            for (String name : names) {
                methods.add(new NamedMethod<>(name, instance, isSimple));
            }
        }
    }

    private Optional<T> build(Method method) {
        String name = method.getDeclaringClass().getName() + "." + method.getName();
        int modifiers = method.getModifiers();

        // Instance methods must be final - this prevents them being overridden and potentially exposed twice.
        if (!Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
            CCTweaked.LOG.warning(String.format("Lua Method %s should be final.", name));
        }

        if (!Modifier.isPublic(modifiers)) {
            CCTweaked.LOG.severe(String.format("Lua Method %s should be a public method.", name));
            return Optional.empty();
        }

        if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            CCTweaked.LOG.warning(String.format("Lua Method %s should be on a public class.", name));
            return Optional.empty();
        }

        CCTweaked.LOG.fine(String.format("Generating method wrapper for %s.", name));

        Class<?>[] exceptions = method.getExceptionTypes();
        for (Class<?> exception : exceptions) {
            if (exception != LuaException.class) {
                CCTweaked.LOG.warning(String.format("Lua Method %s cannot throw %s.", name, exception.getName()));
                return Optional.empty();
            }
        }

        LuaFunction annotation = method.getAnnotation(LuaFunction.class);
        if (annotation.unsafe() && annotation.mainThread()) {
            CCTweaked.LOG.severe(String.format("Lua Method %s cannot use unsafe and mainThread", name));
            return Optional.empty();
        }

        // We have some rather ugly handling of static methods in both here and the main generate function. Static methods
        // only come from generic sources, so this should be safe.
        Class<?> target = Modifier.isStatic(modifiers) ? method.getParameterTypes()[0] : method.getDeclaringClass();

        try {
            String className = method.getDeclaringClass().getName() + "$cc$" + method.getName() + METHOD_ID.getAndIncrement();
            byte[] bytes = generate(className, target, method, annotation.unsafe());
            if (bytes == null) return Optional.empty();

            Class<?> klass = DeclaringClassLoader.INSTANCE.define(className, bytes, method.getDeclaringClass().getProtectionDomain());

            T instance = klass.asSubclass(base).getDeclaredConstructor().newInstance();
            return Optional.of(annotation.mainThread() ? wrap.apply(instance) : instance);
        } catch (ReflectiveOperationException | ClassFormatError | RuntimeException e) {
            CCTweaked.LOG.log(Level.SEVERE, String.format("Error generating wrapper for %s.", name), e);
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
            if (!Modifier.isStatic(method.getModifiers())) {
                mw.visitVarInsn(ALOAD, 1);
                mw.visitTypeInsn(CHECKCAST, Type.getInternalName(target));
            }

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
            if (ret != Object[].class) {
                if (ret == void.class) {
                    mw.visitMethodInsn(INVOKESTATIC, "dan200/computercraft/core/asm/Support", "of", "()" + DESC_METHOD_RESULT);
                } else if (ret.isPrimitive()) {
                    Class<?> boxed = Primitives.wrap(ret);
                    mw.visitMethodInsn(INVOKESTATIC, Type.getInternalName(boxed), "valueOf", "(" + Type.getDescriptor(ret) + ")" + Type.getDescriptor(boxed));
                    mw.visitMethodInsn(INVOKESTATIC, "dan200/computercraft/core/asm/Support", "of", "(Ljava/lang/Object;)" + DESC_METHOD_RESULT);
                } else {
                    mw.visitMethodInsn(INVOKESTATIC, "dan200/computercraft/core/asm/Support", "of", "(Ljava/lang/Object;)" + DESC_METHOD_RESULT);
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

        Class<?> arg = Reflect.getRawType(method, genericArg, true);
        if (arg == null) return null;

        if (arg == IArguments.class) {
            mw.visitVarInsn(ALOAD, 2 + context.size());
            return false;
        }

        int idx = context.indexOf(arg);
        if (idx >= 0) {
            mw.visitVarInsn(ALOAD, 2 + idx);
            return false;
        }

        if (arg == Coerced.class) {
            Class<?> klass = Reflect.getRawType(method, TypeToken.of(genericArg).resolveType(Reflect.COERCED_IN).getType(), false);
            if (klass == null) return null;

            if (klass == String.class) {
                mw.visitTypeInsn(NEW, INTERNAL_COERCED);
                mw.visitInsn(DUP);
                mw.visitVarInsn(ALOAD, 2 + context.size());
                Reflect.loadInt(mw, argIndex);
                mw.visitMethodInsn(INVOKEVIRTUAL, INTERNAL_ARGUMENTS, "getStringCoerced", "(I)Ljava/lang/String;");
                mw.visitMethodInsn(INVOKESPECIAL, INTERNAL_COERCED, "<init>", "(Ljava/lang/Object;)V");
                return true;
            }
        }

        if (arg == Optional.class) {
            Class<?> klass = Reflect.getRawType(method, TypeToken.of(genericArg).resolveType(Reflect.OPTIONAL_IN).getType(), false);
            if (klass == null) return null;

            if (Enum.class.isAssignableFrom(klass) && klass != Enum.class) {
                mw.visitVarInsn(ALOAD, 2 + context.size());
                Reflect.loadInt(mw, argIndex);
                mw.visitLdcInsn(Type.getType(klass));
                mw.visitMethodInsn(INVOKEVIRTUAL, INTERNAL_ARGUMENTS, "optEnum", "(ILjava/lang/Class;)Ljava/util/Optional;");
                return true;
            }

            String name = Reflect.getLuaName(Primitives.unwrap(klass), unsafe);
            if (name != null) {
                mw.visitVarInsn(ALOAD, 2 + context.size());
                Reflect.loadInt(mw, argIndex);
                mw.visitMethodInsn(INVOKEVIRTUAL, INTERNAL_ARGUMENTS, "opt" + name, "(I)Ljava/util/Optional;");
                return true;
            }
        }

        if (Enum.class.isAssignableFrom(arg) && arg != Enum.class) {
            mw.visitVarInsn(ALOAD, 2 + context.size());
            Reflect.loadInt(mw, argIndex);
            mw.visitLdcInsn(Type.getType(arg));
            mw.visitMethodInsn(INVOKEVIRTUAL, INTERNAL_ARGUMENTS, "getEnum", "(ILjava/lang/Class;)Ljava/lang/Enum;");
            mw.visitTypeInsn(CHECKCAST, Type.getInternalName(arg));
            return true;
        }

        String name = arg == Object.class ? "" : Reflect.getLuaName(arg, unsafe);
        if (name != null) {
            if (Reflect.getRawType(method, genericArg, false) == null) return null;

            mw.visitVarInsn(ALOAD, 2 + context.size());
            Reflect.loadInt(mw, argIndex);
            mw.visitMethodInsn(INVOKEVIRTUAL, INTERNAL_ARGUMENTS, "get" + name, "(I)" + Type.getDescriptor(arg));
            return true;
        }

        CCTweaked.LOG.severe(String.format("Unknown parameter type %s for method %s.%s.", arg.getName(), method.getDeclaringClass().getName(), method.getName()));
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
                CCTweaked.LOG.log(Level.SEVERE, "Error generating @LuaFunctions", e);
                return def;
            }
        };
    }
}
