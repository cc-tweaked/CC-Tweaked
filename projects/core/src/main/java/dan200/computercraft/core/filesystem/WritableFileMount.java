// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.filesystem;

import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.WritableMount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import static dan200.computercraft.api.filesystem.MountConstants.*;


/**
 * A {@link WritableFileMount} implementation which provides read-write access to a directory.
 */
public class WritableFileMount extends FileMount implements WritableMount {
    private static final Logger LOG = LoggerFactory.getLogger(WritableFileMount.class);

    protected final File rootFile;
    private final long capacity;
    private long usedSpace;

    public WritableFileMount(File rootFile, long capacity) {
        super(rootFile.toPath());
        this.rootFile = rootFile;
        this.capacity = capacity + MINIMUM_FILE_SIZE;
        usedSpace = created() ? measureUsedSpace(root) : MINIMUM_FILE_SIZE;
    }

    protected File resolveFile(String path) {
        return new File(rootFile, path);
    }

    private void create() throws FileOperationException {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new FileOperationException(ACCESS_DENIED);
        }
    }

    @Override
    public long getRemainingSpace() {
        return Math.max(capacity - usedSpace, 0);
    }

    @Override
    public long getCapacity() {
        return capacity - MINIMUM_FILE_SIZE;
    }

    @Override
    public boolean isReadOnly(String path) {
        var file = resolveFile(path);
        while (true) {
            if (file.exists()) return !file.canWrite();
            if (file.equals(rootFile)) return false;
            file = file.getParentFile();
        }
    }

    @Override
    public void makeDirectory(String path) throws IOException {
        create();
        var file = resolveFile(path);
        if (file.exists()) {
            if (!file.isDirectory()) throw new FileOperationException(path, FILE_EXISTS);
            return;
        }

        var dirsToCreate = 1;
        var parent = file.getParentFile();
        while (!parent.exists()) {
            ++dirsToCreate;
            parent = parent.getParentFile();
        }

        if (getRemainingSpace() < dirsToCreate * MINIMUM_FILE_SIZE) {
            throw new FileOperationException(path, OUT_OF_SPACE);
        }

        if (file.mkdirs()) {
            usedSpace += dirsToCreate * MINIMUM_FILE_SIZE;
        } else {
            throw new FileOperationException(path, ACCESS_DENIED);
        }
    }

    @Override
    public void delete(String path) throws IOException {
        if (path.isEmpty()) throw new FileOperationException(path, ACCESS_DENIED);

        if (created()) {
            var file = resolveFile(path);
            if (file.exists()) deleteRecursively(file);
        }
    }

    private void deleteRecursively(File file) throws IOException {
        // Empty directories first
        if (file.isDirectory()) {
            var children = file.list();
            for (var aChildren : children) {
                deleteRecursively(new File(file, aChildren));
            }
        }

        // Then delete
        var fileSize = file.isDirectory() ? 0 : file.length();
        var success = file.delete();
        if (success) {
            usedSpace -= Math.max(MINIMUM_FILE_SIZE, fileSize);
        } else {
            throw new IOException(ACCESS_DENIED);
        }
    }

    @Override
    public void rename(String source, String dest) throws FileOperationException {
        var sourceFile = resolvePath(source);
        var destFile = resolvePath(dest);
        if (!Files.exists(sourceFile)) throw new FileOperationException(source, NO_SUCH_FILE);
        if (Files.exists(destFile)) throw new FileOperationException(dest, FILE_EXISTS);

        if (destFile.startsWith(sourceFile)) {
            throw new FileOperationException(source, "Cannot move a directory inside itself");
        }

        try {
            Files.move(sourceFile, destFile);
        } catch (IOException e) {
            throw remapException(source, e);
        }
    }

    private @Nullable BasicFileAttributes tryGetAttributes(String path, Path resolved) throws FileOperationException {
        try {
            return Files.readAttributes(resolved, BasicFileAttributes.class);
        } catch (NoSuchFileException ignored) {
            return null;
        } catch (IOException e) {
            throw remapException(path, e);
        }
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

        create();

        var file = resolvePath(path);
        var attributes = tryGetAttributes(path, file);
        if (attributes != null && attributes.isDirectory()) {
            throw new FileOperationException(path, flags.create() ? CANNOT_WRITE_TO_DIRECTORY : NOT_A_FILE);
        }

        if (attributes == null) {
            if (!flags.create()) throw new FileOperationException(path, NO_SUCH_FILE);

            if (getRemainingSpace() < MINIMUM_FILE_SIZE) throw new FileOperationException(path, OUT_OF_SPACE);
            usedSpace += MINIMUM_FILE_SIZE;
        } else if (flags.truncate()) {
            usedSpace -= Math.max(attributes.size(), MINIMUM_FILE_SIZE);
            usedSpace += MINIMUM_FILE_SIZE;
        }

        // Allowing seeking when appending is not recommended, so we use a separate channel.
        try {
            return new CountingChannel(Files.newByteChannel(file, options));
        } catch (IOException e) {
            throw remapException(path, e);
        }
    }

    private class CountingChannel implements SeekableByteChannel {
        private final SeekableByteChannel channel;

        CountingChannel(SeekableByteChannel channel) {
            this.channel = channel;
        }

        @Override
        public int write(ByteBuffer b) throws IOException {
            var toWrite = b.remaining();

            // If growing the file, make sure we have space for it.
            var newPosition = Math.addExact(channel.position(), toWrite);
            var newBytes = newPosition - Math.max(MINIMUM_FILE_SIZE, channel.size());
            if (newBytes > 0) {
                var newUsedSpace = Math.addExact(usedSpace, newBytes);
                if (newUsedSpace > capacity) throw new IOException(OUT_OF_SPACE);
                usedSpace = newUsedSpace;
            }

            var written = channel.write(b);

            // Some safety checks to check our file size accounting is reasonable.
            if (written != toWrite) throw new IllegalStateException("Not all bytes were written");
            assert channel.position() == newPosition : "Position is consistent";

            return written;
        }

        @Override
        public boolean isOpen() {
            return channel.isOpen();
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            if (!isOpen()) throw new ClosedChannelException();
            if (newPosition < 0) throw new IllegalArgumentException("Cannot seek before the beginning of the stream");

            return channel.position(newPosition);
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            throw new UnsupportedOperationException("File cannot be truncated");
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return channel.read(dst);
        }

        @Override
        public long position() throws IOException {
            return channel.position();
        }

        @Override
        public long size() throws IOException {
            return channel.size();
        }
    }

    private static long measureUsedSpace(Path path) {
        if (!Files.exists(path)) return 0;

        class CountingVisitor extends SimpleFileVisitor<Path> {
            long size;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                size += MINIMUM_FILE_SIZE;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size += Math.max(attrs.size(), MINIMUM_FILE_SIZE);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOG.error("Error computing file size for {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        }

        try {
            var visitor = new CountingVisitor();
            Files.walkFileTree(path, visitor);
            return visitor.size;
        } catch (IOException e) {
            LOG.error("Error computing file size for {}", path, e);
            return 0;
        }
    }
}
