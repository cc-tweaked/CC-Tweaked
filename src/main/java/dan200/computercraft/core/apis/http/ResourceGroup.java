// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import dan200.computercraft.core.util.ThreadUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntSupplier;

/**
 * A collection of {@link Resource}s, with an upper bound on capacity.
 *
 * @param <T> The type of the resource this group manages.
 */
public final class ResourceGroup<T extends Resource<T, ?>> {
    public static final int DEFAULT_LIMIT = 512;

    private final IntSupplier limit;

    private final ReentrantLock lock = ThreadUtils.newReentrantLock("ResourceGroup.lock");

    @GuardedBy("lock")
    private boolean active = false;

    @GuardedBy("lock")
    private final Set<T> resources = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final int pendingLimit;

    @GuardedBy("lock")
    private final @Nullable ArrayDeque<T> pending;

    /**
     * Create a new {@link ResourceGroup} which will run at most {@code limit} tasks concurrently and reject any
     * additional tasks immediately.
     *
     * @param limit The maximum number of concurrent tasks.
     */
    public ResourceGroup(IntSupplier limit) {
        this(limit, 0);
    }

    /**
     * Create a new {@link ResourceGroup} which will run at most {@code limit} tasks concurrently and queue up to
     * {@code pendingLimit} additional tasks.
     *
     * @param limit        The maximum number of concurrent tasks.
     * @param pendingLimit The size of queued tasks.
     */
    public ResourceGroup(IntSupplier limit, int pendingLimit) {
        this.limit = limit;
        this.pendingLimit = pendingLimit;
        this.pending = pendingLimit >= 0 ? new ArrayDeque<>() : null;
    }

    /**
     * Start this {@link ResourceGroup}, allowing resources to be added to it.
     */
    public void startup() {
        lock.lock();
        try {
            active = true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Shutdown this {@link ResourceGroup}, disposing all associated resources.
     */
    public void shutdown() {
        lock.lock();
        try {
            active = false;

            for (var resource : resources) resource.close();
            resources.clear();

            if (pending != null) pending.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add a resource to this group, either starting it now, or when this group has capacity.
     *
     * @param resource The resource to queue.
     * @return Whether this resource could be queued.
     * @see Resource#queue()
     * @see Resource#start()
     */
    public boolean queue(T resource) {
        if (resource.limiter != this) throw new IllegalStateException("Resource is owned by a different group");

        lock.lock();
        try {
            if (!active) return false;
            var limit = this.limit.getAsInt();
            if (limit <= 0 || resources.size() < limit) {
                resource.start();
                resources.add(resource);
                return true;
            }

            if (pending != null && pending.size() <= pendingLimit) {
                pending.add(resource);
                return true;
            }

            return false;
        } finally {
            lock.unlock();
        }
    }

    void release(T resource) {
        lock.lock();
        try {
            resources.remove(resource);

            if (!active || pending == null) return;

            var limit = this.limit.getAsInt();
            if (limit <= 0 || resources.size() < limit) {
                var next = pending.poll();
                if (next != null) resources.add(next);
            }
        } finally {
            lock.unlock();
        }
    }
}
