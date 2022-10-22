/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.gson.JsonElement;
import net.minecraft.block.Block;
import net.minecraft.data.*;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A copy of {@link net.minecraft.data.BlockStateProvider} which accepts a custom generator.
 * <p>
 * Please don't sue me Mojang. Or at least make these changes to vanilla before doing so!
 */
public class ModelProvider implements IDataProvider
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final DataGenerator generator;

    private final Consumer<BlockModelProvider> blocks;
    private final Consumer<ItemModelProvider> items;

    public ModelProvider( DataGenerator generator, Consumer<BlockModelProvider> blocks, Consumer<ItemModelProvider> items )
    {
        this.generator = generator;
        this.blocks = blocks;
        this.items = items;
    }

    @Override
    public void run( @Nonnull DirectoryCache output )
    {
        Map<Block, IFinishedBlockState> blockStates = new HashMap<>();
        Consumer<IFinishedBlockState> addBlockState = generator -> {
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
        blocks.accept( new BlockModelProvider( addBlockState, addModel, explicitItems::add ) );
        items.accept( new ItemModelProvider( addModel ) );

        for( Block block : ForgeRegistries.BLOCKS )
        {
            if( !blockStates.containsKey( block ) ) continue;

            Item item = Item.BY_BLOCK.get( block );
            if( item == null || explicitItems.contains( item ) ) continue;

            ResourceLocation model = ModelsResourceUtil.getModelLocation( item );
            if( !models.containsKey( model ) )
            {
                models.put( model, new BlockModelWriter( ModelsResourceUtil.getModelLocation( block ) ) );
            }
        }

        saveCollection( output, generator.getOutputFolder(), blockStates, ModelProvider::createBlockStatePath );
        saveCollection( output, generator.getOutputFolder(), models, ModelProvider::createModelPath );
    }

    private <T> void saveCollection( DirectoryCache output, Path root, Map<T, ? extends Supplier<JsonElement>> items, BiFunction<Path, T, Path> getLocation )
    {
        for( Map.Entry<T, ? extends Supplier<JsonElement>> entry : items.entrySet() )
        {
            Path path = getLocation.apply( root, entry.getKey() );
            try
            {
                PrettyJsonWriter.save( output, entry.getValue().get(), path );
            }
            catch( Exception exception )
            {
                LOGGER.error( "Couldn't save {}", path, exception );
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
