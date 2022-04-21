/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.upgrades;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.internal.upgrades.SerialiserWithCraftingItem;
import dan200.computercraft.internal.upgrades.SimpleSerialiser;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A data generator/provider for turtle and pocket computer upgrades. This should not be extended direclty, instead see
 * the other sub-classes.
 *
 * @param <T> The base class of upgrades.
 * @param <R> The upgrade serialiser to register for.
 */
public abstract class UpgradeDataProvider<T extends IUpgradeBase, R extends UpgradeSerialiser<?, R>> implements DataProvider
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final DataGenerator generator;
    private final String name;
    private final String folder;
    private final ResourceKey<Registry<R>> registry;

    private List<T> upgrades;

    protected UpgradeDataProvider( @Nonnull DataGenerator generator, @Nonnull String name, @Nonnull String folder, @Nonnull ResourceKey<Registry<R>> registry )
    {
        this.generator = generator;
        this.name = name;
        this.folder = folder;
        this.registry = registry;
    }

    /**
     * Register an upgrade using a "simple" serialiser (e.g. {@link TurtleUpgradeSerialiser#simple(Function)}).
     *
     * @param id         The ID of the upgrade to create.
     * @param serialiser The simple serialiser.
     * @return The constructed upgrade, ready to be passed off to {@link #addUpgrades(Consumer)}'s consumer.
     */
    @Nonnull
    public final Upgrade<R> simple( @Nonnull ResourceLocation id, @Nonnull R serialiser )
    {
        if( !(serialiser instanceof SimpleSerialiser) )
        {
            throw new IllegalStateException( serialiser + " must be a simple() seriaiser." );
        }

        return new Upgrade<>( id, serialiser, s -> {} );
    }

    /**
     * Register an upgrade using a "simple" serialiser (e.g. {@link TurtleUpgradeSerialiser#simple(Function)}).
     *
     * @param id         The ID of the upgrade to create.
     * @param serialiser The simple serialiser.
     * @param item       The crafting upgrade for this item.
     * @return The constructed upgrade, ready to be passed off to {@link #addUpgrades(Consumer)}'s consumer.
     */
    @Nonnull
    public final Upgrade<R> simpleWithCustomItem( @Nonnull ResourceLocation id, @Nonnull R serialiser, @Nonnull Item item )
    {
        if( !(serialiser instanceof SerialiserWithCraftingItem) )
        {
            throw new IllegalStateException( serialiser + " must be a simpleWithCustomItem() serialiser." );
        }

        return new Upgrade<>( id, serialiser, s ->
            s.addProperty( "item", Objects.requireNonNull( item.getRegistryName(), "Item is not registered" ).toString() )
        );
    }

    /**
     * Add all turtle or pocket computer upgrades.
     *
     * <strong>Example usage:</strong>
     * <pre>{@code
     * protected void addUpgrades(@Nonnull Consumer<Upgrade<TurtleUpgradeSerialiser<?>>> addUpgrade) {
     *     simple(new ResourceLocation("mymod", "speaker"), SPEAKER_SERIALISER.get()).add(addUpgrade);
     * }
     * }</pre>
     *
     * @param addUpgrade A callback used to register an upgrade.
     */
    protected abstract void addUpgrades( @Nonnull Consumer<Upgrade<R>> addUpgrade );

    @Override
    public final void run( @Nonnull HashCache cache ) throws IOException
    {
        Path base = generator.getOutputFolder().resolve( "data" );

        Set<ResourceLocation> seen = new HashSet<>();
        List<T> upgrades = new ArrayList<>();
        addUpgrades( upgrade -> {
            if( !seen.add( upgrade.id() ) ) throw new IllegalStateException( "Duplicate upgrade " + upgrade.id() );

            var json = new JsonObject();
            json.addProperty( "type", Objects.requireNonNull( upgrade.serialiser().getRegistryName(), "Serialiser has not been registered" ).toString() );
            upgrade.serialise().accept( json );

            try
            {
                DataProvider.save( GSON, cache, json, base.resolve( upgrade.id().getNamespace() + "/" + folder + "/" + upgrade.id().getPath() + ".json" ) );
            }
            catch( IOException e )
            {
                LOGGER.error( "Failed to save {} {}", name, upgrade.id(), e );
            }

            try
            {
                @SuppressWarnings( "unchecked" ) var result = (T) upgrade.serialiser().fromJson( upgrade.id(), json );
                upgrades.add( result );
            }
            catch( IllegalArgumentException | JsonParseException e )
            {
                LOGGER.error( "Failed to parse {} {}", name, upgrade.id(), e );
            }
        } );

        this.upgrades = upgrades;
    }

    @Nonnull
    @Override
    public final String getName()
    {
        return name;
    }

    @Nonnull
    public final R existingSerialiser( @Nonnull ResourceLocation id )
    {
        var result = RegistryManager.ACTIVE.getRegistry( registry ).getValue( id );
        if( result == null ) throw new IllegalArgumentException( "No such serialiser " + registry );
        return result;
    }

    @Nonnull
    public List<T> getGeneratedUpgrades()
    {
        if( upgrades == null ) throw new IllegalStateException( "Upgrades have not beeen generated yet" );
        return upgrades;
    }

    /**
     * A constructed upgrade instance, produced {@link #addUpgrades(Consumer)}.
     *
     * @param id         The ID for this upgrade.
     * @param serialiser The serialiser which reads and writes this upgrade.
     * @param serialise  Augment the generated JSON with additional fields.
     * @param <R>        The type of upgrade serialiser.
     */
    public static record Upgrade<R extends UpgradeSerialiser<?, R>>(
        ResourceLocation id, R serialiser, Consumer<JsonObject> serialise
    )
    {
        /**
         * Convenience method for registering an upgrade.
         *
         * @param add The callback given to {@link #addUpgrades(Consumer)}
         */
        public void add( @Nonnull Consumer<Upgrade<R>> add )
        {
            add.accept( this );
        }
    }
}
