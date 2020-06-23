/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.filesystem;

import com.google.common.io.Files;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.apis.ObjectWrapper;
import dan200.computercraft.core.apis.handles.EncodedWritableHandle;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileSystemTest
{
    private static final File ROOT = new File( "test-files/filesystem" );

    /**
     * Ensures writing a file truncates it.
     *
     * @throws FileSystemException When the file system cannot be constructed.
     * @throws LuaException        When Lua functions fail.
     * @throws IOException         When reading and writing from strings
     */
    @Test
    public void testWriteTruncates() throws FileSystemException, LuaException, IOException
    {
        IWritableMount writableMount = new FileMount( ROOT, 1000000 );
        FileSystem fs = new FileSystem( "hdd", writableMount );

        {
            FileSystemWrapper<BufferedWriter> writer = fs.openForWrite( "out.txt", false, EncodedWritableHandle::openUtf8 );
            ObjectWrapper wrapper = new ObjectWrapper( new EncodedWritableHandle( writer.get(), writer ) );
            wrapper.call( "write", "This is a long line" );
            wrapper.call( "close" );
        }

        assertEquals( "This is a long line", Files.asCharSource( new File( ROOT, "out.txt" ), StandardCharsets.UTF_8 ).read() );

        {
            FileSystemWrapper<BufferedWriter> writer = fs.openForWrite( "out.txt", false, EncodedWritableHandle::openUtf8 );
            ObjectWrapper wrapper = new ObjectWrapper( new EncodedWritableHandle( writer.get(), writer ) );
            wrapper.call( "write", "Tiny line" );
            wrapper.call( "close" );
        }

        assertEquals( "Tiny line", Files.asCharSource( new File( ROOT, "out.txt" ), StandardCharsets.UTF_8 ).read() );
    }
}
