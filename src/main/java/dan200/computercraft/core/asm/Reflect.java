/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import static org.objectweb.asm.Opcodes.ICONST_0;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import dan200.computercraft.ComputerCraft;
import org.objectweb.asm.MethodVisitor;

final class Reflect {
    static final java.lang.reflect.Type OPTIONAL_IN = Optional.class.getTypeParameters()[0];

    private Reflect() {
    }

    @Nullable
    static String getLuaName(Class<?> klass) {
        if (klass.isPrimitive()) {
            if (klass == int.class) {
                return "Int";
            }
            if (klass == boolean.class) {
                return "Boolean";
            }
            if (klass == double.class) {
                return "Double";
            }
            if (klass == long.class) {
                return "Long";
            }
        } else {
            if (klass == Map.class) {
                return "Table";
            }
            if (klass == String.class) {
                return "String";
            }
            if (klass == ByteBuffer.class) {
                return "Bytes";
            }
        }

        return null;
    }

    @Nullable
    static Class<?> getRawType(Method method, Type root, boolean allowParameter) {
        Type underlying = root;
        while (true) {
            if (underlying instanceof Class<?>) {
                return (Class<?>) underlying;
            }

            if (underlying instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) underlying;
                if (!allowParameter) {
                    for (java.lang.reflect.Type arg : type.getActualTypeArguments()) {
                        if (arg instanceof WildcardType) {
                            continue;
                        }
                        if (arg instanceof TypeVariable && ((TypeVariable<?>) arg).getName()
                                                                                  .startsWith("capture#")) {
                            continue;
                        }

                        ComputerCraft.log.error("Method {}.{} has generic type {} with non-wildcard argument {}.",
                                                method.getDeclaringClass(),
                                                method.getName(),
                                                root,
                                                arg);
                        return null;
                    }
                }

                // Continue to extract from this child
                underlying = type.getRawType();
                continue;
            }

            ComputerCraft.log.error("Method {}.{} has unknown generic type {}.", method.getDeclaringClass(), method.getName(), root);
            return null;
        }
    }

    static void loadInt(MethodVisitor visitor, int value) {
        if (value >= -1 && value <= 5) {
            visitor.visitInsn(ICONST_0 + value);
        } else {
            visitor.visitLdcInsn(value);
        }
    }
}
