// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A collection of {@link Resource}s, with an upper bound on capacity.
 *
 * @param <T> The type of the resource this group manages.
 */
public class ResourceGroup<T extends Resource<T>> {
    public static final int DEFAULT_LIMIT = 512;

    final IntSupplier limit;

    boolean active = false;

    final Set<T> resources = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ResourceGroup(IntSupplier limit) {
        this.limit = limit;
    }

    public void startup() {
        active = true;
    }

    public synchronized void shutdown() {
        active = false;

        for (var resource : resources) resource.close();
        resources.clear();

        Resource.cleanup();
    }


    public final boolean queue(T resource, Runnable setup) {
        return queue(() -> {
            setup.run();
            return resource;
        });
    }

    public synchronized boolean queue(Supplier<T> resource) {
        Resource.cleanup();
        if (!active) return false;

        var limit = this.limit.getAsInt();
        if (limit <= 0 || resources.size() < limit) {
            resources.add(resource.get());
            return true;
        }

        return false;
    }

    public synchronized void release(T resource) {
        resources.remove(resource);
    }
}
