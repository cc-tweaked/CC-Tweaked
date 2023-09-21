// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.filesystem.AbstractInMemoryMount;
import dan200.computercraft.core.util.Nullability;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;

/**
 * A read-only mount {@link Mount} which provides a list of in-memory set of files.
 */
public class MemoryMount extends AbstractInMemoryMount<MemoryMount.FileEntry> {
    public MemoryMount() {
        root = new FileEntry("");
    }

    public MemoryMount addFile(String file, String contents) {
        getOrCreateChild(Nullability.assertNonNull(root), file, FileEntry::new).contents = contents.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    @Override
    protected long getSize(FileEntry file) {
        return file.contents == null ? 0 : file.contents.length;
    }

    @Override
    protected SeekableByteChannel openForRead(FileEntry file) throws IOException {
        if (file.contents == null) throw new FileOperationException(file.path, "File is a directory");
        return new ArrayByteChannel(file.contents);
    }

    protected static class FileEntry extends AbstractInMemoryMount.FileEntry<FileEntry> {
        @Nullable
        byte[] contents;

        protected FileEntry(String path) {
            super(path);
        }
    }
}
