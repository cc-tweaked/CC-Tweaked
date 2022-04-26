/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.io.ByteStreams;
import dan200.computercraft.api.filesystem.IMount;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class JarMountTest
{
    private static final File ZIP_FILE = new File( "test-files/jar-mount.zip" );

    private static final FileTime MODIFY_TIME = FileTime.from( Instant.EPOCH.plus( 2, ChronoUnit.DAYS ) );

    @BeforeAll
    public static void before() throws IOException
    {
        ZIP_FILE.getParentFile().mkdirs();

        try( ZipOutputStream stream = new ZipOutputStream( new FileOutputStream( ZIP_FILE ) ) )
        {
            stream.putNextEntry( new ZipEntry( "dir/" ) );
            stream.closeEntry();

            stream.putNextEntry( new ZipEntry( "dir/file.lua" ).setLastModifiedTime( MODIFY_TIME ) );
            stream.write( "print('testing')".getBytes( StandardCharsets.UTF_8 ) );
            stream.closeEntry();
        }
    }

    @Test
    public void mountsDir() throws IOException
    {
        IMount mount = new JarMount( ZIP_FILE, "dir" );
        assertTrue( mount.isDirectory( "" ), "Root should be directory" );
        assertTrue( mount.exists( "file.lua" ), "File should exist" );
    }

    @Test
    public void mountsFile() throws IOException
    {
        IMount mount = new JarMount( ZIP_FILE, "dir/file.lua" );
        assertTrue( mount.exists( "" ), "Root should exist" );
        assertFalse( mount.isDirectory( "" ), "Root should be a file" );
    }

    @Test
    public void opensFileFromFile() throws IOException
    {
        IMount mount = new JarMount( ZIP_FILE, "dir/file.lua" );
        byte[] contents;
        try( ReadableByteChannel stream = mount.openForRead( "" ) )
        {
            contents = ByteStreams.toByteArray( Channels.newInputStream( stream ) );
        }

        assertEquals( new String( contents, StandardCharsets.UTF_8 ), "print('testing')" );
    }

    @Test
    public void opensFileFromDir() throws IOException
    {
        IMount mount = new JarMount( ZIP_FILE, "dir" );
        byte[] contents;
        try( ReadableByteChannel stream = mount.openForRead( "file.lua" ) )
        {
            contents = ByteStreams.toByteArray( Channels.newInputStream( stream ) );
        }

        assertEquals( new String( contents, StandardCharsets.UTF_8 ), "print('testing')" );
    }

    @Test
    public void fileAttributes() throws IOException
    {
        BasicFileAttributes attributes = new JarMount( ZIP_FILE, "dir" ).getAttributes( "file.lua" );
        assertFalse( attributes.isDirectory() );
        assertEquals( "print('testing')".length(), attributes.size() );
        assertEquals( MODIFY_TIME, attributes.lastModifiedTime() );
    }

    @Test
    public void directoryAttributes() throws IOException
    {
        BasicFileAttributes attributes = new JarMount( ZIP_FILE, "dir" ).getAttributes( "" );
        assertTrue( attributes.isDirectory() );
        assertEquals( 0, attributes.size() );
    }
}
