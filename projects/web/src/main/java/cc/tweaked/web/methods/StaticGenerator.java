// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.methods;

import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.asm.Reflect;
import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.util.Nullability;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.extension.introspect.IntrospectClass;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessFlag;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;

import static java.lang.constant.ConstantDescs.*;

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
    private static final Logger LOG = LoggerFactory.getLogger(StaticGenerator.class);

    private static final String METHOD_NAME = "apply";

    private static final ClassDesc CD_METHOD_RESULT = MethodResult.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_ARGUMENTS = IArguments.class.describeConstable().orElseThrow();
    private static final ClassDesc CD_COERCED = Coerced.class.describeConstable().orElseThrow();

    private final Class<T> base;
    private final List<Class<?>> context;

    private final MethodTypeDesc methodDesc;
    private final String classPrefix;

    private final Function<byte[], IntrospectClass<?>> createClass;

    private final Map<Method, Optional<IntrospectClass<T>>> methodCache = new HashMap<>();

    public StaticGenerator(Class<T> base, List<Class<?>> context, Function<byte[], IntrospectClass<?>> createClass) {
        this.base = base;
        this.context = context;
        this.createClass = createClass;

        List<ClassDesc> arguments = new ArrayList<>(context.size() + 2);
        arguments.add(CD_Object);
        for (var klass : context) arguments.add(klass.describeConstable().orElseThrow());
        arguments.add(CD_ARGUMENTS);
        methodDesc = MethodTypeDesc.of(CD_METHOD_RESULT, arguments);

        classPrefix = StaticGenerator.class.getPackageName() + "." + base.getSimpleName() + "$";
    }

    public Optional<IntrospectClass<T>> getMethod(Method method) {
        return methodCache.computeIfAbsent(method, this::build);
    }

    private Optional<IntrospectClass<T>> build(Method method) {
        var name = method.getDeclaringClass().getName() + "." + method.getName();
        var modifiers = method.getModifiers();

        // Methods must be final - this prevents them being overridden and potentially exposed twice.
        if (!Modifier.isFinal(modifiers) && !Modifier.isFinal(method.getDeclaringClass().getModifiers())) {
            LOG.warn("Lua Method {} should be final.", name);
        }

        if (!Modifier.isPublic(modifiers) || !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            LOG.warn("Lua Method {} should be a public method.", name);
            return Optional.empty();
        }

        var exceptions = method.getExceptionTypes();
        for (var exception : exceptions) {
            if (exception != LuaException.class) {
                LOG.warn("Lua Method {} cannot throw {}.", name, exception.getName());
                return Optional.empty();
            }
        }

        var annotation = Nullability.assertNonNull(method.getAnnotation(LuaFunction.class));
        if (annotation.unsafe() && annotation.mainThread()) {
            LOG.warn("Lua Method {} cannot use unsafe and mainThread.", name);
            return Optional.empty();
        }

        try {
            var target = method.getDeclaringClass();
            var bytes = generate(classPrefix + method.getDeclaringClass().getSimpleName() + "$" + method.getName(), target, method, annotation.unsafe());
            if (bytes == null) return Optional.empty();

            return Optional.of(createClass.apply(bytes).asSubclass(base));
        } catch (ClassFormatError | RuntimeException e) {
            LOG.error("Error generating {}", name, e);
            return Optional.empty();
        }
    }

    private byte @Nullable [] generate(String className, Class<?> target, Method targetMethod, boolean unsafe) {
        var internalName = ClassDesc.of(className);

        // Construct a public final class which extends Object and implements MethodInstance.Delegate
        return ClassFile.of().build(internalName, klass -> {
            klass.withFlags(AccessFlag.PUBLIC, AccessFlag.FINAL);
            klass.withInterfaceSymbols(base.describeConstable().orElseThrow());

            klass.withField("INSTANCE", internalName, f -> f.withFlags(AccessFlag.PUBLIC, AccessFlag.STATIC, AccessFlag.FINAL));

            // Constructor just invokes super.
            klass.withMethod(INIT_NAME, ConstantDescs.MTD_void, AccessFlag.PUBLIC.mask(), method -> method.withCode(code -> {
                code.aload(0);
                code.invokespecial(CD_Object, INIT_NAME, ConstantDescs.MTD_void);
                code.return_();
            }));

            // Static initialiser sets the INSTANCE field.
            klass.withMethod(ConstantDescs.CLASS_INIT_NAME, ConstantDescs.MTD_void, AccessFlag.STATIC.mask(), method -> method.withCode(code -> {
                code.new_(internalName);
                code.dup();
                code.invokespecial(internalName, INIT_NAME, ConstantDescs.MTD_void);
                code.putstatic(internalName, "INSTANCE", internalName);
                code.return_();
            }));

            klass.withMethod(METHOD_NAME, methodDesc, AccessFlag.PUBLIC.mask(), method -> method.withCode(code -> {
                // Load the target as the first argument.
                code.aload(1);
                code.checkcast(ClassDesc.of(target.getName()));

                var argIndex = 0;
                for (var genericArg : targetMethod.getGenericParameterTypes()) {
                    var loadedArg = loadArg(code, target, targetMethod, unsafe, genericArg, argIndex);
                    if (loadedArg) argIndex++;
                }

                code.invokevirtual(
                    targetMethod.getDeclaringClass().describeConstable().orElseThrow(),
                    targetMethod.getName(),
                    MethodType.methodType(targetMethod.getReturnType(), targetMethod.getParameterTypes()).describeConstable().orElseThrow()
                );

                // We allow a reasonable amount of flexibility on the return value's type. Alongside the obvious MethodResult,
                // we convert basic types into an immediate result.
                var ret = targetMethod.getReturnType();
                if (ret != MethodResult.class) {
                    if (ret == void.class) {
                        code.invokestatic(CD_METHOD_RESULT, "of", MethodTypeDesc.of(CD_METHOD_RESULT), false);
                    } else if (ret.isPrimitive()) {
                        var boxed = Primitives.wrap(ret);
                        code.invokestatic(
                            boxed.describeConstable().orElseThrow(), "valueOf",
                            MethodType.methodType(boxed, ret).describeConstable().orElseThrow()
                        );
                        code.invokestatic(CD_METHOD_RESULT, "of", MethodTypeDesc.of(CD_METHOD_RESULT, CD_Object), false);
                    } else if (ret == Object[].class) {
                        code.invokestatic(CD_METHOD_RESULT, "of", MethodTypeDesc.of(CD_METHOD_RESULT, CD_Object.arrayType()), false);
                    } else {
                        code.invokestatic(CD_METHOD_RESULT, "of", MethodTypeDesc.of(CD_METHOD_RESULT, CD_Object), false);
                    }
                }

                code.return_(TypeKind.REFERENCE);
            }));
        });
    }

    private boolean loadArg(CodeBuilder code, Class<?> target, Method method, boolean unsafe, java.lang.reflect.Type genericArg, int argIndex) {
        if (genericArg == target) {
            code.aload(1);
            code.checkcast(target.describeConstable().orElseThrow());
            return false;
        }

        var arg = Reflect.getRawType(method, genericArg, true);
        if (arg == null) throw new IllegalStateException("Invalid argument");

        var argDesc = arg.describeConstable().orElseThrow();

        if (arg == IArguments.class) {
            code.aload(2 + context.size());
            return false;
        }

        var idx = context.indexOf(arg);
        if (idx >= 0) {
            code.aload(2 + idx);
            return false;
        }

        if (arg == Coerced.class) {
            var klass = Reflect.getRawType(method, TypeToken.of(genericArg).resolveType(Reflect.COERCED_IN).getType(), false);
            if (klass == null) throw new IllegalStateException("Invalid argument");

            if (klass == String.class) {
                loadCoerced(code, argIndex, "getStringCoerced", MethodTypeDesc.of(CD_String, CD_int));
                return true;
            } else if (klass == ByteBuffer.class) {
                loadCoerced(code, argIndex, "getBytesCoerced", MethodTypeDesc.of(ByteBuffer.class.describeConstable().orElseThrow(), CD_int));
                return true;
            }
        }

        if (arg == Optional.class) {
            var klass = Reflect.getRawType(method, TypeToken.of(genericArg).resolveType(Reflect.OPTIONAL_IN).getType(), false);
            if (klass == null) throw new IllegalStateException("Invalid argument");

            if (Enum.class.isAssignableFrom(klass) && klass != Enum.class) {
                code.aload(2 + context.size());
                code.loadConstant(argIndex);
                code.loadConstant(klass.describeConstable().orElseThrow());
                code.invokeinterface(CD_ARGUMENTS, "optEnum", MethodType.methodType(Optional.class, int.class, Class.class).describeConstable().orElseThrow());
                return true;
            }

            var name = getLuaName(Primitives.unwrap(klass), unsafe);
            if (name != null) {
                code.aload(2 + context.size());
                code.loadConstant(argIndex);
                code.invokeinterface(CD_ARGUMENTS, "opt" + name, MethodType.methodType(Optional.class, int.class).describeConstable().orElseThrow());
                return true;
            }
        }

        if (Enum.class.isAssignableFrom(arg) && arg != Enum.class) {
            code.aload(2 + context.size());
            code.loadConstant(argIndex);
            code.loadConstant(argDesc);
            code.invokeinterface(CD_ARGUMENTS, "getEnum", MethodType.methodType(Enum.class, int.class, Class.class).describeConstable().orElseThrow());
            code.checkcast(argDesc);
            return true;
        }

        var name = arg == Object.class ? "" : getLuaName(arg, unsafe);
        if (name != null) {
            if (Reflect.getRawType(method, genericArg, false) == null) {
                throw new IllegalStateException("Invalid argument");
            }

            code.aload(2 + context.size());
            code.loadConstant(argIndex);
            code.invokeinterface(CD_ARGUMENTS, "get" + name, MethodType.methodType(arg, int.class).describeConstable().orElseThrow());
            return true;
        }

        LOG.warn("Unknown parameter type {} for method {}.{}.", arg.getName(), method.getDeclaringClass().getName(), method.getName());
        throw new IllegalStateException("Invalid argument");
    }

    @Nullable
    private static String getLuaName(Class<?> klass, boolean unsafe) {
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

    private void loadCoerced(CodeBuilder code, int argIndex, String getter, MethodTypeDesc getterType) {
        code.new_(CD_COERCED);
        code.dup();
        code.aload(2 + context.size());
        code.loadConstant(argIndex);
        code.invokeinterface(CD_ARGUMENTS, getter, getterType);
        code.invokespecial(CD_COERCED, INIT_NAME, MethodTypeDesc.of(CD_void, CD_Object));
    }
}
