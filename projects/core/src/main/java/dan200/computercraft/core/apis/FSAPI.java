// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.apis;

import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.core.apis.handles.ReadHandle;
import dan200.computercraft.core.apis.handles.ReadWriteHandle;
import dan200.computercraft.core.apis.handles.WriteHandle;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.metrics.Metrics;

import javax.annotation.Nullable;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Interact with the computer's files and filesystem, allowing you to manipulate files, directories and paths. This
 * includes:
 *
 * <ul>
 * <li>**Reading and writing files:** Call {@link #open} to obtain a file "handle", which can be used to read from or
 * write to a file.</li>
 * <li>**Path manipulation:** {@link #combine}, {@link #getName} and {@link #getDir} allow you to manipulate file
 * paths, joining them together or extracting components.</li>
 * <li>**Querying paths:** For instance, checking if a file exists, or whether it's a directory. See {@link #getSize},
 * {@link #exists}, {@link #isDir}, {@link #isReadOnly} and {@link #attributes}.</li>
 * <li>**File and directory manipulation:** For instance, moving or copying files. See {@link #makeDir}, {@link #move},
 * {@link #copy} and {@link #delete}.</li>
 * </ul>
 * <p>
 * > [!NOTE]
 * > All functions in the API work on absolute paths, and do not take the [current directory][`shell.dir`] into account.
 * > You can use [`shell.resolve`] to convert a relative path into an absolute one.
 * <p>
 * ## Mounts
 * While a computer can only have one hard drive and filesystem, other filesystems may be "mounted" inside it. For
 * instance, the {@link dan200.computercraft.shared.peripheral.diskdrive.DiskDrivePeripheral drive peripheral} mounts
 * its disk's contents at {@code "disk/"}, {@code "disk1/"}, etc...
 * <p>
 * You can see which mount a path belongs to with the {@link #getDrive} function. This returns {@code "hdd"} for the
 * computer's main filesystem ({@code "/"}), {@code "rom"} for the rom ({@code "rom/"}).
 * <p>
 * Most filesystems have a limited capacity, operations which would cause that capacity to be reached (such as writing
 * an incredibly large file) will fail. You can see a mount's capacity with {@link #getCapacity} and the remaining
 * space with {@link #getFreeSpace}.
 *
 * @cc.module fs
 */
public class FSAPI implements ILuaAPI {
    private static final Set<OpenOption> READ_EXTENDED = Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE);
    private static final Set<OpenOption> WRITE_EXTENDED = union(Set.of(StandardOpenOption.READ), MountConstants.WRITE_OPTIONS);

    private final IAPIEnvironment environment;
    private @Nullable FileSystem fileSystem = null;

    public FSAPI(IAPIEnvironment env) {
        environment = env;
    }

    @Override
    public String[] getNames() {
        return new String[]{ "fs" };
    }

    @Override
    public void startup() {
        fileSystem = environment.getFileSystem();
    }

    @Override
    public void shutdown() {
        fileSystem = null;
    }

    private FileSystem getFileSystem() {
        var filesystem = fileSystem;
        if (filesystem == null) throw new IllegalStateException("File system is not mounted");
        return filesystem;
    }

    /**
     * Returns a list of files in a directory.
     *
     * @param path The path to list.
     * @return A table with a list of files in the directory.
     * @throws LuaException If the path doesn't exist.
     * @cc.usage List all files under {@code /rom/}
     * <pre>{@code
     * local files = fs.list("/rom/")
     * for i = 1, #files do
     *   print(files[i])
     * end
     * }</pre>
     */
    @LuaFunction
    public final List<String> list(String path) throws LuaException {
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            return getFileSystem().list(path);
        } catch (FileSystemException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Combines several parts of a path into one full path, adding separators as
     * needed.
     *
     * @param arguments The paths to combine.
     * @return The new path, with separators added between parts as needed.
     * @throws LuaException On argument errors.
     * @cc.tparam string path The first part of the path. For example, a parent directory path.
     * @cc.tparam string ... Additional parts of the path to combine.
     * @cc.changed 1.95.0 Now supports multiple arguments.
     * @cc.usage Combine several file paths together
     * <pre>{@code
     * fs.combine("/rom/programs", "../apis", "parallel.lua")
     * -- => rom/apis/parallel.lua
     * }</pre>
     */
    @LuaFunction
    public final String combine(IArguments arguments) throws LuaException {
        var result = new StringBuilder();
        result.append(FileSystem.sanitizePath(arguments.getString(0), true));

        for (int i = 1, n = arguments.count(); i < n; i++) {
            var part = FileSystem.sanitizePath(arguments.getString(i), true);
            if (result.length() != 0 && !part.isEmpty()) result.append('/');
            result.append(part);
        }

        return FileSystem.sanitizePath(result.toString(), true);
    }

    /**
     * Returns the file name portion of a path.
     *
     * @param path The path to get the name from.
     * @return The final part of the path (the file name).
     * @cc.since 1.2
     * @cc.usage Get the file name of {@code rom/startup.lua}
     * <pre>{@code
     * fs.getName("rom/startup.lua")
     * -- => startup.lua
     * }</pre>
     */
    @LuaFunction
    public final String getName(String path) {
        return FileSystem.getName(path);
    }

    /**
     * Returns the parent directory portion of a path.
     *
     * @param path The path to get the directory from.
     * @return The path with the final part removed (the parent directory).
     * @cc.since 1.63
     * @cc.usage Get the directory name of {@code rom/startup.lua}
     * <pre>{@code
     * fs.getDir("rom/startup.lua")
     * -- => rom
     * }</pre>
     */
    @LuaFunction
    public final String getDir(String path) {
        return FileSystem.getDirectory(path);
    }

    /**
     * Returns the size of the specified file.
     *
     * @param path The file to get the file size of.
     * @return The size of the file, in bytes.
     * @throws LuaException If the path doesn't exist.
     * @cc.since 1.3
     */
    @LuaFunction
    public final long getSize(String path) throws LuaException {
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            return getFileSystem().getSize(path);
        } catch (FileSystemException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Returns whether the specified path exists.
     *
     * @param path The path to check the existence of.
     * @return Whether the path exists.
     */
    @LuaFunction
    public final boolean exists(String path) {
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            return getFileSystem().exists(path);
        } catch (FileSystemException e) {
            return false;
        }
    }

    /**
     * Returns whether the specified path is a directory.
     *
     * @param path The path to check.
     * @return Whether the path is a directory.
     */
    @LuaFunction
    public final boolean isDir(String path) {
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            return getFileSystem().isDir(path);
        } catch (FileSystemException e) {
            return false;
        }
    }

    /**
     * Returns whether a path is read-only.
     *
     * @param path The path to check.
     * @return Whether the path cannot be written to.
     */
    @LuaFunction
    public final boolean isReadOnly(String path) {
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            return getFileSystem().isReadOnly(path);
        } catch (FileSystemException e) {
            return false;
        }
    }

    /**
     * Creates a directory, and any missing parents, at the specified path.
     *
     * @param path The path to the directory to create.
     * @throws LuaException If the directory couldn't be created.
     */
    @LuaFunction
    public final void makeDir(String path) throws LuaException {
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            getFileSystem().makeDir(path);
        } catch (FileSystemException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Moves a file or directory from one path to another.
     * <p>
     * Any parent directories are created as needed.
     *
     * @param path The current file or directory to move from.
     * @param dest The destination path for the file or directory.
     * @throws LuaException If the file or directory couldn't be moved.
     */
    @LuaFunction
    public final void move(String path, String dest) throws LuaException {
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            getFileSystem().move(path, dest);
        } catch (FileSystemException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Copies a file or directory to a new path.
     * <p>
     * Any parent directories are created as needed.
     *
     * @param path The file or directory to copy.
     * @param dest The path to the destination file or directory.
     * @throws LuaException If the file or directory couldn't be copied.
     */
    @LuaFunction
    public final void copy(String path, String dest) throws LuaException {
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            getFileSystem().copy(path, dest);
        } catch (FileSystemException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Deletes a file or directory.
     * <p>
     * If the path points to a directory, all of the enclosed files and
     * subdirectories are also deleted.
     *
     * @param path The path to the file or directory to delete.
     * @throws LuaException If the file or directory couldn't be deleted.
     */
    @LuaFunction
    public final void delete(String path) throws LuaException {
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            getFileSystem().delete(path);
        } catch (FileSystemException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Opens a file for reading or writing at a path.
     * <p>
     * The {@code mode} string can be any of the following:
     * <ul>
     * <li><strong>"r"</strong>: Read mode.</li>
     * <li><strong>"w"</strong>: Write mode.</li>
     * <li><strong>"a"</strong>: Append mode.</li>
     * <li><strong>"r+"</strong>: Update mode (allows reading and writing), all data is preserved.</li>
     * <li><strong>"w+"</strong>: Update mode, all data is erased.</li>
     * </ul>
     * <p>
     * The mode may also have a "b" at the end, which opens the file in "binary
     * mode". This changes {@link ReadHandle#read(Optional)} and {@link WriteHandle#write(IArguments)}
     * to read/write single bytes as numbers rather than strings.
     *
     * @param path The path to the file to open.
     * @param mode The mode to open the file with.
     * @return A file handle object for the file, or {@code nil} + an error message on error.
     * @throws LuaException If an invalid mode was specified.
     * @cc.treturn [1] table A file handle object for the file.
     * @cc.treturn [2] nil If the file does not exist, or cannot be opened.
     * @cc.treturn string|nil A message explaining why the file cannot be opened.
     * @cc.usage Read the contents of a file.
     * <pre>{@code
     * local file = fs.open("/rom/help/intro.txt", "r")
     * local contents = file.readAll()
     * file.close()
     *
     * print(contents)
     * }</pre>
     * @cc.usage Open a file and read all lines into a table. [`io.lines`] offers an alternative way to do this.
     * <pre>{@code
     * local file = fs.open("/rom/motd.txt", "r")
     * local lines = {}
     * while true do
     *   local line = file.readLine()
     *
     *   -- If line is nil then we've reached the end of the file and should stop
     *   if not line then break end
     *
     *   lines[#lines + 1] = line
     * end
     *
     * file.close()
     *
     * print(lines[math.random(#lines)]) -- Pick a random line and print it.
     * }</pre>
     * @cc.usage Open a file and write some text to it. You can run {@code edit out.txt} to see the written text.
     * <pre>{@code
     * local file = fs.open("out.txt", "w")
     * file.write("Just testing some code")
     * file.close() -- Remember to call close, otherwise changes may not be written!
     * }</pre>
     * @cc.changed 1.109.0 Add support for update modes ({@code r+} and {@code w+}).
     * @cc.changed 1.109.0 Opening a file in non-binary mode now uses the raw bytes of the file rather than encoding to
     * UTF-8.
     */
    @LuaFunction
    public final Object[] open(String path, String mode) throws LuaException {
        if (mode.isEmpty()) throw new LuaException(MountConstants.UNSUPPORTED_MODE);

        var binary = mode.indexOf('b') >= 0;
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            switch (mode) {
                case "r", "rb" -> {
                    var reader = getFileSystem().openForRead(path);
                    return new Object[]{ new ReadHandle(reader.get(), reader, binary) };
                }
                case "w", "wb" -> {
                    var writer = getFileSystem().openForWrite(path, MountConstants.WRITE_OPTIONS);
                    return new Object[]{ WriteHandle.of(writer.get(), writer, binary, true) };
                }
                case "a", "ab" -> {
                    var writer = getFileSystem().openForWrite(path, MountConstants.APPEND_OPTIONS);
                    return new Object[]{ WriteHandle.of(writer.get(), writer, binary, false) };
                }
                case "r+", "r+b" -> {
                    var reader = getFileSystem().openForWrite(path, READ_EXTENDED);
                    return new Object[]{ new ReadWriteHandle(reader.get(), reader, binary) };
                }
                case "w+", "w+b" -> {
                    var writer = getFileSystem().openForWrite(path, WRITE_EXTENDED);
                    return new Object[]{ new ReadWriteHandle(writer.get(), writer, binary) };
                }
                default -> throw new LuaException(MountConstants.UNSUPPORTED_MODE);
            }
        } catch (FileSystemException e) {
            return new Object[]{ null, e.getMessage() };
        }
    }

    /**
     * Returns the name of the mount that the specified path is located on.
     *
     * @param path The path to get the drive of.
     * @return The name of the drive that the file is on; e.g. {@code hdd} for local files, or {@code rom} for ROM files.
     * @throws LuaException If the path doesn't exist.
     * @cc.treturn string|nil The name of the drive that the file is on; e.g. {@code hdd} for local files, or {@code rom} for ROM files.
     * @cc.usage Print the drives of a couple of mounts:
     *
     * <pre>{@code
     * print("/: " .. fs.getDrive("/"))
     * print("/rom/: " .. fs.getDrive("rom"))
     * }</pre>
     */
    @Nullable
    @LuaFunction
    public final Object[] getDrive(String path) throws LuaException {
        try {
            return getFileSystem().exists(path) ? new Object[]{ getFileSystem().getMountLabel(path) } : null;
        } catch (FileSystemException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Returns the amount of free space available on the drive the path is
     * located on.
     *
     * @param path The path to check the free space for.
     * @return The amount of free space available, in bytes.
     * @throws LuaException If the path doesn't exist.
     * @cc.treturn number|"unlimited" The amount of free space available, in bytes, or "unlimited".
     * @cc.since 1.4
     * @see #getCapacity To get the capacity of this drive.
     */
    @LuaFunction
    public final Object getFreeSpace(String path) throws LuaException {
        try {
            var freeSpace = getFileSystem().getFreeSpace(path);
            return freeSpace >= 0 ? freeSpace : "unlimited";
        } catch (FileSystemException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Returns the capacity of the drive the path is located on.
     *
     * @param path The path of the drive to get.
     * @return The drive's capacity.
     * @throws LuaException If the capacity cannot be determined.
     * @cc.treturn number|nil This drive's capacity. This will be nil for "read-only" drives, such as the ROM or
     * treasure disks.
     * @cc.since 1.87.0
     * @see #getFreeSpace To get the free space available on this drive.
     */
    @Nullable
    @LuaFunction
    public final Object getCapacity(String path) throws LuaException {
        try {
            var capacity = getFileSystem().getCapacity(path);
            return capacity.isPresent() ? capacity.getAsLong() : null;
        } catch (FileSystemException e) {
            throw new LuaException(e.getMessage());
        }
    }

    /**
     * Get attributes about a specific file or folder.
     * <p>
     * The returned attributes table contains information about the size of the file, whether it is a directory,
     * when it was created and last modified, and whether it is read only.
     * <p>
     * The creation and modification times are given as the number of milliseconds since the UNIX epoch. This may be
     * given to {@link OSAPI#date} in order to convert it to more usable form.
     *
     * @param path The path to get attributes for.
     * @return The resulting attributes.
     * @throws LuaException If the path does not exist.
     * @cc.treturn { size = number, isDir = boolean, isReadOnly = boolean, created = number, modified = number } The resulting attributes.
     * @cc.since 1.87.0
     * @cc.changed 1.91.0 Renamed `modification` field to `modified`.
     * @cc.changed 1.95.2 Added `isReadOnly` to attributes.
     * @see #getSize If you only care about the file's size.
     * @see #isDir If you only care whether a path is a directory or not.
     */
    @LuaFunction
    public final Map<String, Object> attributes(String path) throws LuaException {
        try (var ignored = environment.time(Metrics.FS_OPS)) {
            var attributes = getFileSystem().getAttributes(path);
            Map<String, Object> result = new HashMap<>();
            result.put("modification", attributes.lastModifiedTime().toMillis());
            result.put("modified", attributes.lastModifiedTime().toMillis());
            result.put("created", attributes.creationTime().toMillis());
            result.put("size", attributes.isDirectory() ? 0 : attributes.size());
            result.put("isDir", attributes.isDirectory());
            result.put("isReadOnly", getFileSystem().isReadOnly(path));
            return result;
        } catch (FileSystemException e) {
            throw new LuaException(e.getMessage());
        }
    }

    private static Set<OpenOption> union(Set<OpenOption> a, Set<OpenOption> b) {
        Set<OpenOption> union = new HashSet<>();
        union.addAll(a);
        union.addAll(b);
        return Set.copyOf(union);
    }
}
