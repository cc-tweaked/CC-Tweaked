/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import javax.annotation.Nonnull;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.IMount;

public class EmptyMount implements IMount {
    @Override
    public void list(@Nonnull String path, @Nonnull List<String> contents) {
    }

    @Nonnull
    @Override
    public ReadableByteChannel openForRead(@Nonnull String path) throws IOException {
        throw new FileOperationException(path, "No such file");
    }

    @Override
    public boolean exists(@Nonnull String path) {
        return path.isEmpty();
    }

    @Override
    public boolean isDirectory(@Nonnull String path) {
        return path.isEmpty();
    }

    @Override
    public long getSize(@Nonnull String path) {
        return 0;
    }
}
