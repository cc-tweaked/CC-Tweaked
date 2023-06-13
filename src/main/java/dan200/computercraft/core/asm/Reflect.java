// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.Coerced;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

import static org.objectweb.asm.Opcodes.ICONST_0;

final class Reflect {
    static final java.lang.reflect.Type OPTIONAL_IN = Optional.class.getTypeParameters()[0];
    static final java.lang.reflect.Type COERCED_IN = Coerced.class.getTypeParameters()[0];

    private Reflect() {
    }

    static String getLuaName(Class<?> klass, boolean unsafe) {
        if (klass.isPrimitive()) {
            if (klass == int.class) return "Int";
            if (klass == boolean.class) return "Boolean";
            if (klass == double.class) return "Double";
            if (klass == long.class) return "Long";
        } else {
            if (klass == Map.class) return "Table";
            if (klass == String.class) return "String";
            if (klass == ByteBuffer.class) return "Bytes";
        }

        return null;
    }

    static Class<?> getRawType(Method method, Type root, boolean allowParameter) {
        Type underlying = root;
        while (true) {
            if (underlying instanceof Class<?>) return (Class<?>) underlying;

            if (underlying instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) underlying;
                if (!allowParameter) {
                    for (Type arg : type.getActualTypeArguments()) {
                        if (arg instanceof WildcardType) continue;
                        if (arg instanceof TypeVariable<?> && ((TypeVariable<?>) arg).getName().startsWith("capture#")) {
                            continue;
                        }

                        // LOG.error("Method {}.{} has generic type {} with non-wildcard argument {}.", method.getDeclaringClass(), method.getName(), root, arg);
                        return null;
                    }
                }

                // Continue to extract from this child
                underlying = type.getRawType();
                continue;
            }

            // LOG.error("Method {}.{} has unknown generic type {}.", method.getDeclaringClass(), method.getName(), root);
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
