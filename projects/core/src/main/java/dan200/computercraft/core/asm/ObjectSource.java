// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import java.util.function.BiConsumer;

/**
 * A Lua object which exposes additional methods.
 * <p>
 * This can be used to merge multiple objects together into one. Ideally this'd be part of the API, but I'm not entirely
 * happy with the interface - something I'd like to think about first.
 */
public interface ObjectSource {
    Iterable<Object> getExtra();

    static <T> void allMethods(Generator<T> generator, Object object, BiConsumer<Object, NamedMethod<T>> accept) {
        for (var method : generator.getMethods(object.getClass())) accept.accept(object, method);

        if (object instanceof ObjectSource source) {
            for (var extra : source.getExtra()) {
                for (var method : generator.getMethods(extra.getClass())) accept.accept(extra, method);
            }
        }
    }
}
