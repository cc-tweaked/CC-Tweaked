---  The FS API allows you to manipulate files and the filesystem.
--
-- @module fs

function list(path) end
function combine(base, child) end
function getName(path) end
function getSize(path) end
function exists(path) end
function isDir(path) end
function isReadOnly(path) end
function makeDir(path) end
function move(from, to) end
function copy(from, to) end
function delete(path) end
function open(path, mode) end
function getDrive(path) end
function getFreeSpace(path) end
function find(pattern) end
function getDir(path) end

--- Returns true if a path is mounted to the parent filesystem.
--
-- The root filesystem "/" is considered a mount, along with disk folders and
-- the rom folder. Other programs (such as network shares) can exstend this to
-- make other mount types by correctly assigning their return value for getDrive.
--
-- @tparam string path The path to check.
-- @treturn boolean If the path is mounted, rather than a normal file/folder.
-- @throws If the path does not exist.
-- @see getDrive
function isDriveRoot(path) end

--- Get the capacity of the drive at the given path.
--
-- This may be used in conjunction with @{getFreeSpace} to determine what
-- percentage of this drive has been used.
--
-- @tparam string path The path of the drive to get.
-- @treturn number This drive's capacity. This will be 0 for "read-only" drives,
-- such as the ROM or treasure disks.
function getCapacity(path) end

--- Get attributes about a specific file or folder.
--
-- The returned attributes table contains information about the size of the
-- file, whether it is a directory, and when it was created and last modified.
--
-- The creation and modification times are given as the number of milliseconds
-- since the UNIX epoch. This may be given to @{os.date} in order to convert it
-- to more usable form.
--
-- @tparam string path The path to get attributes for.
-- @treturn { size = number, isDir = boolean, created = number, modified = number }
-- The resulting attributes.
-- @throws If the path does not exist.
-- @see getSize If you only care about the file's size.
-- @see isDir If you only care whether a path is a directory or not.
function attributes(path) end

-- Defined in bios.lua
function complete(sPath, sLocation, bIncludeFiles, bIncludeDirs) end

--- A file handle which can be read from.
--
-- @type ReadHandle
-- @see fs.open
local ReadHandle = {}
function ReadHandle.read(count) end
function ReadHandle.readAll() end
function ReadHandle.readLine(with_trailing) end
function ReadHandle.seek(whence, offset) end
function ReadHandle.close() end

--- A file handle which can be written to.
--
-- @type WriteHandle
-- @see fs.open
local WriteHandle = {}
function WriteHandle.write(text) end
function WriteHandle.writeLine(text) end
function WriteHandle.flush(text) end
function WriteHandle.seek(whence, offset) end
function WriteHandle.close() end
