/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.IMount;
import net.minecraft.Util;
import net.minecraft.server.packs.FolderPackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceMountTest
{
    private IMount mount;

    @BeforeEach
    public void before()
    {
        ReloadableResourceManager manager = new ReloadableResourceManager( PackType.SERVER_DATA );
        CompletableFuture<Unit> done = new CompletableFuture<>();
        manager.createReload( Util.backgroundExecutor(), Util.backgroundExecutor(), done, List.of(
            new FolderPackResources( new File( "src/main/resources" ) )
        ) );

        mount = ResourceMount.get( "computercraft", "lua/rom", manager );
    }

    @Test
    public void testList() throws IOException
    {
        List<String> files = new ArrayList<>();
        mount.list( "", files );
        files.sort( Comparator.naturalOrder() );

        assertEquals(
            Arrays.asList( "apis", "autorun", "help", "modules", "motd.txt", "programs", "startup.lua" ),
            files
        );
    }

    @Test
    public void testExists() throws IOException
    {
        assertTrue( mount.exists( "" ) );
        assertTrue( mount.exists( "startup.lua" ) );
        assertTrue( mount.exists( "programs/fun/advanced/paint.lua" ) );

        assertFalse( mount.exists( "programs/fun/advance/paint.lua" ) );
        assertFalse( mount.exists( "programs/fun/advanced/paint.lu" ) );
    }

    @Test
    public void testIsDir() throws IOException
    {
        assertTrue( mount.isDirectory( "" ) );
    }

    @Test
    public void testIsFile() throws IOException
    {
        assertFalse( mount.isDirectory( "startup.lua" ) );
    }

    @Test
    public void testSize() throws IOException
    {
        assertNotEquals( mount.getSize( "startup.lua" ), 0 );
    }
}
