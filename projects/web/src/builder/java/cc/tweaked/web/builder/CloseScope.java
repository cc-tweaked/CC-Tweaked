// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.builder;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A {@link Closeable} implementation which can be used to combine other {@link Closeable} instances.
 * <p>
 * This is mostly identical to the version in {@link dan200.computercraft.test.core.CloseScope}. Really they should be
 * merged, but not sure where/how to do that!
 */
public final class CloseScope implements Closeable {
    private final Deque<Closeable> toClose = new ArrayDeque<>();

    public <T extends Closeable> T add(T value) {
        toClose.addLast(value);
        return value;
    }

    @Override
    public void close() throws IOException {
        Throwable error = null;

        AutoCloseable next;
        while ((next = toClose.pollLast()) != null) {
            try {
                next.close();
            } catch (Throwable e) {
                if (error == null) {
                    error = e;
                } else {
                    error.addSuppressed(e);
                }
            }
        }

        if (error != null) CloseScope.<IOException>throwUnchecked0(error);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked0(Throwable t) throws T {
        throw (T) t;
    }
}
