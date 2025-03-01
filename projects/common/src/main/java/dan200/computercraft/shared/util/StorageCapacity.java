// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.config.ConfigSpec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A data component that sets the storage capacity of a computer or disk.
 * <p>
 * This component is not present by default, and consumers should fall back to a globally-configured config value
 * (e.g. {@link ConfigSpec#computerSpaceLimit}, {@link ConfigSpec#floppySpaceLimit}).
 *
 * @param capacity The capacity of this medium.
 * @see ServerComputer.Properties#storageCapacity(long)
 * @see ModRegistry.DataComponents#STORAGE_CAPACITY
 */
public record StorageCapacity(long capacity) {
    public static final Codec<StorageCapacity> CODEC = Codec.LONG.validate(x ->
        x > 0 ? DataResult.success(x) : DataResult.error(() -> "Capacity must be positive: " + x)
    ).xmap(StorageCapacity::new, StorageCapacity::capacity);

    public static final StreamCodec<ByteBuf, StorageCapacity> STREAM_CODEC = ByteBufCodecs.VAR_LONG.map(StorageCapacity::new, StorageCapacity::capacity);

    public StorageCapacity {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be > 0");
    }

    /**
     * Get the configured capacity, or return a default value.
     *
     * @param capacity The capacity to get.
     * @param fallback The value to fall back to. This is typically a config value.
     * @return The capacity for this computer or disk.
     */
    public static long getOrDefault(@Nullable StorageCapacity capacity, Supplier<Integer> fallback) {
        return capacity == null ? fallback.get() : capacity.capacity();
    }

    /**
     * Get the configured capacity, or return a default value.
     *
     * @param capacity The capacity to get.
     * @param fallback The value to fall back to. This is typically {@code -1}.
     * @return The capacity for this computer or disk.
     */
    public static long getOrDefault(@Nullable StorageCapacity capacity, long fallback) {
        return capacity == null ? fallback : capacity.capacity();
    }
}
