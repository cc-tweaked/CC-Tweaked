// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.platform;

import dan200.computercraft.shared.peripheral.generic.ComponentLookup;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;

/**
 * A function which may be called when a capability (or some other object) has been invalidated.
 * <p>
 * This extends {@link NonNullConsumer} for use with {@link LazyOptional#addListener(NonNullConsumer)}, and
 * {@link Runnable} for use with {@link ComponentLookup}.
 */
public interface InvalidateCallback extends Runnable, NonNullConsumer<Object> {
    @Override
    default void accept(Object o) {
        run();
    }

    /**
     * Cast this callback to a {@link NonNullConsumer} of an arbitrary type.
     *
     * @param <T> The type of the consumer, normally a {@link LazyOptional}.
     * @return {@code this}, but with a compatible type.
     */
    @SuppressWarnings("unchecked")
    default <T> NonNullConsumer<T> castConsumer() {
        return (NonNullConsumer<T>) this;
    }
}
