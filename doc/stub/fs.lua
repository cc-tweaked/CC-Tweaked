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
