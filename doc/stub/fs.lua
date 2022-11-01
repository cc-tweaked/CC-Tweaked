--- @module fs

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

@tparam[1] string path The path to complete.
@tparam[1] string location The location where paths are resolved from.
@tparam[1,opt=true] boolean include_files When @{false}, only directories will
be included in the returned list.
@tparam[1,opt=true] boolean include_dirs When @{false}, "raw" directories will
not be included in the returned list.

@tparam[2] string path The path to complete.
@tparam[2] string location The location where paths are resolved from.
@tparam[2] {
    include_dirs? = boolean, include_files? = boolean,
    include_hidden? = boolean
} options
This table form is an expanded version of the previous syntax. The
`include_files` and `include_dirs` arguments from above are passed in as fields.

This table also accepts the following options:
 - `include_hidden`: Whether to include hidden files (those starting with `.`)
   by default. They will still be shown when typing a `.`.

@treturn { string... } A list of possible completion candidates.
@since 1.74
@changed 1.101.0
@usage Complete files in the root directory.

    read(nil, nil, function(str)
        return fs.complete(str, "", true, false)
    end)

@usage Complete files in the root directory, hiding hidden files by default.

    read(nil, nil, function(str)
        return fs.complete(str, "", {
            include_files = true,
            include_dirs = false,
            included_hidden = false,
        })
    end)
]]
function complete(path, location, include_files, include_dirs) end
