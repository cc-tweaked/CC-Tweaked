// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.Coerced;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.Optional;

public final class Reflect {
    private static final Logger LOG = LoggerFactory.getLogger(Reflect.class);
    public static final java.lang.reflect.Type OPTIONAL_IN = Optional.class.getTypeParameters()[0];
    public static final java.lang.reflect.Type COERCED_IN = Coerced.class.getTypeParameters()[0];

    private Reflect() {
    }

    @Nullable
    public static Class<?> getRawType(Member method, Type root, boolean allowParameter) {
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
}
