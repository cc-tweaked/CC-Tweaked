/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class BlockData
{
    @Nonnull
    public static <T extends Map<? super String, Object>> T fill( @Nonnull T data, @Nonnull BlockState state )
    {
        data.put( "name", DataHelpers.getId( state.getBlock() ) );

        Map<Object, Object> stateTable = new HashMap<>();
        for( ImmutableMap.Entry<Property<?>, ? extends Comparable<?>> entry : state.getValues().entrySet() )
        {
            Property<?> property = entry.getKey();
            stateTable.put( property.getName(), getPropertyValue( property, entry.getValue() ) );
        }
        data.put( "state", stateTable );

        return data;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static Object getPropertyValue( Property property, Comparable value )
    {
        if( value instanceof String || value instanceof Number || value instanceof Boolean ) return value;
        return property.getName( value );
    }
}
