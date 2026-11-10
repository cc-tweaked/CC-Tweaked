// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * A global, shared {@link Cleaner} instance.
 */
public final class GlobalCleaner {
    private static final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    private static class CleanInstance extends PhantomReference<Object> {
        final Runnable action;

        public CleanInstance(Object referent, ReferenceQueue<? super Object> queue, Runnable action) {
            super(referent, queue);
            this.action = action;
        }
    }

    static {
        ThreadUtils.factory("Cleaner").newThread(() -> {
            try {
                while (true) {
                    ((CleanInstance) queue.remove()).action.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private GlobalCleaner() {
    }

    /**
     * Register an object and a function to run when that object is no longer reachable.
     *
     * @param obj    The object to register.
     * @param action The action to run to after the object is garbage collected. This must NOT hold a reference to the
     *               original object.
     */
    public static void register(Object obj, Runnable action) {
        new CleanInstance(obj, queue, action);
    }
}
