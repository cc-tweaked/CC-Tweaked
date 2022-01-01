/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.IMount;
import net.minecraft.resources.FolderPack;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.SimpleReloadableResourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceMountTest
{
    private IMount mount;

    @BeforeEach
    public void before()
    {
        SimpleReloadableResourceManager manager = new SimpleReloadableResourceManager( ResourcePackType.SERVER_DATA );
        manager.add( new FolderPack( new File( "src/main/resources" ) ) );

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
