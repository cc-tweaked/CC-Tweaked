/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.upload;

import dan200.computercraft.ComputerCraft;

import java.nio.ByteBuffer;
import java.util.List;

public class FileSlice
{
    private final int fileId;
    private final int offset;
    private final ByteBuffer bytes;

    public FileSlice( int fileId, int offset, ByteBuffer bytes )
    {
        this.fileId = fileId;
        this.offset = offset;
        this.bytes = bytes;
    }

    public int getFileId()
    {
        return fileId;
    }

    public int getOffset()
    {
        return offset;
    }

    public ByteBuffer getBytes()
    {
        return bytes;
    }

    public void apply( List<FileUpload> files )
    {
        if( fileId < 0 || fileId >= files.size() )
        {
            ComputerCraft.log.warn( "File ID is out-of-bounds (0 <= {} < {})", fileId, files.size() );
            return;
        }

        ByteBuffer file = files.get( fileId ).getBytes();
        if( offset < 0 || offset + bytes.remaining() > file.capacity() )
        {
            ComputerCraft.log.warn( "File offset is out-of-bounds (0 <= {} <= {})", offset, file.capacity() - offset );
            return;
        }

        file.put( offset, bytes, bytes.position(), bytes.remaining() );
    }
}
