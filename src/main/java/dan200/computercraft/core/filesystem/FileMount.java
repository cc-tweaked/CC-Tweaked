/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.IWritableMount;

public class FileMount implements IWritableMount {
    private static final int MINIMUM_FILE_SIZE = 500;
    private static final Set<OpenOption> READ_OPTIONS = Collections.singleton(StandardOpenOption.READ);
    private static final Set<OpenOption> WRITE_OPTIONS = Sets.newHashSet(StandardOpenOption.WRITE,
                                                                         StandardOpenOption.CREATE,
                                                                         StandardOpenOption.TRUNCATE_EXISTING);
    private static final Set<OpenOption> APPEND_OPTIONS = Sets.newHashSet(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    private File m_rootPath;
    private long m_capacity;
    private long m_usedSpace;
    public FileMount(File rootPath, long capacity) {
        this.m_rootPath = rootPath;
        this.m_capacity = capacity + MINIMUM_FILE_SIZE;
        this.m_usedSpace = this.created() ? measureUsedSpace(this.m_rootPath) : MINIMUM_FILE_SIZE;
    }

    private boolean created() {
        return this.m_rootPath.exists();
    }

    private static long measureUsedSpace(File file) {
        if (!file.exists()) {
            return 0;
        }

        try {
            Visitor visitor = new Visitor();
            Files.walkFileTree(file.toPath(), visitor);
            return visitor.size;
        } catch (IOException e) {
            ComputerCraft.log.error("Error computing file size for {}", file, e);
            return 0;
        }
    }

    // IMount implementation

    @Override
    public void list(@Nonnull String path, @Nonnull List<String> contents) throws IOException {
        if (!this.created()) {
            if (!path.isEmpty()) {
                throw new FileOperationException(path, "Not a directory");
            }
            return;
        }

        File file = this.getRealPath(path);
        if (!file.exists() || !file.isDirectory()) {
            throw new FileOperationException(path, "Not a directory");
        }

        String[] paths = file.list();
        for (String subPath : paths) {
            if (new File(file, subPath).exists()) {
                contents.add(subPath);
            }
        }
    }

    @Nonnull
    @Override
    public ReadableByteChannel openForRead(@Nonnull String path) throws IOException {
        if (this.created()) {
            File file = this.getRealPath(path);
            if (file.exists() && !file.isDirectory()) {
                return FileChannel.open(file.toPath(), READ_OPTIONS);
            }
        }

        throw new FileOperationException(path, "No such file");
    }

    @Nonnull
    @Override
    public BasicFileAttributes getAttributes(@Nonnull String path) throws IOException {
        if (this.created()) {
            File file = this.getRealPath(path);
            if (file.exists()) {
                return Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            }
        }

        throw new FileOperationException(path, "No such file");
    }

    @Override
    public boolean exists(@Nonnull String path) {
        if (!this.created()) {
            return path.isEmpty();
        }

        File file = this.getRealPath(path);
        return file.exists();
    }

    @Override
    public boolean isDirectory(@Nonnull String path) {
        if (!this.created()) {
            return path.isEmpty();
        }

        File file = this.getRealPath(path);
        return file.exists() && file.isDirectory();
    }

    @Override
    public long getSize(@Nonnull String path) throws IOException {
        if (!this.created()) {
            if (path.isEmpty()) {
                return 0;
            }
        } else {
            File file = this.getRealPath(path);
            if (file.exists()) {
                return file.isDirectory() ? 0 : file.length();
            }
        }

        throw new FileOperationException(path, "No such file");
    }

    // IWritableMount implementation

    private File getRealPath(String path) {
        return new File(this.m_rootPath, path);
    }

    @Override
    public void makeDirectory(@Nonnull String path) throws IOException {
        this.create();
        File file = this.getRealPath(path);
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new FileOperationException(path, "File exists");
            }
            return;
        }

        int dirsToCreate = 1;
        File parent = file.getParentFile();
        while (!parent.exists()) {
            ++dirsToCreate;
            parent = parent.getParentFile();
        }

        if (this.getRemainingSpace() < dirsToCreate * MINIMUM_FILE_SIZE) {
            throw new FileOperationException(path, "Out of space");
        }

