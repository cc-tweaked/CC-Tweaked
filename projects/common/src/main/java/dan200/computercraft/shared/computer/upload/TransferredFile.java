// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.upload;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.handles.BinaryReadableHandle;
import dan200.computercraft.core.apis.handles.ByteBufferChannel;
import dan200.computercraft.core.methods.ObjectSource;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Optional;

/**
 * A binary file handle that has been transferred to this computer.
 * <p>
 * This inherits all methods of {@link BinaryReadableHandle binary file handles}, meaning you can use the standard
 * {@link BinaryReadableHandle#read(Optional) read functions} to access the contents of the file.
 *
 * @cc.module [kind=event] file_transfer.TransferredFile
 * @see BinaryReadableHandle
 */
public class TransferredFile implements ObjectSource {
    private final String name;
    private final BinaryReadableHandle handle;

    public TransferredFile(String name, ByteBuffer contents) {
        this.name = name;
        handle = BinaryReadableHandle.of(new ByteBufferChannel(contents));
    }

    /**
     * Get the name of this file being transferred.
     *
     * @return The file's name.
     */
    @LuaFunction
    public final String getName() {
        return name;
    }

    @Override
    public Iterable<Object> getExtra() {
        return Collections.singleton(handle);
    }
}
