/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.upload;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.handles.BinaryReadableHandle;
import dan200.computercraft.core.apis.handles.ByteBufferChannel;
import dan200.computercraft.core.asm.ObjectSource;

import java.nio.ByteBuffer;
import java.util.Collections;

/**
 * A binary file handle which as been transferred to this computer.
 * <p>
 * This inherits all methods of {@link BinaryReadableHandle binary file handles}.
 * // TODO: @cc.module [kind=event] file_transfer.TransferredFile
 *
 * @see BinaryReadableHandle
 */
public class TransferredFile implements ObjectSource
{
    private final String name;
    private final BinaryReadableHandle handle;

    public TransferredFile( String name, ByteBuffer contents )
    {
        this.name = name;
        handle = BinaryReadableHandle.of( new ByteBufferChannel( contents ) );
    }

    /**
     * Get the name of this file. This will just be a file name, so does not contain any slashes ("/" or "\").
     *
     * @return The file's name.
     */
    @LuaFunction
    public final String getName()
    {
        return name;
    }

    @Override
    public Iterable<Object> getExtra()
    {
        return Collections.singleton( handle );
    }
}
