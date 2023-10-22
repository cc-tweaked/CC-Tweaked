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
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import static dan200.computercraft.core.filesystem.MountHelpers.*;

/**
 * A {@link WritableFileMount} implementation which provides read-write access to a directory.
 */
public class WritableFileMount extends FileMount implements WritableMount {
    private static final Logger LOG = LoggerFactory.getLogger(WritableFileMount.class);

    private static final Set<OpenOption> WRITE_OPTIONS = Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    private static final Set<OpenOption> APPEND_OPTIONS = Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

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
    public SeekableByteChannel openForWrite(String path) throws FileOperationException {
        create();

        var file = resolvePath(path);
        var attributes = tryGetAttributes(path, file);
        if (attributes == null) {
            if (getRemainingSpace() < MINIMUM_FILE_SIZE) throw new FileOperationException(path, OUT_OF_SPACE);
        } else if (attributes.isDirectory()) {
            throw new FileOperationException(path, CANNOT_WRITE_TO_DIRECTORY);
        } else {
            usedSpace -= Math.max(attributes.size(), MINIMUM_FILE_SIZE);
        }

        usedSpace += MINIMUM_FILE_SIZE;

        try {
            return new CountingChannel(Files.newByteChannel(file, WRITE_OPTIONS), MINIMUM_FILE_SIZE, true);
        } catch (IOException e) {
            throw remapException(path, e);
        }
    }

    @Override
    public SeekableByteChannel openForAppend(String path) throws FileOperationException {
        create();

        var file = resolvePath(path);
        var attributes = tryGetAttributes(path, file);
        if (attributes == null) {
            if (getRemainingSpace() < MINIMUM_FILE_SIZE) throw new FileOperationException(path, OUT_OF_SPACE);
        } else if (attributes.isDirectory()) {
            throw new FileOperationException(path, CANNOT_WRITE_TO_DIRECTORY);
        }

        // Allowing seeking when appending is not recommended, so we use a separate channel.
        try {
            return new CountingChannel(
                Files.newByteChannel(file, APPEND_OPTIONS),
                Math.max(MINIMUM_FILE_SIZE - (attributes == null ? 0 : attributes.size()), 0),
                false
            );
        } catch (IOException e) {
            throw remapException(path, e);
        }
    }

    private class CountingChannel implements SeekableByteChannel {
        private final SeekableByteChannel channel;
        private long ignoredBytesLeft;
        private final boolean canSeek;

        CountingChannel(SeekableByteChannel channel, long bytesToIgnore, boolean canSeek) {
            this.channel = channel;
            ignoredBytesLeft = bytesToIgnore;
            this.canSeek = canSeek;
        }

        @Override
        public int write(ByteBuffer b) throws IOException {
            count(b.remaining());
            return channel.write(b);
        }

        void count(long n) throws IOException {
            ignoredBytesLeft -= n;
            if (ignoredBytesLeft < 0) {
                var newBytes = -ignoredBytesLeft;
                ignoredBytesLeft = 0;

                var bytesLeft = capacity - usedSpace;
                if (newBytes > bytesLeft) throw new IOException(OUT_OF_SPACE);
                usedSpace += newBytes;
            }
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
            if (!canSeek) throw new UnsupportedOperationException("File does not support seeking");
            if (newPosition < 0) {
                throw new IllegalArgumentException("Cannot seek before the beginning of the stream");
            }

            var delta = newPosition - channel.position();
            if (delta < 0) {
                ignoredBytesLeft -= delta;
            } else {
                count(delta);
            }

            return channel.position(newPosition);
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            throw new UnsupportedOperationException("File cannot be truncated");
        }

        @Override
        public int read(ByteBuffer dst) throws ClosedChannelException {
            if (!channel.isOpen()) throw new ClosedChannelException();
            throw new NonReadableChannelException();
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
