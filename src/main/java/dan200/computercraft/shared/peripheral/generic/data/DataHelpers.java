/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.generic.data;

import net.minecraft.util.Identifier;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class DataHelpers
{
    private DataHelpers()
    { }

    @Nonnull
    public static Map<String, Boolean> getTags( @Nonnull Collection<Identifier> tags )
    {
        Map<String, Boolean> result = new HashMap<>( tags.size() );
        for( Identifier location : tags ) result.put( location.toString(), true );
        return result;
    }

    @Nullable
    public static String getId( @Nonnull IForgeRegistryEntry<?> entry )
    {
        Identifier id = entry.getRegistryName();
        return id == null ? null : id.toString();
    }
}
