/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.peripheral;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * The type of a {@link GenericPeripheral}.
 *
 * When determining the final type of the resulting peripheral, the union of all types is taken, with the
 * lexicographically smallest non-empty name being chosen.
 */
public final class PeripheralType
{
    private static final PeripheralType UNTYPED = new PeripheralType( null, Collections.emptySet() );

    private final String type;
    private final Set<String> additionalTypes;

    public PeripheralType( String type, Set<String> additionalTypes )
    {
        this.type = type;
        this.additionalTypes = additionalTypes;
        if( additionalTypes.contains( null ) )
        {
            throw new IllegalArgumentException( "All additional types must be non-null" );
        }
    }

    /**
     * An empty peripheral type, used when a {@link GenericPeripheral} does not have an explicit type.
     *
     * @return The empty peripheral type.
     */
    public static PeripheralType untyped()
    {
        return UNTYPED;
    }

    /**
     * Create a new non-empty peripheral type.
     *
     * @param type The name of the type.
     * @return The constructed peripheral type.
     */
    public static PeripheralType ofType( @Nonnull String type )
    {
        if( Strings.isNullOrEmpty( type ) ) throw new IllegalArgumentException( "type cannot be null or empty" );
        return new PeripheralType( type, Collections.emptySet() );
    }

    /**
     * Create a new non-empty peripheral type with additional traits.
     *
     * @param type            The name of the type.
     * @param additionalTypes Additional types, or "traits" of this peripheral. For instance, {@code "inventory"}.
     * @return The constructed peripheral type.
     */
    public static PeripheralType ofType( @Nonnull String type, Collection<String> additionalTypes )
    {
        if( Strings.isNullOrEmpty( type ) ) throw new IllegalArgumentException( "type cannot be null or empty" );
        return new PeripheralType( type, ImmutableSet.copyOf( additionalTypes ) );
    }

    /**
     * Create a new non-empty peripheral type with additional traits.
     *
     * @param type            The name of the type.
     * @param additionalTypes Additional types, or "traits" of this peripheral. For instance, {@code "inventory"}.
     * @return The constructed peripheral type.
     */
    public static PeripheralType ofType( @Nonnull String type, @Nonnull String... additionalTypes )
    {
        if( Strings.isNullOrEmpty( type ) ) throw new IllegalArgumentException( "type cannot be null or empty" );
        return new PeripheralType( type, ImmutableSet.copyOf( additionalTypes ) );
    }

    /**
     * Create a new peripheral type with no primary type but additional traits.
     *
     * @param additionalTypes Additional types, or "traits" of this peripheral. For instance, {@code "inventory"}.
     * @return The constructed peripheral type.
     */
    public static PeripheralType ofAdditional( Collection<String> additionalTypes )
    {
        return new PeripheralType( null, ImmutableSet.copyOf( additionalTypes ) );
    }

    /**
     * Create a new peripheral type with no primary type but additional traits.
     *
     * @param additionalTypes Additional types, or "traits" of this peripheral. For instance, {@code "inventory"}.
     * @return The constructed peripheral type.
     */
    public static PeripheralType ofAdditional( @Nonnull String... additionalTypes )
    {
        return new PeripheralType( null, ImmutableSet.copyOf( additionalTypes ) );
    }

    /**
     * Get the name of this peripheral type. This may be {@code null}.
     *
     * @return The type of this peripheral.
     */
    @Nullable
    public String getPrimaryType()
    {
        return type;
    }

    /**
     * Get any additional types or "traits" of this peripheral. These effectively act as a standard set of interfaces
     * a peripheral might have.
     *
     * @return All additional types.
     */
    public Set<String> getAdditionalTypes()
    {
        return additionalTypes;
    }
}
