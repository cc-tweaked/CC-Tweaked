// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.util;

import com.google.errorprone.annotations.MustBeClosed;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Locked access to an arbitrary reference.
 * <p>
 * This combines a {@link ReentrantLock} and some arbitrary value into a single class, similar to Rust's
 * <a href="https://doc.rust-lang.org/nightly/std/sync/struct.Mutex.html"><code>Mutex</code></a>. This provides
 * a slightly better UX than manual {@code @GuardedBy} annotations and {@code lock.lock(); try { ... } finally { lock.unlock(); }}
 * calls.
 * <p>
 * Accesses to this value should be by a {@code try}-with-resources block on the result of {@link #lock()}. Nested
 * access is <strong>NOT</strong> supported. Instead, the {@link ReadHandle} or {@link ReadWriteHandle} should be passed
 * to calls that need it.
 * <p>
 * {@snippet :
 *   LockedReference.Mutable<String> reference = LockedReference.mutable("My Reference", "Hello");
 *   try (var handle = reference.lock()) {
 *     System.out.println(handle.get());
 *      handle.set("Hello, world!");
 *   }
 * }
 *
 * @param <H> The type of handle returned by {@link #lock()}.
 */
public abstract sealed class LockedReference<H extends LockedReference.ReadHandle<?>> {
    /**
     * A read-only handle to this reference.
     *
     * @param <T> The type of value owned by this reference.
     */
    public sealed interface ReadHandle<T extends @Nullable Object> extends AutoCloseable {
        /**
         * Get the value currently held by this reference. Usages of this value should be done within the scope of
         * {@link ReadHandle}.
         *
         * @return The value currently held by this reference.
         */
        T get();

        @Override
        void close();
    }

    /**
     * A read-write handle to this reference.
     *
     * @param <T> The type of value owned by this reference.
     */
    public sealed interface ReadWriteHandle<T extends @Nullable Object> extends ReadHandle<T> {
        /**
         * Set the value held by this reference. The reference is assumed to take ownership of this value, so this value
         * should not be mutated outside the scope of this {@link ReadWriteHandle}.
         *
         * @param value The new value.
         */
        void set(T value);
    }

    /**
     * Acquire an exclusive handle to this reference.
     *
     * @return A handle used to access this reference.
     */
    @MustBeClosed
    public abstract H lock();

    /**
     * Create a {@link LockedReference} whose value can be changed.
     *
     * @param name  The name of this locked reference.
     * @param value The initial value of this reference.
     * @param <T>   The type of value owned by this reference.
     * @return The newly created reference.
     */
    public static <T extends @Nullable Object> Mutable<T> mutable(String name, T value) {
        return new Mutable<>(name, value);
    }

    /**
     * A {@link LockedReference} which can be read and written to.
     *
     * @param <T> The type of value owned by this reference.
     */
    public static final class Mutable<T extends @Nullable Object> extends LockedReference<ReadWriteHandle<T>> {
        private final ReentrantLock lock;
        @GuardedBy("lock")
        private T value;
        private final MutableHandleImpl<T> handle = new MutableHandleImpl<>(this);

        private Mutable(String name, T value) {
            this.lock = ThreadUtils.newReentrantLock(name);
            this.value = value;
        }

        /**
         * Acquire an exclusive {@link ReadWriteHandle} to this value.
         *
         * @return A handle used to access this value.
         */
        @Override
        @MustBeClosed
        public ReadWriteHandle<T> lock() {
            if (lock.isHeldByCurrentThread()) throw new IllegalMonitorStateException("Nested access are not supported");
            lock.lock();
            return handle;
        }
    }

    private static final class MutableHandleImpl<T extends @Nullable Object> implements ReadWriteHandle<T> {
        private final Mutable<T> owner;

        private MutableHandleImpl(Mutable<T> owner) {
            this.owner = owner;
        }

        @Override
        @GuardedBy("owner.lock")
        public T get() {
            if (!owner.lock.isHeldByCurrentThread()) throw new IllegalMonitorStateException("Handle must be open");
            return owner.value;
        }

        @Override
        @GuardedBy("owner.lock")
        public void set(T value) {
            if (!owner.lock.isHeldByCurrentThread()) throw new IllegalMonitorStateException("Handle must be open");
            owner.value = value;
        }

        @Override
        public void close() {
            owner.lock.unlock();
        }
    }
}
