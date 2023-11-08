// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.transfer;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.handles.ReadHandle;
import dan200.computercraft.core.methods.ObjectSource;

import java.nio.channels.SeekableByteChannel;
import java.util.List;
import java.util.Optional;

/**
 * A binary file handle that has been transferred to this computer.
 * <p>
 * This inherits all methods of {@link ReadHandle binary file handles}, meaning you can use the standard
 * {@link ReadHandle#read(Optional) read functions} to access the contents of the file.
 *
 * @cc.module [kind=event] file_transfer.TransferredFile
 * @see ReadHandle
 */
public class TransferredFile implements ObjectSource {
    private final String name;
    private final ReadHandle handle;

    public TransferredFile(String name, SeekableByteChannel contents) {
        this.name = name;
        handle = new ReadHandle(contents, true);
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
        return List.of(handle);
    }
}
