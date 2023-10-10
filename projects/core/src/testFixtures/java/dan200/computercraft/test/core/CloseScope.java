// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * An {@link AutoCloseable} implementation which can be used to combine other {@link AutoCloseable} instances.
 * <p>
 * Values which implement {@link AutoCloseable} can be dynamically registered with {@link CloseScope#add(AutoCloseable)}.
 * When the scope is closed, each value is closed in the opposite order.
 * <p>
 * This is largely intended for cases where it's not appropriate to nest try-with-resources blocks, for instance when
 * nested would be too deep or when objects are dynamically created.
 */
public class CloseScope implements AutoCloseable {
    private final Deque<AutoCloseable> toClose = new ArrayDeque<>();

    /**
     * Add a value to be closed when this scope exists.
     *
     * @param value The value to be closed.
     * @param <T>   The type of the provided value.
     * @return The provided value.
     */
    public <T extends AutoCloseable> T add(T value) {
        Objects.requireNonNull(value, "Value cannot be null");
        toClose.add(value);
        return value;
    }

    @Override
    public void close() throws Exception {
        close(null);
    }

    public void close(@Nullable Exception baseException) throws Exception {
        while (true) {
            var close = toClose.pollLast();
            if (close == null) break;

            try {
                close.close();
            } catch (Exception e) {
                if (baseException == null) {
                    baseException = e;
                } else {
                    baseException.addSuppressed(e);
                }
            }
        }

        if (baseException != null) throw baseException;
    }
}
