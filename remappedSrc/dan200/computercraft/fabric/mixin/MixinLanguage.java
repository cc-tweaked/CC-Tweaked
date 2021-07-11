/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.fabric.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonParseException;
import dan200.computercraft.shared.peripheral.generic.data.ItemData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Language;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.function.BiConsumer;

/**
 * Loads all mods en_us lang file into the default Language instance on the dedicated server.
 * Needed so that lua code running on the server can access the display name of items.
 *
 * @see ItemData#fill
 */
@Mixin( Language.class )
public class MixinLanguage
{
    @Shadow
    private static Logger LOGGER;

    @Shadow
    public static void load( InputStream inputStream, BiConsumer<String, String> entryConsumer )
    {
    }

    private static void loadModLangFile( String modId, BiConsumer<String, String> biConsumer )
    {
        String path = "/assets/" + modId + "/lang/en_us.json";

        try ( InputStream inputStream = Language.class.getResourceAsStream( path ) )
        {
            if ( inputStream == null ) return;
            load( inputStream, biConsumer );
        }
        catch ( JsonParseException | IOException e )
        {
            LOGGER.error( "Couldn't read strings from " + path, e );
        }
    }

    @Inject( method = "create", locals = LocalCapture.CAPTURE_FAILSOFT, at = @At( value = "INVOKE", remap = false, target = "Lcom/google/common/collect/ImmutableMap$Builder;build()Lcom/google/common/collect/ImmutableMap;" ) )
    private static void create( CallbackInfoReturnable<Language> cir, ImmutableMap.Builder<String, String> builder )
    {
        /*  We must ensure that the keys are de-duplicated because we can't catch the error that might otherwise
         *  occur when the injected function calls build() on the ImmutableMap builder. So we use our own hash map and
         *  exclude "minecraft", as the injected function has already loaded those keys at this point.
         */
        HashMap<String, String> translations = new HashMap<>();

        FabricLoader.getInstance().getAllMods().stream().map( modContainer -> modContainer.getMetadata().getId() )
            .filter( id -> !id.equals( "minecraft" ) ).forEach( id -> {
                loadModLangFile( id, translations::put );
            } );

        builder.putAll( translations );
    }
}
