// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.FileAttributes;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.core.util.Nullability;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.OpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;

import static dan200.computercraft.api.filesystem.MountConstants.*;

/**
 * A basic {@link Mount} which stores files and directories in-memory.
 */
public final class MemoryMount extends AbstractInMemoryMount<MemoryMount.FileEntry> implements WritableMount {
    private static final byte[] EMPTY = new byte[0];

    private final long capacity;

    /**
     * Create a memory mount with a 1GB capacity.
     */
    public MemoryMount() {
        this(1000_000_000);
    }

    /**
     * Create a memory mount with a custom capacity. Note, this is only used in calculations for {@link #getCapacity()}
     * and {@link #getRemainingSpace()}, it is not checked when creating or writing files.
     *
     * @param capacity The capacity of this mount.
     */
    public MemoryMount(long capacity) {
        this.capacity = capacity;
        root = new FileEntry();
        root.children = new HashMap<>();
    }

    public MemoryMount addFile(String file, byte[] contents, FileTime created, FileTime modified) {
        var entry = getOrCreateChild(Nullability.assertNonNull(root), file, x -> new FileEntry());
        entry.contents = contents;
        entry.length = contents.length;
        entry.created = created;
        entry.modified = modified;
        return this;
    }

    public MemoryMount addFile(String file, String contents, FileTime created, FileTime modified) {
        return addFile(file, contents.getBytes(StandardCharsets.UTF_8), created, modified);
    }

    public MemoryMount addFile(String file, byte[] contents) {
        return addFile(file, contents, EPOCH, EPOCH);
    }

    public MemoryMount addFile(String file, String contents) {
        return addFile(file, contents, EPOCH, EPOCH);
    }

    @Override
    protected long getSize(String path, FileEntry file) {
        return file.length;
    }

    @Override
    protected SeekableByteChannel openForRead(String path, FileEntry file) throws IOException {
        if (file.contents == null) throw new FileOperationException(path, NOT_A_FILE);
        return new EntryChannel(file, 0);
    }

    @Override
    protected BasicFileAttributes getAttributes(String path, FileEntry file) throws IOException {
        return new FileAttributes(file.isDirectory(), file.length, file.created, file.modified);
    }

    private @Nullable ParentAndName getParentAndName(String path) {
        if (path.isEmpty()) throw new IllegalArgumentException("Path is empty");
        var index = path.lastIndexOf('/');
        if (index == -1) {
            return new ParentAndName(Nullability.assertNonNull(Nullability.assertNonNull(root).children), path);
        }

        var entry = get(path.substring(0, index));
        return entry == null || entry.children == null
            ? null
            : new ParentAndName(entry.children, path.substring(index + 1));
    }

    @Override
    public void makeDirectory(String path) throws IOException {
        if (path.isEmpty()) return;

        var lastEntry = Nullability.assertNonNull(root);
        var lastIndex = 0;
        while (lastIndex < path.length()) {
            if (lastEntry.children == null) throw new NullPointerException("children is null");

            var nextIndex = path.indexOf('/', lastIndex);
            if (nextIndex < 0) nextIndex = path.length();

            var part = path.substring(lastIndex, nextIndex);
            var nextEntry = lastEntry.children.get(part);
            if (nextEntry == null) {
                lastEntry.children.put(part, nextEntry = FileEntry.newDir());
            } else if (nextEntry.children == null) {
                throw new FileOperationException(path, FILE_EXISTS);
            }

            lastEntry = nextEntry;
            lastIndex = nextIndex + 1;
        }
    }

    @Override
    public void delete(String path) throws IOException {
        if (path.isEmpty()) throw new AccessDeniedException(ACCESS_DENIED);
        var node = getParentAndName(path);
        if (node != null) node.parent().remove(node.name());
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        if (dest.startsWith(source)) throw new FileOperationException(source, "Cannot move a directory inside itself");

        var sourceParent = getParentAndName(source);
        if (sourceParent == null || !sourceParent.exists()) throw new FileOperationException(source, NO_SUCH_FILE);

        var destParent = getParentAndName(dest);
        if (destParent == null) throw new FileOperationException(dest, "Parent directory does not exist");
        if (destParent.exists()) throw new FileOperationException(dest, FILE_EXISTS);

        destParent.put(sourceParent.parent().remove(sourceParent.name()));
    }

    @Override
    @Deprecated(forRemoval = true)
    public SeekableByteChannel openForWrite(String path) throws IOException {
        return openFile(path, WRITE_OPTIONS);
    }

    @Override
    @Deprecated(forRemoval = true)
    public SeekableByteChannel openForAppend(String path) throws IOException {
        return openFile(path, APPEND_OPTIONS);
    }

