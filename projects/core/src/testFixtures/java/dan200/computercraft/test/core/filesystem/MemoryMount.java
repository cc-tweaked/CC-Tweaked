// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;

import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A read-only mount {@link Mount} which provides a list of in-memory set of files.
 */
public class MemoryMount implements Mount {
    private final Map<String, byte[]> files = new HashMap<>();
    private final Set<String> directories = new HashSet<>();

    public MemoryMount() {
        directories.add("");
    }

    @Override
    public boolean exists(String path) {
        return files.containsKey(path) || directories.contains(path);
    }

    @Override
    public boolean isDirectory(String path) {
        return directories.contains(path);
    }

    @Override
    public void list(String path, List<String> files) {
        for (var file : this.files.keySet()) {
            if (file.startsWith(path)) files.add(file.substring(path.length() + 1));
        }
    }

    @Override
    public long getSize(String path) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public SeekableByteChannel openForRead(String path) throws FileOperationException {
        var file = files.get(path);
        if (file == null) throw new FileOperationException(path, "File not found");
        return new ArrayByteChannel(file);
    }

    public MemoryMount addFile(String file, String contents) {
        files.put(file, contents.getBytes(StandardCharsets.UTF_8));
        return this;
    }
}
