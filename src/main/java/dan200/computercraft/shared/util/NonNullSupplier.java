package dan200.computercraft.shared.util;

import javax.annotation.Nonnull;

/**
 * Equivalent to {@link Supplier}, except with nonnull contract.
 *
 * @see Supplier
 */
@FunctionalInterface
public interface NonNullSupplier<T>
{
    @Nonnull
    T get();
}
