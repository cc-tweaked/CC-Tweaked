/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.generic.data;

import dan200.computercraft.api.detail.BlockReference;
import net.minecraft.block.BlockState;
import net.minecraft.state.Property;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class BlockData
{
    public static void fillBasic( @Nonnull Map<? super String, Object> data, @Nonnull BlockReference block )
    {
        BlockState state = block.getState();

        data.put( "name", DataHelpers.getId( state.getBlock() ) );

        Map<Object, Object> stateTable = new HashMap<>();
        for( Map.Entry<Property<?>, ? extends Comparable<?>> entry : state.getValues().entrySet() )
        {
            Property<?> property = entry.getKey();
            stateTable.put( property.getName(), getPropertyValue( property, entry.getValue() ) );
        }
        data.put( "state", stateTable );
    }

    public static void fill( @Nonnull Map<? super String, Object> data, @Nonnull BlockReference block )
    {
        data.put( "tags", DataHelpers.getTags( block.getState().getBlock().getTags() ) );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private static Object getPropertyValue( Property property, Comparable value )
    {
        if( value instanceof String || value instanceof Number || value instanceof Boolean ) return value;
        return property.getName( value );
    }
}
