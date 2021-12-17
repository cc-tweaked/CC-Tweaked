/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.util;

import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.Set;
import java.util.stream.Collectors;

// A utility class for holding translation mappings prior collision resolution.
public class ServerTranslationEntry
{
    private final ModMetadata providingModMetadata;
    private final String key;
    private final String value;

    public ServerTranslationEntry( ModMetadata providingModMetadata, String key, String value )
    {
        this.providingModMetadata = providingModMetadata;
        this.key = key;
        this.value = value;
    }

    public String key()
    {
        return key;
    }

    public String value()
    {
        return value;
    }

    public String getModId()
    {
        return providingModMetadata.getId();
    }

    public Set<String> getDependencyIds()
    {
        Set<String> deps = providingModMetadata.getDepends().stream().map( ModDependency::getModId ).collect( Collectors.toSet() );
        // For the purposes of handling key collisions, all mods should depend on minecraft
        if( !getModId().equals( "minecraft" ) ) deps.add( "minecraft" );
        return deps;
    }

    public int getDependencyIntersectionSize( Set<String> idSet )
    {
        Set<String> intersection = getDependencyIds();
        intersection.retainAll( idSet );
        return intersection.size();
    }
}
