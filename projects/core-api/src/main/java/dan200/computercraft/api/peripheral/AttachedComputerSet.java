// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * A thread-safe collection of computers.
 * <p>
 * This collection is intended to be used by peripherals that need to maintain a set of all attached computers.
 * <p>
 * It is recommended to use over Java's built-in concurrent collections (e.g. {@link CopyOnWriteArraySet} or
 * {@link ConcurrentHashMap}), as {@link AttachedComputerSet} ensures that computers cannot be accessed after they are
 * detached, guaranteeing that {@link NotAttachedException}s will not be thrown.
 * <p>
 * To ensure this, {@link AttachedComputerSet} is not directly iterable, as we cannot ensure that computers are not
 * detached while the iterator is running (and so trying to use the computer would error). Instead, computers should be
 * looped over using {@link #forEach(Consumer)}.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * public class MyPeripheral implements IPeripheral {
 *     private final AttachedComputerSet computers = new ComputerCollection();
 *
 *     @Override
 *     public void attach(IComputerAccess computer) {
 *         computers.add(computer);
 *     }
 *
 *     @Override
 *     public void detach(IComputerAccess computer) {
 *         computers.remove(computer);
 *     }
 * }
 * }</pre>
 *
 * @see IComputerAccess
 * @see IPeripheral#attach(IComputerAccess)
 * @see IPeripheral#detach(IComputerAccess)
 */
public final class AttachedComputerSet {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<IComputerAccess> computers = new HashSet<>(0);

    /**
     * Add a computer to this collection of computers. This should be called from
     * {@link IPeripheral#attach(IComputerAccess)}.
     *
     * @param computer The computer to add.
     */
    public void add(IComputerAccess computer) {
        lock.writeLock().lock();
        try {
            computers.add(computer);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Remove a computer from this collection of computers. This should be called from
     * {@link IPeripheral#detach(IComputerAccess)}.
     *
     * @param computer The computer to remove.
     */
    public void remove(IComputerAccess computer) {
        lock.writeLock().lock();
        try {
            computers.remove(computer);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Apply an action to each computer in this collection.
     *
     * @param action The action to apply.
     */
    public void forEach(Consumer<? super IComputerAccess> action) {
        lock.readLock().lock();
        try {
            computers.forEach(action);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@linkplain IComputerAccess#queueEvent(String, Object...) Queue an event} on all computers.
     *
     * @param event     The name of the event to queue.
     * @param arguments The arguments for this event.
     * @see IComputerAccess#queueEvent(String, Object...)
     */
    public void queueEvent(String event, @Nullable Object... arguments) {
        forEach(c -> c.queueEvent(event, arguments));
    }

    /**
     * Determine if this collection contains any computers.
     * <p>
     * This method is primarily intended for presentation purposes (such as rendering an icon in the UI if a computer
     * is attached to your peripheral). Due to the multi-threaded nature of peripherals, it is not recommended to guard
     * any logic behind this check.
     * <p>
     * For instance, {@code if(computers.hasComputers()) computers.queueEvent("foo");} contains a race condition, as
     * there's no guarantee that any computers are still attached within the body of the if statement.
     *
     * @return Whether this collection is non-empty.
     */
    public boolean hasComputers() {
        lock.readLock().lock();
        try {
            return !computers.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }
}
