/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.upload;

import java.nio.ByteBuffer;

public class FileUpload
{
    private final String name;
    private final ByteBuffer bytes;

    public FileUpload( String name, ByteBuffer bytes )
    {
        this.name = name;
        this.bytes = bytes;
    }

    public String getName()
    {
        return name;
    }

    public ByteBuffer getBytes()
    {
        return bytes;
    }
}
