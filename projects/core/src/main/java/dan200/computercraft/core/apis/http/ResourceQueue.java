// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import java.util.ArrayDeque;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A {@link ResourceGroup} which will queue items when the group at capacity.
 *
 * @param <T> The type of the resource this queue manages.
 */
public class ResourceQueue<T extends Resource<T>> extends ResourceGroup<T> {
    private final ArrayDeque<Supplier<T>> pending = new ArrayDeque<>();

    public ResourceQueue(IntSupplier limit) {
        super(limit);
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        pending.clear();
    }

    @Override
    public synchronized boolean queue(Supplier<T> resource) {
        if (!active) return false;
        if (super.queue(resource)) return true;
        if (pending.size() > DEFAULT_LIMIT) return false;

        pending.add(resource);
        return true;
    }

    @Override
    public synchronized void release(T resource) {
        super.release(resource);

        if (!active) return;

        var limit = this.limit.getAsInt();
        if (limit <= 0 || resources.size() < limit) {
            var next = pending.poll();
            if (next != null) resources.add(next.get());
        }
    }
}
