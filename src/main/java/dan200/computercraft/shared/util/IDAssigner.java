/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dan200.computercraft.ComputerCraft;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class IDAssigner
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ID_TOKEN = new TypeToken<Map<String, Integer>>()
    {
    }.getType();

    private IDAssigner()
    {
    }

    private static Map<String, Integer> ids;
    private static WeakReference<MinecraftServer> server;
    private static Path idFile;

    public static File getDir()
    {
        File root = ServerLifecycleHooks.getCurrentServer().getDataDirectory();
        // TODO: File worldDirectory = server.getWorld( World.field_234918_g_ ).getSaveHandler().getWorldDirectory();
        return new File( root, ComputerCraft.MOD_ID );
    }

    private static MinecraftServer getCachedServer()
    {
        if( server == null ) return null;

        MinecraftServer currentServer = server.get();
        if( currentServer == null ) return null;

        if( currentServer != ServerLifecycleHooks.getCurrentServer() ) return null;
        return currentServer;
    }

    public static synchronized int getNextId( String kind )
    {
        MinecraftServer currentServer = getCachedServer();
        if( currentServer == null )
        {
            // The server has changed, refetch our ID map
            server = new WeakReference<>( ServerLifecycleHooks.getCurrentServer() );

            File dir = getDir();
            dir.mkdirs();

            // Load our ID file from disk
            idFile = new File( dir, "ids.json" ).toPath();
            if( Files.isRegularFile( idFile ) )
            {
                try( Reader reader = Files.newBufferedReader( idFile, StandardCharsets.UTF_8 ) )
                {
                    ids = GSON.fromJson( reader, ID_TOKEN );
                }
                catch( Exception e )
                {
                    ComputerCraft.log.error( "Cannot load id file '" + idFile + "'", e );
                    ids = new HashMap<>();
                }
            }
            else
            {
                ids = new HashMap<>();
            }
        }

        Integer existing = ids.get( kind );
        int next = existing == null ? 0 : existing + 1;
        ids.put( kind, next );

        // We've changed the ID file, so save it back again.
        try( Writer writer = Files.newBufferedWriter( idFile, StandardCharsets.UTF_8 ) )
        {
            GSON.toJson( ids, writer );
        }
        catch( Exception e )
        {
            ComputerCraft.log.error( "Cannot update ID file '" + idFile + "'", e );
        }

        return next;
    }
}
