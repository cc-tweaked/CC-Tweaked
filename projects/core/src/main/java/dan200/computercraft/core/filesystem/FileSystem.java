// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.filesystem;

import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.core.CoreConfig;
import dan200.computercraft.core.util.IoUtil;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.OpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

import static dan200.computercraft.api.filesystem.MountConstants.*;

public class FileSystem {
    /**
     * Maximum depth that {@link #copyRecursive(String, MountWrapper, String, MountWrapper, int)} will descend into.
     * <p>
     * This is a pretty arbitrary value, though hopefully it is large enough that it'll never be normally hit. This
     * exists to prevent it overflowing if it ever gets into an infinite loop.
     */
    private static final int MAX_COPY_DEPTH = 128;

    private final Map<String, MountWrapper> mounts = new HashMap<>();

    private final HashMap<WeakReference<FileSystemWrapper<?>>, SeekableByteChannel> openFiles = new HashMap<>();
    private final ReferenceQueue<FileSystemWrapper<?>> openFileQueue = new ReferenceQueue<>();

    public FileSystem(String rootLabel, Mount rootMount) throws FileSystemException {
        mount(rootLabel, "", rootMount);
    }

    public FileSystem(String rootLabel, WritableMount rootMount) throws FileSystemException {
        mountWritable(rootLabel, "", rootMount);
    }

    public void close() {
        // Close all dangling open files
        synchronized (openFiles) {
            for (Closeable file : openFiles.values()) IoUtil.closeQuietly(file);
            openFiles.clear();
            while (openFileQueue.poll() != null) ;
        }
    }

    public synchronized void mount(String label, String location, Mount mount) throws FileSystemException {
        Objects.requireNonNull(mount, "mount cannot be null");
        location = sanitizePath(location);
        if (location.contains("..")) throw new FileSystemException("Cannot mount below the root");
        mount(new MountWrapper(label, location, mount));
    }

    public synchronized void mountWritable(String label, String location, WritableMount mount) throws FileSystemException {
        Objects.requireNonNull(mount, "mount cannot be null");

        location = sanitizePath(location);
        if (location.contains("..")) {
            throw new FileSystemException("Cannot mount below the root");
        }
        mount(new MountWrapper(label, location, mount));
    }

    private synchronized void mount(MountWrapper wrapper) {
        var location = wrapper.getLocation();
        mounts.remove(location);
        mounts.put(location, wrapper);
    }

    public synchronized void unmount(String path) {
        var mount = mounts.remove(sanitizePath(path));
        if (mount == null) return;

        cleanup();

        // Close any files which belong to this mount - don't want people writing to a disk after it's been ejected!
        // There's no point storing a Mount -> Wrapper[] map, as openFiles is small and unmount isn't called very
        // often.
        synchronized (openFiles) {
            for (var iterator = openFiles.keySet().iterator(); iterator.hasNext(); ) {
                var reference = iterator.next();
                var wrapper = reference.get();
                if (wrapper == null) continue;

                if (wrapper.mount == mount) {
                    wrapper.closeExternally();
                    iterator.remove();
                }
            }
        }
    }

    public String combine(String path, String childPath) {
        path = sanitizePath(path, true);
        childPath = sanitizePath(childPath, true);

        if (path.isEmpty()) {
            return childPath;
        } else if (childPath.isEmpty()) {
            return path;
        } else {
            return sanitizePath(path + '/' + childPath, true);
        }
    }

    public static String getDirectory(String path) {
        path = sanitizePath(path, true);
        if (path.isEmpty()) {
            return "..";
        }

        var lastSlash = path.lastIndexOf('/');

        // If the trailing segment is a "..", then just append another one.
        if (path.substring(lastSlash < 0 ? 0 : lastSlash + 1).equals("..")) return path + "/..";

        if (lastSlash >= 0) {
            return path.substring(0, lastSlash);
        } else {
            return "";
        }
    }

