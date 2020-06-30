/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.generic.data;

import net.minecraft.util.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DataHelpers
{
    public static Map<String, Boolean> getTags( Collection<ResourceLocation> tags )
    {
        Map<String, Boolean> result = new HashMap<>( tags.size() );
        for( ResourceLocation location : tags ) result.put( location.toString(), true );
        return result;
    }
}