        if (file.mkdirs()) {
            this.m_usedSpace += dirsToCreate * MINIMUM_FILE_SIZE;
        } else {
            throw new FileOperationException(path, "Access denied");
        }
    }

    @Override
    public void delete(@Nonnull String path) throws IOException {
        if (path.isEmpty()) {
            throw new FileOperationException(path, "Access denied");
        }

        if (this.created()) {
            File file = this.getRealPath(path);
            if (file.exists()) {
                this.deleteRecursively(file);
            }
        }
    }

    @Nonnull
    @Override
    public WritableByteChannel openForWrite(@Nonnull String path) throws IOException {
        this.create();
        File file = this.getRealPath(path);
        if (file.exists() && file.isDirectory()) {
            throw new FileOperationException(path, "Cannot write to directory");
        }

        if (file.exists()) {
            this.m_usedSpace -= Math.max(file.length(), MINIMUM_FILE_SIZE);
        } else if (this.getRemainingSpace() < MINIMUM_FILE_SIZE) {
            throw new FileOperationException(path, "Out of space");
        }
        this.m_usedSpace += MINIMUM_FILE_SIZE;

        return new SeekableCountingChannel(Files.newByteChannel(file.toPath(), WRITE_OPTIONS), MINIMUM_FILE_SIZE);
    }

    @Nonnull
    @Override
    public WritableByteChannel openForAppend(@Nonnull String path) throws IOException {
        if (!this.created()) {
            throw new FileOperationException(path, "No such file");
        }

        File file = this.getRealPath(path);
        if (!file.exists()) {
            throw new FileOperationException(path, "No such file");
        }
        if (file.isDirectory()) {
            throw new FileOperationException(path, "Cannot write to directory");
        }

        // Allowing seeking when appending is not recommended, so we use a separate channel.
        return new WritableCountingChannel(Files.newByteChannel(file.toPath(), APPEND_OPTIONS), Math.max(MINIMUM_FILE_SIZE - file.length(), 0));
    }

    @Override
    public long getRemainingSpace() {
        return Math.max(this.m_capacity - this.m_usedSpace, 0);
    }

    @Nonnull
    @Override
    public OptionalLong getCapacity() {
        return OptionalLong.of(this.m_capacity - MINIMUM_FILE_SIZE);
    }

    private void create() throws IOException {
        if (!this.m_rootPath.exists()) {
            boolean success = this.m_rootPath.mkdirs();
            if (!success) {
                throw new IOException("Access denied");
            }
        }
    }

    private void deleteRecursively(File file) throws IOException {
        // Empty directories first
        if (file.isDirectory()) {
            String[] children = file.list();
            for (String aChildren : children) {
                this.deleteRecursively(new File(file, aChildren));
            }
        }

        // Then delete
        long fileSize = file.isDirectory() ? 0 : file.length();
        boolean success = file.delete();
        if (success) {
            this.m_usedSpace -= Math.max(MINIMUM_FILE_SIZE, fileSize);
        } else {
            throw new IOException("Access denied");
        }
    }

    private static class Visitor extends SimpleFileVisitor<Path> {
        long size;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            this.size += MINIMUM_FILE_SIZE;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            this.size += Math.max(attrs.size(), MINIMUM_FILE_SIZE);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            ComputerCraft.log.error("Error computing file size for {}", file, exc);
            return FileVisitResult.CONTINUE;
        }
    }

    private class WritableCountingChannel implements WritableByteChannel {

        private final WritableByteChannel m_inner;
        long m_ignoredBytesLeft;

        WritableCountingChannel(WritableByteChannel inner, long bytesToIgnore) {
            this.m_inner = inner;
            this.m_ignoredBytesLeft = bytesToIgnore;
        }

        @Override
        public int write(@Nonnull ByteBuffer b) throws IOException {
            this.count(b.remaining());
            return this.m_inner.write(b);
        }

        void count(long n) throws IOException {
            this.m_ignoredBytesLeft -= n;
            if (this.m_ignoredBytesLeft < 0) {
                long newBytes = -this.m_ignoredBytesLeft;
                this.m_ignoredBytesLeft = 0;

                long bytesLeft = FileMount.this.m_capacity - FileMount.this.m_usedSpace;
                if (newBytes > bytesLeft) {
                    throw new IOException("Out of space");
                }
                FileMount.this.m_usedSpace += newBytes;
            }
        }

        @Override
        public boolean isOpen() {
            return this.m_inner.isOpen();
        }

        @Override
        public void close() throws IOException {
            this.m_inner.close();
        }
    }

    private class SeekableCountingChannel extends WritableCountingChannel implements SeekableByteChannel {
        private final SeekableByteChannel m_inner;

        SeekableCountingChannel(SeekableByteChannel inner, long bytesToIgnore) {
            super(inner, bytesToIgnore);
            this.m_inner = inner;
        }

        @Override
        public int read(ByteBuffer dst) throws ClosedChannelException {
            if (!this.m_inner.isOpen()) {
                throw new ClosedChannelException();
            }
            throw new NonReadableChannelException();
        }

        @Override
        public long position() throws IOException {
            return this.m_inner.position();
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            if (!this.isOpen()) {
                throw new ClosedChannelException();
            }
            if (newPosition < 0) {
                throw new IllegalArgumentException("Cannot seek before the beginning of the stream");
            }

            long delta = newPosition - this.m_inner.position();
            if (delta < 0) {
                this.m_ignoredBytesLeft -= delta;
            } else {
                this.count(delta);
            }

            return this.m_inner.position(newPosition);
        }

        @Override
        public long size() throws IOException {
            return this.m_inner.size();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            throw new IOException("Not yet implemented");
        }
    }
}
