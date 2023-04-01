// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.LuaTable;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

import static org.objectweb.asm.Opcodes.ICONST_0;

final class Reflect {
    private static final Logger LOG = LoggerFactory.getLogger(Reflect.class);
    static final java.lang.reflect.Type OPTIONAL_IN = Optional.class.getTypeParameters()[0];

    private Reflect() {
    }

    @Nullable
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
            if (klass == LuaTable.class && unsafe) return "TableUnsafe";
        }

        return null;
    }

    @Nullable
    static Class<?> getRawType(Method method, Type root, boolean allowParameter) {
        var underlying = root;
        while (true) {
            if (underlying instanceof Class<?> klass) return klass;

            if (underlying instanceof ParameterizedType type) {
                if (!allowParameter) {
                    for (var arg : type.getActualTypeArguments()) {
                        if (arg instanceof WildcardType) continue;
                        if (arg instanceof TypeVariable<?> var && var.getName().startsWith("capture#")) {
                            continue;
                        }

                        LOG.error("Method {}.{} has generic type {} with non-wildcard argument {}.", method.getDeclaringClass(), method.getName(), root, arg);
                        return null;
                    }
                }

                // Continue to extract from this child
                underlying = type.getRawType();
                continue;
            }

            LOG.error("Method {}.{} has unknown generic type {}.", method.getDeclaringClass(), method.getName(), root);
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
