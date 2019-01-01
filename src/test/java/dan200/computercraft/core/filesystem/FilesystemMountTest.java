/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.IMount;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilesystemMountTest
{
    private static final File ZIP_FILE = new File( "test-files/filesystem-mount.zip" );

    @BeforeClass
    public static void before() throws IOException
    {
        if( ZIP_FILE.exists() ) return;
        ZIP_FILE.getParentFile().mkdirs();

        try( ZipOutputStream stream = new ZipOutputStream( new FileOutputStream( ZIP_FILE ) ) )
        {
            stream.putNextEntry( new ZipEntry( "dir/" ) );
            stream.closeEntry();

            stream.putNextEntry( new ZipEntry( "dir/file.lua" ) );
            stream.write( "print('testing')".getBytes( StandardCharsets.UTF_8 ) );
            stream.closeEntry();
        }
    }

    @Test
    public void mountsDir() throws IOException
    {
        FileSystem fs = FileSystems.newFileSystem( ZIP_FILE.toPath(), getClass().getClassLoader() );
        IMount mount = new FileSystemMount( fs, "dir" );
        assertTrue( "Root should be directory", mount.isDirectory( "" ) );
        assertTrue( "File should exist", mount.exists( "file.lua" ) );
    }

    @Test
    public void mountsFile() throws IOException
    {
        FileSystem fs = FileSystems.newFileSystem( ZIP_FILE.toPath(), getClass().getClassLoader() );
        IMount mount = new FileSystemMount( fs, "dir/file.lua" );
        assertTrue( "Root should exist", mount.exists( "" ) );
        assertFalse( "Root should be a file", mount.isDirectory( "" ) );
    }
}
