/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

import javax.annotation.Nonnull;

import dan200.computercraft.api.filesystem.IMount;

public class SubMount implements IMount {
    private IMount m_parent;
    private String m_subPath;

    public SubMount(IMount parent, String subPath) {
        this.m_parent = parent;
        this.m_subPath = subPath;
    }

    // IMount implementation

    @Override
    public boolean exists(@Nonnull String path) throws IOException {
        return this.m_parent.exists(this.getFullPath(path));
    }

    @Override
    public boolean isDirectory(@Nonnull String path) throws IOException {
        return this.m_parent.isDirectory(this.getFullPath(path));
    }

    @Override
    public void list(@Nonnull String path, @Nonnull List<String> contents) throws IOException {
        this.m_parent.list(this.getFullPath(path), contents);
    }

    @Override
    public long getSize(@Nonnull String path) throws IOException {
        return this.m_parent.getSize(this.getFullPath(path));
    }

    @Nonnull
    @Override
    public ReadableByteChannel openChannelForRead(@Nonnull String path) throws IOException {
        return this.m_parent.openChannelForRead(this.getFullPath(path));
    }

    @Nonnull
    @Override
    @Deprecated
    public InputStream openForRead(@Nonnull String path) throws IOException {
        return this.m_parent.openForRead(this.getFullPath(path));
    }

    private String getFullPath(String path) {
        if (path.isEmpty()) {
            return this.m_subPath;
        } else {
            return this.m_subPath + "/" + path;
        }
    }
}
