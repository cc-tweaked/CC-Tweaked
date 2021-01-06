/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dan200.computercraft.ComputerCraft;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootParameterSets;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;
import net.minecraft.world.storage.loot.ValidationTracker;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * An alternative to {@link net.minecraft.data.LootTableProvider}, with a more flexible interface.
 */
public abstract class LootTableProvider implements IDataProvider
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final DataGenerator generator;

    public LootTableProvider( DataGenerator generator )
    {
        this.generator = generator;
    }

    @Override
    public void act( @Nonnull DirectoryCache cache )
    {
        Map<ResourceLocation, LootTable> tables = new HashMap<>();
        ValidationTracker validation = new ValidationTracker( LootParameterSets.GENERIC, x -> null, tables::get );

        registerLoot( ( id, table ) -> {
            if( tables.containsKey( id ) ) validation.addProblem( "Duplicate loot tables for " + id );
            tables.put( id, table );
        } );

        tables.forEach( ( key, value ) -> LootTableManager.func_227508_a_( validation, key, value ) );

        Multimap<String, String> problems = validation.getProblems();
        if( !problems.isEmpty() )
        {
            problems.forEach( ( child, problem ) ->
                ComputerCraft.log.warn( "Found validation problem in " + child + ": " + problem ) );
            throw new IllegalStateException( "Failed to validate loot tables, see logs" );
        }

        tables.forEach( ( key, value ) -> {
            Path path = getPath( key );
            try
            {
                IDataProvider.save( GSON, cache, LootTableManager.toJson( value ), path );
            }
            catch( IOException e )
            {
                ComputerCraft.log.error( "Couldn't save loot table {}", path, e );
            }
        } );
    }

    protected abstract void registerLoot( BiConsumer<ResourceLocation, LootTable> add );

    @Nonnull
    @Override
    public String getName()
    {
        return "LootTables";
    }

    private Path getPath( ResourceLocation id )
    {
        return generator.getOutputFolder()
            .resolve( "data" ).resolve( id.getNamespace() ).resolve( "loot_tables" )
            .resolve( id.getPath() + ".json" );
    }
}
