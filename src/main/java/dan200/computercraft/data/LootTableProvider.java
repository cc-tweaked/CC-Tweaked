/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.data;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dan200.computercraft.ComputerCraft;
import net.minecraft.data.DataCache;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.loot.LootManager;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.util.Identifier;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * An alternative to {@link net.minecraft.data.LootTableProvider}, with a more flexible interface.
 */
public abstract class LootTableProvider implements DataProvider
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final DataGenerator generator;

    public LootTableProvider( DataGenerator generator )
    {
        this.generator = generator;
    }

    @Override
    public void run( @Nonnull DataCache cache )
    {
        Map<Identifier, LootTable> tables = new HashMap<>();
        LootTableReporter validation = new LootTableReporter( LootContextTypes.GENERIC, x -> null, tables::get );

        registerLoot( ( id, table ) -> {
            if( tables.containsKey( id ) ) validation.report( "Duplicate loot tables for " + id );
            tables.put( id, table );
        } );

        tables.forEach( ( key, value ) -> LootManager.validate( validation, key, value ) );

        Multimap<String, String> problems = validation.getMessages();
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
                DataProvider.writeToPath( GSON, cache, LootManager.toJson( value ), path );
            }
            catch( IOException e )
            {
                ComputerCraft.log.error( "Couldn't save loot table {}", path, e );
            }
        } );
    }

    protected abstract void registerLoot( BiConsumer<Identifier, LootTable> add );

    @Nonnull
    @Override
    public String getName()
    {
        return "LootTables";
    }

    private Path getPath( Identifier id )
    {
        return generator.getOutput()
            .resolve( "data" ).resolve( id.getNamespace() ).resolve( "loot_tables" )
            .resolve( id.getPath() + ".json" );
    }
}
