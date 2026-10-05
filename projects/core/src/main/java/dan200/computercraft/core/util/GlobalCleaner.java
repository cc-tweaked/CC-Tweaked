// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import java.lang.ref.Cleaner;

/**
 * A global, shared {@link Cleaner} instance.
 */
public final class GlobalCleaner {
    private static final Cleaner instance = Cleaner.create();

    private GlobalCleaner() {
    }

    public static Cleaner get() {
        return instance;
    }

    /**
     * Register an object and a function to run when that object is no longer reachable.
     *
     * @param obj    The object to register.
     * @param action The action to run to after the object is garbage collected. This must NOT hold a reference to the
     *               original object.
     * @return A {@link Cleaner.Cleanable} instance.
     */
    public static Cleaner.Cleanable register(Object obj, Runnable action) {
        return get().register(obj, action);
    }
}
