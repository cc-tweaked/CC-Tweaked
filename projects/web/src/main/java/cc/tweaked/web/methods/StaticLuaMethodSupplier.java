// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.methods;

import dan200.computercraft.core.methods.LuaMethod;
import dan200.computercraft.core.methods.MethodSupplier;
import dan200.computercraft.core.methods.ObjectSource;

/**
 * A {@link MethodSupplier} for {@link LuaMethod}s with a version which uses {@link MethodReflection} to statically
 * generate classes.
 */
public final class StaticLuaMethodSupplier implements MethodSupplier<LuaMethod> {
    public static final StaticLuaMethodSupplier INSTANCE = new StaticLuaMethodSupplier();

    private StaticLuaMethodSupplier() {
    }

    @Override
    public boolean forEachSelfMethod(Object object, UntargetedConsumer<LuaMethod> consumer) {
        return MethodReflection.getMethods(object.getClass(), method -> consumer.accept(method.name(), method.method(), method));
    }

    @Override
    public boolean forEachMethod(Object object, TargetedConsumer<LuaMethod> consumer) {
        var hasMethods = MethodReflection.getMethods(object.getClass(), method -> consumer.accept(object, method.name(), method.method(), method));

        if (object instanceof ObjectSource source) {
            for (var extra : source.getExtra()) {
                hasMethods |= MethodReflection.getMethods(extra.getClass(), method -> consumer.accept(extra, method.name(), method.method(), method));
            }
        }

        return hasMethods;
    }
}
