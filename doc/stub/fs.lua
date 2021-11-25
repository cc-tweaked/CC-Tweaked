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
-- @since 1.87.0
function isDriveRoot(path) end

--[[- Provides completion for a file or directory name, suitable for use with
@{_G.read}.

When a directory is a possible candidate for completion, two entries are
included - one with a trailing slash (indicating that entries within this
directory exist) and one without it (meaning this entry is an immediate
completion candidate). `include_dirs` can be set to @{false} to only include
those with a trailing slash.

@tparam string path The path to complete.
@tparam string location The location where paths are resolved from.
@tparam[opt] boolean include_files When @{false}, only directories will be
included in the returned list.
@tparam[opt] boolean include_dirs When @{false}, "raw" directories will not be
included in the returned list.
@treturn { string... } A list of possible completion candidates.
@since 1.74
]]
function complete(path, location, include_files, include_dirs) end
