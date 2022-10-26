/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.upload;

import dan200.computercraft.ComputerCraft;

import java.nio.ByteBuffer;
import java.util.List;

public record FileSlice(int fileId, int offset, ByteBuffer bytes)
{
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
