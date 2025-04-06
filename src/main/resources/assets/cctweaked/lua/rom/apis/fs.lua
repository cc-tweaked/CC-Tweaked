-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--- @module fs

local expect = dofile("rom/modules/main/cc/expect.lua")
local expect, field = expect.expect, expect.field

local native = fs

local fs = _ENV
for k, v in pairs(native) do fs[k] = v end

--[[- Provides completion for a file or directory name, suitable for use with
[`_G.read`].

When a directory is a possible candidate for completion, two entries are
included - one with a trailing slash (indicating that entries within this
directory exist) and one without it (meaning this entry is an immediate
completion candidate). `include_dirs` can be set to [`false`] to only include
those with a trailing slash.

@tparam[1] string path The path to complete.
@tparam[1] string location The location where paths are resolved from.
@tparam[1,opt=true] boolean include_files When [`false`], only directories will
be included in the returned list.
@tparam[1,opt=true] boolean include_dirs When [`false`], "raw" directories will
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

local function find_aux(path, parts, i, out)
    local part = parts[i]
    if not part then
        -- If we're at the end of the pattern, ensure our path exists and append it.
        if fs.exists(path) then out[#out + 1] = path end
    elseif part.exact then
        -- If we're an exact match, just recurse into this directory.
        return find_aux(fs.combine(path, part.contents), parts, i + 1, out)
    else
        -- Otherwise we're a pattern. Check we're a directory, then recurse into each
        -- matching file.
        if not fs.isDir(path) then return end

        local files = fs.list(path)
        for j = 1, #files do
            local file = files[j]
            if file:find(part.contents) then find_aux(fs.combine(path, file), parts, i + 1, out) end
        end
    end
end

local find_escape = {
    -- Escape standard Lua pattern characters
    ["^"] = "%^", ["$"] = "%$", ["("] = "%(", [")"] = "%)", ["%"] = "%%",
    ["."] = "%.", ["["] = "%[", ["]"] = "%]", ["+"] = "%+", ["-"] = "%-",
    -- Aside from our wildcards.
    ["*"] = ".*",
    ["?"] = ".",
}

--[[- Searches for files matching a string with wildcards.

This string looks like a normal path string, but can include wildcards, which
can match multiple paths:

 - "?" matches any single character in a file name.
 - "*" matches any number of characters.

For example, `rom/*/command*` will look for any path starting with `command`
inside any subdirectory of `/rom`.

Note that these wildcards match a single segment of the path. For instance
`rom/*.lua` will include `rom/startup.lua` but _not_ include `rom/programs/list.lua`.

@tparam string path The wildcard-qualified path to search for.
@treturn { string... } A list of paths that match the search string.
@throws If the supplied path was invalid.
@since 1.6
@changed 1.106.0 Added support for the `?` wildcard.

@usage List all Markdown files in the help folder

    fs.find("rom/help/*.md")
]]
function fs.find(pattern)
    expect(1, pattern, "string")

    pattern = fs.combine(pattern) -- Normalise the path, removing ".."s.

    -- If the pattern is trying to search outside the computer root, just abort.
    -- This will fail later on anyway.
    if pattern == ".." or pattern:sub(1, 3) == "../" then
        error("/" .. pattern .. ": Invalid Path", 2)
    end

    -- If we've no wildcards, just check the file exists.
    if not pattern:find("[*?]") then
        if fs.exists(pattern) then return { pattern } else return {} end
    end

    local parts = {}
    for part in pattern:gmatch("[^/]+") do
        if part:find("[*?]") then
            parts[#parts + 1] = {
                exact = false,
                contents = "^" .. part:gsub(".", find_escape) .. "$",
            }
        else
            parts[#parts + 1] = { exact = true, contents = part }
        end
    end

    local out = {}
    find_aux("", parts, 1, out)
    return out
end

--- Returns true if a path is mounted to the parent filesystem.
--
-- The root filesystem "/" is considered a mount, along with disk folders and
-- the rom folder.
--
-- @tparam string path The path to check.
-- @treturn boolean If the path is mounted, rather than a normal file/folder.
-- @throws If the path does not exist.
-- @see getDrive
-- @since 1.87.0
function fs.isDriveRoot(path)
    expect(1, path, "string")

    local parent = fs.getDir(path)

    -- Force the root directory to be a mount.
    if parent == ".." then return true end

    local drive = fs.getDrive(path)
    return drive ~= nil and drive ~= fs.getDrive(parent)
end
