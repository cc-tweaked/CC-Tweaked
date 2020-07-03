---  The FS API allows you to manipulate files and the filesystem.
--
-- @module fs

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
