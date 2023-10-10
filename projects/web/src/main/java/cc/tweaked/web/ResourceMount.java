// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web;

import cc.tweaked.web.js.Callbacks;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.filesystem.AbstractInMemoryMount;

import javax.annotation.Nullable;
import java.nio.channels.SeekableByteChannel;

/**
 * Mounts in files from JavaScript-supplied resources.
 *
 * @see Callbacks#listResources()
 * @see Callbacks#getResource(String)
 */
final class ResourceMount extends AbstractInMemoryMount<ResourceMount.FileEntry> {
    private static final String PREFIX = "rom/";

    ResourceMount() {
        root = new FileEntry("");
        for (var file : Callbacks.listResources()) {
            if (file.startsWith(PREFIX)) getOrCreateChild(root, file.substring(PREFIX.length()), FileEntry::new);
        }
    }

    @Override
    protected long getSize(String path, FileEntry file) {
        return file.isDirectory() ? 0 : getContents(file).length;
    }

    @Override
    protected SeekableByteChannel openForRead(String path, FileEntry file) {
        return new ArrayByteChannel(getContents(file));
    }

    private byte[] getContents(FileEntry file) {
        return file.contents != null ? file.contents : (file.contents = Callbacks.getResource(PREFIX + file.path));
    }

    protected static final class FileEntry extends AbstractInMemoryMount.FileEntry<FileEntry> {
        private final String path;
        private @Nullable byte[] contents;

        FileEntry(String path) {
            this.path = path;
        }
    }
}