    public static String getName(String path) {
        path = sanitizePath(path, true);
        if (path.isEmpty()) return "root";

        var lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    public synchronized long getSize(String path) throws FileSystemException {
        return getMount(sanitizePath(path)).getSize(sanitizePath(path));
    }

    public synchronized BasicFileAttributes getAttributes(String path) throws FileSystemException {
        return getMount(sanitizePath(path)).getAttributes(sanitizePath(path));
    }

    public synchronized List<String> list(String path) throws FileSystemException {
        path = sanitizePath(path);
        var mount = getMount(path);

        // Gets a list of the files in the mount
        List<String> list = new ArrayList<>();
        mount.list(path, list);

        // Add any mounts that are mounted at this location
        for (var otherMount : mounts.values()) {
            if (getDirectory(otherMount.getLocation()).equals(path)) {
                list.add(getName(otherMount.getLocation()));
            }
        }

        // Return list
        list.sort(Comparator.naturalOrder());
        return list;
    }

    public synchronized boolean exists(String path) throws FileSystemException {
        path = sanitizePath(path);
        var mount = getMount(path);
        return mount.exists(path);
    }

    public synchronized boolean isDir(String path) throws FileSystemException {
        path = sanitizePath(path);
        var mount = getMount(path);
        return mount.isDirectory(path);
    }

    public synchronized boolean isReadOnly(String path) throws FileSystemException {
        path = sanitizePath(path);
        var mount = getMount(path);
        return mount.isReadOnly(path);
    }

    public synchronized String getMountLabel(String path) throws FileSystemException {
        path = sanitizePath(path);
        var mount = getMount(path);
        return mount.getLabel();
    }

    public synchronized void makeDir(String path) throws FileSystemException {
        path = sanitizePath(path);
        var mount = getMount(path);
        mount.makeDirectory(path);
    }

    public synchronized void delete(String path) throws FileSystemException {
        path = sanitizePath(path);
        var mount = getMount(path);
        mount.delete(path);
    }

    public synchronized void move(String sourcePath, String destPath) throws FileSystemException {
        sourcePath = sanitizePath(sourcePath);
        destPath = sanitizePath(destPath);

        if (isReadOnly(sourcePath) || isReadOnly(destPath)) throw new FileSystemException(ACCESS_DENIED);
        if (!exists(sourcePath)) throw new FileSystemException(NO_SUCH_FILE);
        if (exists(destPath)) throw new FileSystemException(FILE_EXISTS);
        if (contains(sourcePath, destPath)) throw new FileSystemException("Can't move a directory inside itself");

        var mount = getMount(sourcePath);
        if (mount == getMount(destPath)) {
            mount.rename(sourcePath, destPath);
        } else {
            copy(sourcePath, destPath);
            delete(sourcePath);
        }
    }

    public synchronized void copy(String sourcePath, String destPath) throws FileSystemException {
        sourcePath = sanitizePath(sourcePath);
        destPath = sanitizePath(destPath);
        if (isReadOnly(destPath)) throw new FileSystemException(destPath, ACCESS_DENIED);
        if (!exists(sourcePath)) throw new FileSystemException(sourcePath, NO_SUCH_FILE);
        if (exists(destPath)) throw new FileSystemException(destPath, FILE_EXISTS);
        if (contains(sourcePath, destPath)) {
            throw new FileSystemException(sourcePath, "Can't copy a directory inside itself");
        }
        copyRecursive(sourcePath, getMount(sourcePath), destPath, getMount(destPath), 0);
    }

    private synchronized void copyRecursive(String sourcePath, MountWrapper sourceMount, String destinationPath, MountWrapper destinationMount, int depth) throws FileSystemException {
        if (!sourceMount.exists(sourcePath)) return;
        if (depth >= MAX_COPY_DEPTH) throw new FileSystemException("Too many directories to copy");

        if (sourceMount.isDirectory(sourcePath)) {
            // Copy a directory:
            // Make the new directory
            destinationMount.makeDirectory(destinationPath);

            // Copy the source contents into it
            List<String> sourceChildren = new ArrayList<>();
            sourceMount.list(sourcePath, sourceChildren);
            for (var child : sourceChildren) {
                copyRecursive(
                    combine(sourcePath, child), sourceMount,
                    combine(destinationPath, child), destinationMount,
                    depth + 1
                );
            }
        } else {
            // Copy a file:
            try (var source = sourceMount.openForRead(sourcePath);
                 var destination = destinationMount.openForWrite(destinationPath, WRITE_OPTIONS)) {
                // Copy bytes as fast as we can
                ByteStreams.copy(source, destination);
            } catch (AccessDeniedException e) {
                throw new FileSystemException(ACCESS_DENIED);
            } catch (IOException e) {
                throw FileSystemException.of(e);
            }
        }
    }

    private void cleanup() {
        synchronized (openFiles) {
            Reference<?> ref;
            while ((ref = openFileQueue.poll()) != null) {
                IoUtil.closeQuietly(openFiles.remove(ref));
            }
        }
    }

    private synchronized FileSystemWrapper<SeekableByteChannel> openFile(MountWrapper mount, SeekableByteChannel channel) throws FileSystemException {
        synchronized (openFiles) {
            if (CoreConfig.maximumFilesOpen > 0 &&
                openFiles.size() >= CoreConfig.maximumFilesOpen) {
                IoUtil.closeQuietly(channel);
                throw new FileSystemException("Too many files already open");
            }

            var fsWrapper = new FileSystemWrapper<>(this, mount, channel, openFileQueue);
            openFiles.put(fsWrapper.self, channel);
            return fsWrapper;
        }
    }

    void removeFile(FileSystemWrapper<?> handle) {
        synchronized (openFiles) {
            openFiles.remove(handle.self);
        }
    }

    public synchronized FileSystemWrapper<SeekableByteChannel> openForRead(String path) throws FileSystemException {
        cleanup();

        path = sanitizePath(path);
        var mount = getMount(path);
        var channel = mount.openForRead(path);
        return openFile(mount, channel);
    }

    public synchronized FileSystemWrapper<SeekableByteChannel> openForWrite(String path, Set<OpenOption> options) throws FileSystemException {
        cleanup();

        path = sanitizePath(path);
        var mount = getMount(path);
        var channel = mount.openForWrite(path, options);
        return openFile(mount, channel);
    }

    public synchronized long getFreeSpace(String path) throws FileSystemException {
        path = sanitizePath(path);
        var mount = getMount(path);
        return mount.getFreeSpace();
    }

    public synchronized OptionalLong getCapacity(String path) throws FileSystemException {
        path = sanitizePath(path);
        var mount = getMount(path);
        return mount.getCapacity();
    }

    private synchronized MountWrapper getMount(String path) throws FileSystemException {
        // Return the deepest mount that contains a given path
        var it = mounts.values().iterator();
        MountWrapper match = null;
        var matchLength = 999;
        while (it.hasNext()) {
            var mount = it.next();
            if (contains(mount.getLocation(), path)) {
                var len = toLocal(path, mount.getLocation()).length();
                if (match == null || len < matchLength) {
                    match = mount;
                    matchLength = len;
                }
            }
        }
        if (match == null) {
            throw new FileSystemException(path, "Invalid Path");
        }
        return match;
    }

    private static String sanitizePath(String path) {
        return sanitizePath(path, false);
    }

    private static final Pattern threeDotsPattern = Pattern.compile("^\\.{3,}$");

    // IMPORTANT: Both arrays are sorted by ASCII value.
    private static final char[] specialChars = new char[]{ '"', '*', ':', '<', '>', '?', '|' };
    private static final char[] specialCharsAllowWildcards = new char[]{ '"', ':', '<', '>', '|' };

    public static String sanitizePath(String path, boolean allowWildcards) {
        // Allow windowsy slashes
        path = path.replace('\\', '/');

        // Clean the path or illegal characters.
        var cleanName = new StringBuilder();
        var allowedChars = allowWildcards ? specialCharsAllowWildcards : specialChars;
        for (var i = 0; i < path.length(); i++) {
            var c = path.charAt(i);
            if (c >= 32 && Arrays.binarySearch(allowedChars, c) < 0) cleanName.append(c);
        }
        path = cleanName.toString();

        // Collapse the string into its component parts, removing ..'s
        var outputParts = new ArrayDeque<String>();
        for (var fullPart : Splitter.on('/').split(path)) {
            var part = fullPart.strip();

            if (part.isEmpty() || part.equals(".") || threeDotsPattern.matcher(part).matches()) {
                // . is redundant
                // ... and more are treated as .
                continue;
            }

            if (part.equals("..")) {
                // .. can cancel out the last folder entered
                if (!outputParts.isEmpty()) {
                    var top = outputParts.peekLast();
                    if (!top.equals("..")) {
                        outputParts.removeLast();
                    } else {
                        outputParts.addLast("..");
                    }
                } else {
                    outputParts.addLast("..");
                }
            } else if (part.length() >= 255) {
                // If part length > 255 and it is the last part
                outputParts.addLast(part.substring(0, 255).strip());
            } else {
                // Anything else we add to the stack
                outputParts.addLast(part);
            }
        }

        return String.join("/", outputParts);
    }

    private static boolean contains(String pathA, String pathB) {
        pathA = sanitizePath(pathA).toLowerCase(Locale.ROOT);
        pathB = sanitizePath(pathB).toLowerCase(Locale.ROOT);

        if (pathB.equals("..")) {
            return false;
        } else if (pathB.startsWith("../")) {
            return false;
        } else if (pathB.equals(pathA)) {
            return true;
        } else if (pathA.isEmpty()) {
            return true;
        } else {
            return pathB.startsWith(pathA + "/");
        }
    }

    static String toLocal(String path, String location) {
        path = sanitizePath(path);
        location = sanitizePath(location);

        assert contains(location, path);
        var local = path.substring(location.length());
        if (local.startsWith("/")) {
            return local.substring(1);
        } else {
            return local;
        }
    }
}
