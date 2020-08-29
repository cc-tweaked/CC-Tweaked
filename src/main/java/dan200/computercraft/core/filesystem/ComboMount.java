/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import dan200.computercraft.api.filesystem.IMount;

public class ComboMount implements IMount {
    private IMount[] m_parts;

    public ComboMount(IMount[] parts) {
        this.m_parts = parts;
    }

    // IMount implementation

    @Override
    public boolean exists(@Nonnull String path) throws IOException {
        for (int i = this.m_parts.length - 1; i >= 0; --i) {
            IMount part = this.m_parts[i];
            if (part.exists(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDirectory(@Nonnull String path) throws IOException {
        for (int i = this.m_parts.length - 1; i >= 0; --i) {
            IMount part = this.m_parts[i];
            if (part.isDirectory(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void list(@Nonnull String path, @Nonnull List<String> contents) throws IOException {
        // Combine the lists from all the mounts
        List<String> foundFiles = null;
        int foundDirs = 0;
        for (int i = this.m_parts.length - 1; i >= 0; --i) {
            IMount part = this.m_parts[i];
            if (part.exists(path) && part.isDirectory(path)) {
                if (foundFiles == null) {
                    foundFiles = new ArrayList<>();
                }
                part.list(path, foundFiles);
                foundDirs++;
            }
        }

        if (foundDirs == 1) {
            // We found one directory, so we know it already doesn't contain duplicates
            contents.addAll(foundFiles);
        } else if (foundDirs > 1) {
            // We found multiple directories, so filter for duplicates
            Set<String> seen = new HashSet<>();
            for (String file : foundFiles) {
                if (seen.add(file)) {
                    contents.add(file);
                }
            }
        } else {
            throw new IOException("/" + path + ": Not a directory");
        }
    }

    @Override
    public long getSize(@Nonnull String path) throws IOException {
        for (int i = this.m_parts.length - 1; i >= 0; --i) {
            IMount part = this.m_parts[i];
            if (part.exists(path)) {
                return part.getSize(path);
            }
        }
        throw new IOException("/" + path + ": No such file");
    }

    @Nonnull
    @Override
    public ReadableByteChannel openChannelForRead(@Nonnull String path) throws IOException {
        for (int i = this.m_parts.length - 1; i >= 0; --i) {
            IMount part = this.m_parts[i];
            if (part.exists(path) && !part.isDirectory(path)) {
                return part.openChannelForRead(path);
            }
        }
        throw new IOException("/" + path + ": No such file");
    }

    @Nonnull
    @Override
    @Deprecated
    public InputStream openForRead(@Nonnull String path) throws IOException {
        for (int i = this.m_parts.length - 1; i >= 0; --i) {
            IMount part = this.m_parts[i];
            if (part.exists(path) && !part.isDirectory(path)) {
                return part.openForRead(path);
            }
        }
        throw new IOException("/" + path + ": No such file");
    }
}
