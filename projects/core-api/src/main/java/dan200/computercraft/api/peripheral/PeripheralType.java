// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * The type of a {@link GenericPeripheral}.
 * <p>
 * When determining the final type of the resulting peripheral, the union of all types is taken, with the
 * lexicographically smallest non-empty name being chosen.
 */
public final class PeripheralType {
    private static final PeripheralType UNTYPED = new PeripheralType(null, Set.of());

    private final @Nullable String type;
    private final Set<String> additionalTypes;

    public PeripheralType(@Nullable String type, Set<String> additionalTypes) {
        this.type = type;
        this.additionalTypes = additionalTypes;
        for (var item : additionalTypes) {
            if (item == null) throw new NullPointerException("All additional types must be non-null");
        }
    }

    /**
     * An empty peripheral type, used when a {@link GenericPeripheral} does not have an explicit type.
     *
     * @return The empty peripheral type.
     */
    public static PeripheralType untyped() {
        return UNTYPED;
    }

    /**
     * Create a new non-empty peripheral type.
     *
     * @param type The name of the type.
     * @return The constructed peripheral type.
     */
    public static PeripheralType ofType(String type) {
        checkTypeName("type cannot be null or empty");
        return new PeripheralType(type, Set.of());
    }

    /**
     * Create a new non-empty peripheral type with additional traits.
     *
     * @param type            The name of the type.
     * @param additionalTypes Additional types, or "traits" of this peripheral. For instance, {@code "inventory"}.
     * @return The constructed peripheral type.
     */
    public static PeripheralType ofType(String type, Collection<String> additionalTypes) {
        checkTypeName("type cannot be null or empty");
        return new PeripheralType(type, getTypes(additionalTypes));
    }

    /**
     * Create a new non-empty peripheral type with additional traits.
     *
     * @param type            The name of the type.
     * @param additionalTypes Additional types, or "traits" of this peripheral. For instance, {@code "inventory"}.
     * @return The constructed peripheral type.
     */
    public static PeripheralType ofType(String type, String... additionalTypes) {
        checkTypeName(type);
        return new PeripheralType(type, Set.of(additionalTypes));
    }

    /**
     * Create a new peripheral type with no primary type but additional traits.
     *
     * @param additionalTypes Additional types, or "traits" of this peripheral. For instance, {@code "inventory"}.
     * @return The constructed peripheral type.
     */
    public static PeripheralType ofAdditional(Collection<String> additionalTypes) {
        return new PeripheralType(null, getTypes(additionalTypes));
    }

    /**
     * Create a new peripheral type with no primary type but additional traits.
     *
     * @param additionalTypes Additional types, or "traits" of this peripheral. For instance, {@code "inventory"}.
     * @return The constructed peripheral type.
     */
    public static PeripheralType ofAdditional(String... additionalTypes) {
        return new PeripheralType(null, Set.of(additionalTypes));
    }

    /**
     * Get the name of this peripheral type. This may be {@code null}.
     *
     * @return The type of this peripheral.
     */
    @Nullable
    public String getPrimaryType() {
        return type;
    }

    /**
     * Get any additional types or "traits" of this peripheral. These effectively act as a standard set of interfaces
     * a peripheral might have.
     *
     * @return All additional types.
     */
    public Set<String> getAdditionalTypes() {
        return additionalTypes;
    }

    private static void checkTypeName(@Nullable String type) {
        if (type == null || type.isEmpty()) throw new IllegalArgumentException("type cannot be null or empty");
    }

    private static Set<String> getTypes(Collection<String> types) {
        if (types.isEmpty()) return Set.of();
        if (types.size() == 1) return Set.of(types.iterator().next());
        return Set.copyOf(types);
    }
}
