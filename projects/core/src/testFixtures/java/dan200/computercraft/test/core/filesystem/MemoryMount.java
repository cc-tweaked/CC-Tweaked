/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.test.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * In-memory file mounts.
 */
public class MemoryMount implements WritableMount {
    private final Map<String, byte[]> files = new HashMap<>();
    private final Set<String> directories = new HashSet<>();

    public MemoryMount() {
        directories.add("");
    }

    @Override
    public void makeDirectory(String path) {
        var file = new File(path);
        while (file != null) {
            directories.add(file.getPath());
            file = file.getParentFile();
        }
    }

    @Override
    public void delete(String path) {
        if (files.containsKey(path)) {
            files.remove(path);
        } else {
            directories.remove(path);
            for (var file : files.keySet().toArray(new String[0])) {
                if (file.startsWith(path)) {
                    files.remove(file);
                }
            }

            var parent = new File(path).getParentFile();
            if (parent != null) delete(parent.getPath());
        }
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        throw new IOException("Not supported");
    }

    @Override
    public WritableByteChannel openForWrite(final String path) {
        return Channels.newChannel(new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                files.put(path, toByteArray());
            }
        });
    }

    @Override
    public WritableByteChannel openForAppend(final String path) throws IOException {
        var stream = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                files.put(path, toByteArray());
            }
        };

        var current = files.get(path);
        if (current != null) stream.write(current);

        return Channels.newChannel(stream);
    }

    @Override
    public long getRemainingSpace() {
        return 1000000L;
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
    public long getCapacity() {
        return Long.MAX_VALUE;
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
