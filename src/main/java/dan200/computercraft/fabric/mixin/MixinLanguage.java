/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import dan200.computercraft.fabric.util.ServerTranslationEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.font.TextVisitFactory;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Language;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Loads all mods' en_us lang file into the Language instance on the dedicated server with a basic strategy for
 * resolving collisions.
 */
@Mixin( Language.class )
public class MixinLanguage
{
    @Shadow
    @Final
    private static Logger LOGGER;

    private static final String DEFAULT = "en_us";

    @Shadow
    public static void load( InputStream inputStream, BiConsumer<String, String> entryConsumer )
    {
    }

    private static void loadModLangFile( ModContainer modContainer, BiConsumer<String, String> biConsumer )
    {
        Path path = modContainer.getPath( "assets/" + modContainer.getMetadata().getId() + "/lang/" + DEFAULT + ".json" );
        if( !Files.exists( path ) ) return;

        try( InputStream inputStream = Files.newInputStream( path ) )
        {
            load( inputStream, biConsumer );
        }
        catch( JsonParseException | IOException e )
        {
            LOGGER.error( "Couldn't read strings from " + path, e );
        }
    }

    @Inject( method = "create", cancellable = true, at = @At( "HEAD" ) )
    private static void create( CallbackInfoReturnable<Language> cir )
    {
        Map<String, List<ServerTranslationEntry>> translations = new HashMap<>();

        for( ModContainer mod : FabricLoader.getInstance().getAllMods() )
        {
            loadModLangFile( mod, ( k, v ) -> {
                if( !translations.containsKey( k ) ) translations.put( k, new ArrayList<>() );
                translations.get( k ).add( new ServerTranslationEntry( mod.getMetadata(), k, v ) );
            } );
        }

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        for( Map.Entry<String, List<ServerTranslationEntry>> keyEntry : translations.entrySet() )
        {
            if( keyEntry.getValue().size() == 1 )
            {
                // Only one value provided for this key
                builder.put( keyEntry.getKey(), keyEntry.getValue().get( 0 ).value() );
            }
            else
            {
                // Collision occurred for this key.
                // Strategy: Resolve collision by choosing value provided by the mod that depends on the greatest number
                // of other mods in this collision cluster, according to mod metadata.
                // Rationale: The mod that intends to overwrite another mod's keys is more likely to declare the
                // overwritee as a dependency.
                Set<String> clusterIds = keyEntry.getValue().stream().map( ServerTranslationEntry::getModId ).collect( Collectors.toSet() );
                ServerTranslationEntry pickedEntry = Collections.max( keyEntry.getValue(),
                    Comparator.comparingInt( entry -> entry.getDependencyIntersectionSize( clusterIds ) ) );
                builder.put( keyEntry.getKey(), pickedEntry.value() );
            }
        }

        final Map<String, String> map = builder.build();
        cir.setReturnValue( new Language()
        {
            @Override
            public String get( String key )
            {
                return map.getOrDefault( key, key );
            }

            @Override
            public boolean hasTranslation( String key )
            {
                return map.containsKey( key );
            }

            @Override
            public boolean isRightToLeft()
            {
                return false;
            }

            @Override
            public OrderedText reorder( @NotNull StringVisitable text )
            {
                return visitor -> text.visit( ( style, string ) -> TextVisitFactory.visitFormatted( string, style, visitor ) ? Optional.empty() : StringVisitable.TERMINATE_VISIT, Style.EMPTY ).isPresent();
            }
        } );
    }
}