    @Override
    public SeekableByteChannel openFile(String path, Set<OpenOption> options) throws IOException {
        var flags = FileFlags.of(options);

        if (path.isEmpty()) {
            throw new FileOperationException(path, flags.create() ? CANNOT_WRITE_TO_DIRECTORY : NOT_A_FILE);
        }

        var parent = getParentAndName(path);
        if (parent == null) throw new FileOperationException(path, NO_SUCH_FILE);

        var file = parent.get();
        if (file != null && file.isDirectory()) {
            throw new FileOperationException(path, flags.create() ? CANNOT_WRITE_TO_DIRECTORY : NOT_A_FILE);
        }

        if (file == null) {
            if (!flags.create()) throw new FileOperationException(path, NO_SUCH_FILE);
            parent.put(file = FileEntry.newFile());
        } else if (flags.truncate()) {
            file.contents = EMPTY;
            file.length = 0;
        }

        // Files are always read AND write, so don't need to do anything fancy here!
        return new EntryChannel(file, flags.append() ? file.length : 0);
    }

    @Override
    public long getRemainingSpace() {
        return capacity - computeUsedSpace();
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    private long computeUsedSpace() {
        Queue<FileEntry> queue = new ArrayDeque<>();
        queue.add(root);

        long size = 0;

        FileEntry entry;
        while ((entry = queue.poll()) != null) {
            if (entry.children == null) {
                size += Math.max(MINIMUM_FILE_SIZE, Nullability.assertNonNull(entry.contents).length);
            } else {
                size += MINIMUM_FILE_SIZE;
                queue.addAll(entry.children.values());
            }
        }

        return size - MINIMUM_FILE_SIZE; // Subtract one file for the root.
    }

    protected static final class FileEntry extends AbstractInMemoryMount.FileEntry<FileEntry> {
        FileTime created = EPOCH;
        FileTime modified = EPOCH;
        @Nullable
        byte[] contents;

        int length;

        static FileEntry newFile() {
            var entry = new FileEntry();
            entry.contents = EMPTY;
            entry.created = entry.modified = FileTime.from(Instant.now());
            return entry;
        }

        static FileEntry newDir() {
            var entry = new FileEntry();
            entry.children = new HashMap<>();
            entry.created = entry.modified = FileTime.from(Instant.now());
            return entry;
        }
    }

    private record ParentAndName(Map<String, FileEntry> parent, String name) {
        boolean exists() {
            return parent.containsKey(name);
        }

        @Nullable
        FileEntry get() {
            return parent.get(name);
        }

        void put(FileEntry entry) {
            assert !parent.containsKey(name);
            parent.put(name, entry);
        }
    }

    private static final class EntryChannel implements SeekableByteChannel {
        private final FileEntry entry;
        private long position;
        private boolean isOpen = true;

        private void checkClosed() throws ClosedChannelException {
            if (!isOpen()) throw new ClosedChannelException();
        }

        private EntryChannel(FileEntry entry, int position) {
            this.entry = entry;
            this.position = position;
        }

        @Override
        public int read(ByteBuffer destination) throws IOException {
            checkClosed();

            var backing = Nullability.assertNonNull(entry.contents);
            if (position >= entry.length) return -1;

            var remaining = Math.min(entry.length - (int) position, destination.remaining());
            destination.put(backing, (int) position, remaining);
            position += remaining;
            return remaining;
        }

        private byte[] ensureCapacity(int capacity) {
            var contents = Nullability.assertNonNull(entry.contents);
            if (capacity >= contents.length) {
                var newCapacity = Math.max(capacity, contents.length << 1);
                contents = entry.contents = Arrays.copyOf(contents, newCapacity);
            }

            return contents;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            var toWrite = src.remaining();
            var endPosition = position + toWrite;
            if (endPosition > 1 << 30) throw new IOException("File is too large");

            var contents = ensureCapacity((int) endPosition);
            src.get(contents, (int) position, toWrite);
            position = endPosition;
            if (endPosition > entry.length) entry.length = (int) endPosition;
            return toWrite;
        }

        @Override
        public long position() throws IOException {
            checkClosed();
            return position;
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            checkClosed();
            if (newPosition < 0) throw new IllegalArgumentException("Position out of bounds");
            this.position = newPosition;
            return this;
        }

        @Override
        public long size() throws IOException {
            checkClosed();
            return entry.length;
        }

        @Override
        public SeekableByteChannel truncate(long size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            return isOpen && entry.contents != null;
        }

        @Override
        public void close() throws IOException {
            checkClosed();
            isOpen = false;
        }
    }
}
