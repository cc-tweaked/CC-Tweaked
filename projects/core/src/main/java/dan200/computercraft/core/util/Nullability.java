// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import javax.annotation.Nullable;

public final class Nullability {
    private Nullability() {
    }

    /**
     * An alternative to {@link java.util.Objects#requireNonNull(Object)}, which should be interpreted as an assertion
     * ("this case should never happen") rather than an argument check.
     *
     * @param object The object to check, possibly {@literal null}.
     * @param <T>    The type of the object to check
     * @return The checked value.
     */
    public static <T> T assertNonNull(@Nullable T object) {
        if (object == null) throw new NullPointerException("Impossible: Should never be null");
        return object;
    }
}
