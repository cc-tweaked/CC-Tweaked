// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import java.util.Collections;
import java.util.function.BiConsumer;

public final class Methods {
    private Methods() {
    }

    public static final Generator<LuaMethod> LUA_METHOD = new Generator<>(LuaMethod.class, Collections.emptyList(), m -> {
        throw new IllegalStateException("Impossible");
    });

    public static <T> void forEachMethod(Generator<T> generator, Object object, BiConsumer<Object, NamedMethod<T>> accept) {
        for (NamedMethod<T> method : generator.getMethods(object.getClass())) accept.accept(object, method);

        if (object instanceof ObjectSource) {
            for (Object extra : ((ObjectSource) object).getExtra()) {
                for (NamedMethod<T> method : generator.getMethods(extra.getClass())) accept.accept(extra, method);
            }
        }
    }
}
