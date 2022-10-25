/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import dan200.computercraft.ComputerCraft;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A copy of {@link net.minecraft.data.models.ModelProvider} which accepts a custom generator.
 * <p>
 * Please don't sue me Mojang. Or at least make these changes to vanilla before doing so!
 */
public class ModelProvider implements DataProvider
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final DataGenerator generator;

    private final Consumer<BlockModelGenerators> blocks;
    private final Consumer<ItemModelGenerators> items;

    public ModelProvider( DataGenerator generator, Consumer<BlockModelGenerators> blocks, Consumer<ItemModelGenerators> items )
    {
        this.generator = generator;
        this.blocks = blocks;
        this.items = items;
    }

    @Override
    public void run( @Nonnull HashCache output )
    {
        Map<Block, BlockStateGenerator> blockStates = new HashMap<>();
        Consumer<BlockStateGenerator> addBlockState = generator -> {
            Block block = generator.getBlock();
            if( blockStates.containsKey( block ) )
            {
                throw new IllegalStateException( "Duplicate blockstate definition for " + block );
            }
            blockStates.put( block, generator );
        };

        Map<ResourceLocation, Supplier<JsonElement>> models = new HashMap<>();
        BiConsumer<ResourceLocation, Supplier<JsonElement>> addModel = ( id, contents ) -> {
            if( models.containsKey( id ) ) throw new IllegalStateException( "Duplicate model definition for " + id );
            models.put( id, contents );
        };
        Set<Item> explicitItems = new HashSet<>();
        blocks.accept( new BlockModelGenerators( addBlockState, addModel, explicitItems::add ) );
        items.accept( new ItemModelGenerators( addModel ) );

        for( Block block : ForgeRegistries.BLOCKS )
        {
            if( !blockStates.containsKey( block ) ) continue;

            Item item = Item.BY_BLOCK.get( block );
            if( item == null || explicitItems.contains( item ) ) continue;

            ResourceLocation model = ModelLocationUtils.getModelLocation( item );
            if( !models.containsKey( model ) )
            {
                models.put( model, new DelegatedModel( ModelLocationUtils.getModelLocation( block ) ) );
            }
        }

        saveCollection( output, generator.getOutputFolder(), blockStates, ModelProvider::createBlockStatePath );
        saveCollection( output, generator.getOutputFolder(), models, ModelProvider::createModelPath );
    }

    private <T> void saveCollection( HashCache output, Path root, Map<T, ? extends Supplier<JsonElement>> items, BiFunction<Path, T, Path> getLocation )
    {
        for( Map.Entry<T, ? extends Supplier<JsonElement>> entry : items.entrySet() )
        {
            Path path = getLocation.apply( root, entry.getKey() );
            try
            {
                String contents;
                try( StringWriter writer = new StringWriter(); JsonWriter jsonWriter = PrettyJsonWriter.createWriter( writer ) )
                {
                    GSON.toJson( entry.getValue().get(), jsonWriter );
                    contents = writer.toString();
                }

                String hash = SHA1.hashUnencodedChars( contents ).toString();
                if( !Objects.equals( output.getHash( path ), hash ) || !Files.exists( path ) )
                {
                    Files.createDirectories( path.getParent() );
                    try( BufferedWriter writer = Files.newBufferedWriter( path ) )
                    {
                        writer.write( contents );
                    }
                }

                output.putNew( path, hash );
            }
            catch( Exception exception )
            {
                ComputerCraft.log.error( "Couldn't save {}", path, exception );
            }
        }
    }

    private static Path createBlockStatePath( Path path, Block block )
    {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey( block );
        return path.resolve( "assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json" );
    }

    private static Path createModelPath( Path path, ResourceLocation id )
    {
        return path.resolve( "assets/" + id.getNamespace() + "/models/" + id.getPath() + ".json" );
    }

    @Nonnull
    @Override
    public String getName()
    {
        return "Block State Definitions";
    }
}
