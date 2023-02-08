--- @module fs

local expect = dofile("rom/modules/main/cc/expect.lua")
local expect, field = expect.expect, expect.field

local native = fs

local fs = _ENV
for k, v in pairs(native) do fs[k] = v end

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
            include_hidden = false,
        })
    end)
]]
function fs.complete(sPath, sLocation, bIncludeFiles, bIncludeDirs)
    expect(1, sPath, "string")
    expect(2, sLocation, "string")
    local bIncludeHidden = nil
    if type(bIncludeFiles) == "table" then
        bIncludeDirs = field(bIncludeFiles, "include_dirs", "boolean", "nil")
        bIncludeHidden = field(bIncludeFiles, "include_hidden", "boolean", "nil")
        bIncludeFiles = field(bIncludeFiles, "include_files", "boolean", "nil")
    else
        expect(3, bIncludeFiles, "boolean", "nil")
        expect(4, bIncludeDirs, "boolean", "nil")
    end

    bIncludeHidden = bIncludeHidden ~= false
    bIncludeFiles = bIncludeFiles ~= false
    bIncludeDirs = bIncludeDirs ~= false
    local sDir = sLocation
    local nStart = 1
    local nSlash = string.find(sPath, "[/\\]", nStart)
    if nSlash == 1 then
        sDir = ""
        nStart = 2
    end
    local sName
    while not sName do
        local nSlash = string.find(sPath, "[/\\]", nStart)
        if nSlash then
            local sPart = string.sub(sPath, nStart, nSlash - 1)
            sDir = fs.combine(sDir, sPart)
            nStart = nSlash + 1
        else
            sName = string.sub(sPath, nStart)
        end
    end

    if fs.isDir(sDir) then
        local tResults = {}
        if bIncludeDirs and sPath == "" then
            table.insert(tResults, ".")
        end
        if sDir ~= "" then
            if sPath == "" then
                table.insert(tResults, bIncludeDirs and ".." or "../")
            elseif sPath == "." then
                table.insert(tResults, bIncludeDirs and "." or "./")
            end
        end
        local tFiles = fs.list(sDir)
        for n = 1, #tFiles do
            local sFile = tFiles[n]
            if #sFile >= #sName and string.sub(sFile, 1, #sName) == sName and (
                bIncludeHidden or sFile:sub(1, 1) ~= "." or sName:sub(1, 1) == "."
            ) then
                local bIsDir = fs.isDir(fs.combine(sDir, sFile))
                local sResult = string.sub(sFile, #sName + 1)
                if bIsDir then
                    table.insert(tResults, sResult .. "/")
                    if bIncludeDirs and #sResult > 0 then
                        table.insert(tResults, sResult)
                    end
                else
                    if bIncludeFiles and #sResult > 0 then
                        table.insert(tResults, sResult)
                    end
                end
            end
        end
        return tResults
    end

    return {}
end

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
function fs.isDriveRoot(sPath)
    expect(1, sPath, "string")
    -- Force the root directory to be a mount.
    return fs.getDir(sPath) == ".." or fs.getDrive(sPath) ~= fs.getDrive(fs.getDir(sPath))
end
