/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.peripheral;

import com.google.common.base.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The type of a {@link GenericPeripheral}.
 *
 * When determining the final type of the resulting peripheral, the union of all types is taken, with the
 * lexicographically smallest non-empty name being chosen.
 */
public final class PeripheralType
{
    private static final PeripheralType UNTYPED = new PeripheralType( null );

    private final String type;

    public PeripheralType( String type )
    {
        this.type = type;
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
        return new PeripheralType( type );
    }

    /**
     * Get the name of this peripheral type. This may be {@literal null}.
     *
     * @return The type of this peripheral.
     */
    @Nullable
    public String getPrimaryType()
    {
        return type;
    }
}
