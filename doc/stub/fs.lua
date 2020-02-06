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
-- @type ReadableHandle
-- @see fs.open
local ReadableHandle = {}
function ReadableHandle.read(count) end
function ReadableHandle.readAll() end
function ReadableHandle.readLine(with_trailing) end
function ReadableHandle.seek(whence, offset) end
function ReadableHandle.close() end

--- A file handle which can be written to.
--
-- @type WritableHandle
-- @see fs.open
local WritableHandle = {}
function WritableHandle.write(text) end
function WritableHandle.writeLine(text) end
function WritableHandle.flush(text) end
function WritableHandle.seek(whence, offset) end
function WritableHandle.close() end
