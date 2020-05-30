--- gist.lua - Gist client for ComputerCraft
-- Made by JackMacWindows for CraftOS-PC and CC: Tweaked
--
-- @module cc.http.gist

local expect = require and require("cc.expect").expect or dofile("/rom/modules/main/cc/expect.lua").expect

if not http then
    if _G.config ~= nil then error("Gist requires http API\nSet http_enable to true in the CraftOS-PC configuration")
    else error("Gist requires http API\nSet http_enable to true in ComputerCraft's configuration") end
end

local gist = {}

local function emptyfn() end -- to reduce memory/speed footprint when using empty functions

-- Internal functions

local function getGistFile(data)
    if not data.truncated then return data.content else
        local handle = http.get(data.raw_url)
        if not handle then error("Could not connect to api.github.com.") end
        if handle.getResponseCode() ~= 200 then
            handle.close()
            error("Failed to download file data.")
        end
        local d = handle.readAll()
        handle.close()
        return d
    end
end

local function setTextColor(c) if term.isColor() then term.setTextColor(c) elseif c == colors.white or c == colors.yellow then term.setTextColor(colors.white) else term.setTextColor(colors.lightGray) end end

local function requestAuth(headers, interactive)
    if settings.get("gist.id") ~= nil then
        headers.Authorization = "token " .. settings.get("gist.id")
        return true
    elseif interactive then
        setTextColor(colors.yellow)
        write("You need to add a Personal Access Token (PAK) to upload Gists. Follow the instructions at ")
        setTextColor(colors.blue)
        write("https://tinyurl.com/GitHubPAK")
        setTextColor(colors.yellow)
        write(" to generate one. Make sure to check the '")
        setTextColor(colors.blue)
        write("gist")
        setTextColor(colors.yellow)
        print("' checkbox on step 7 (under 'Select scopes'). Once done, paste it here.")
        setTextColor(colors.lime)
        write("PAK: ")
        setTextColor(colors.white)
        local pak = read()
        if pak == nil or pak == "" then error("Invalid PAK, please try again.") end
        settings.set("gist.id", pak)
        settings.save(".settings")
        headers.Authorization = "token " .. pak
        return true
    end
    return false
end

-- User API - this can be loaded with require "cc.http.gist"

-- ID can be either just the gist ID or a gist ID followed by a slash and a file name
-- * If a file name is specified, retrieves that file
-- * Otherwise, if there's only one file, retrieves that file
-- * Otherwise, if there's a file named 'init.lua', retrieves 'init.lua'
-- * Otherwise, if there's more than one file but only one *.lua file, retrieves the Lua file
-- * Otherwise, retrieves the first Lua file alphabetically (with a warning)
-- * Otherwise, fails

--- Retrieves one file from a Gist using the specified ID.
-- @tparam string id The Gist ID to download from. See above comments for more details.
-- @tparam[opt] function progress A function to use to report status messages.
-- @treturn string|nil The contents of the specified Gist file, or nil on error.
-- @treturn string|nil The name of the file that was chosen to be downloaded, or a message on error.
function gist.get(id, progress)
    expect(1, id, "string")
    expect(2, progress, "function", "nil")
    progress = progress or emptyfn
    local file
    if id:find("/") ~= nil then id, file = id:match("^([0-9A-Fa-f:]+)/(.+)$") end
    if id == nil or not id:match("^[0-9A-Fa-f][0-9A-Fa-f:]+[0-9A-Fa-f]$") then error("bad argument #1 to 'get' (invalid ID)", 2) end
    if id:find(":") ~= nil then id = id:gsub(":", "/") end
    progress("Connecting to api.github.com... ")
    local handle = http.get("https://api.github.com/gists/" .. id)
    if handle == nil then
        progress("Failed.\n")
        return nil, "Failed to connect"
    end
    local meta = textutils.unserializeJSON(handle.readAll())
    local code = handle.getResponseCode()
    handle.close()
    if code ~= 200 then
        progress("Failed.\n")
        return nil, "Invalid response code (" .. code .. ")" .. (meta and ": " .. meta.message or "")
    end
    if meta == nil or meta.files == nil then
        progress("Failed.\n")
        return nil, meta and "GitHub API error: " .. meta.message or "Error parsing JSON"
    end
    progress("Success.\n")
    if file then return getGistFile(meta.files[file]), file
    elseif next(meta.files, next(meta.files)) == nil then return getGistFile(meta.files[next(meta.files)]), next(meta.files)
    elseif meta.files["init.lua"] ~= nil then return getGistFile(meta.files["init.lua"]), "init.lua"
    else
        local luaFiles = {}
        for k in pairs(meta.files) do if k:match("%.lua$") then table.insert(luaFiles, k) end end
        table.sort(luaFiles)
        if #luaFiles == 0 then
            progress("Error: Could not find any Lua files to download!\n")
            return nil, "Could not find any Lua files to download"
        end
        if #luaFiles > 1 then progress("Warning: More than one Lua file detected, downloading the first one alphabetically.\n") end
        return getGistFile(meta.files[luaFiles[1]]), luaFiles[1]
    end
end

--- Runs a specified Gist. This is a wrapper for convenience.
-- @tparam string id The Gist ID to download from. See above comments for more details.
-- @tparam[opt] function progress A function to use to report status messages. If
-- this is not a function, it will be used as an argument to the script.
-- @tparam[opt] any ... Any arguments to pass to the script.
-- @treturn any Any results returned from the script.
function gist.run(id, progress, ...)
    expect(1, id, "string")
    local args = table.pack(...)
    if type(progress) ~= "function" and progress ~= nil then
        table.insert(args, 1, progress)
        progress = nil
    end
    local data, name = gist.get(id, progress)
    if data == nil then return end
    local fn, err = load(data, name, "t", _ENV)
    if fn == nil then error(err) end
    local retval = table.pack(pcall(fn, table.unpack(args)))
    if not retval[1] then error(retval[2]) end
    return table.unpack(retval, 2)
end

--- Retrieves a table of all files from a Gist.
-- @tparam string id The Gist ID to download.
-- @tparam[opt] function progress A function to use to report status messages.
-- @treturn table|nil A key-value list of all files in the Gist, or nil on error.
-- @treturn string|nil If an error occurred, a string describing the error.
function gist.getAll(id, progress)
    expect(1, id, "string")
    expect(2, progress, "function", "nil")
    progress = progress or emptyfn
    if id:find("/") ~= nil then id = id:match("^([0-9A-Fa-f:]+)/.*$") end
    if id == nil or not id:match("^[0-9A-Fa-f][0-9A-Fa-f:]+[0-9A-Fa-f]$") then error("bad argument #1 to 'getAll' (invalid ID)", 2) end
    if id:find(":") ~= nil then id = id:gsub(":", "/") end
    progress("Connecting to api.github.com... ")
    local handle = http.get("https://api.github.com/gists/" .. id)
    if handle == nil then progress("Failed.\n") return nil, "Failed to connect" end
    local meta = textutils.unserializeJSON(handle.readAll())
    local code = handle.getResponseCode()
    handle.close()
    if code ~= 200 then
        progress("Failed.\n")
        return nil, "Invalid response code (" .. code .. ")" .. (meta and ": " .. meta.message or "")
    end
    if meta == nil or meta.files == nil then
        progress("Failed.\n")
        return nil, meta and meta.message and "GitHub API error: " .. meta.message or "Error parsing JSON"
    end
    progress("Success.\n")
    local retval = {}
    for k, v in pairs(meta.files) do retval[k] = getGistFile(v) end
    return retval
end

--- Returns some information about a Gist.
-- @tparam string id The Gist ID to get info about.
-- @tparam[opt] function progress A function to use to report status messages.
-- @treturn table|nil A table of information about the Gist. The table may
-- contain the following entries:
--  - description: The description for the Gist.
--  - author: The username of the author of the Gist.
--  - revisionCount: The number of revisions that have been made to the Gist.
--  - files: A list of all file names in the Gist, sorted alphabetically.
-- @treturn string|nil If an error occurred, a string describing the error.
function gist.info(id, progress)
    expect(1, id, "string")
    expect(2, progress, "function", "nil")
    progress = progress or emptyfn
    if id:find("/") ~= nil then id = id:match("^([0-9A-Fa-f:]+)/.*$") end
    if id == nil or not id:match("^[0-9A-Fa-f][0-9A-Fa-f:]+[0-9A-Fa-f]$") then error("bad argument #1 to 'info' (invalid ID)", 2) end
    if id:find(":") ~= nil then id = id:gsub(":", "/") end
    progress("Connecting to api.github.com... ")
    local handle = http.get("https://api.github.com/gists/" .. id)
    if handle == nil then progress("Failed.\n") return nil, "Failed to connect" end
    local meta = textutils.unserializeJSON(handle.readAll())
    local code = handle.getResponseCode()
    handle.close()
    if code ~= 200 then
        progress("Failed.\n")
        return nil, "Invalid response code (" .. code .. ")" .. (meta and ": " .. meta.message or "")
    end
    if meta == nil or meta.files == nil then
        progress("Failed.\n")
        return nil, meta and meta.message and "GitHub API error: " .. meta.message or "Error parsing JSON"
    end
    local f = {}
    for k in pairs(meta.files) do table.insert(f, k) end
    table.sort(f)
    progress("Success.\n")
    return { description = meta.description, author = meta.owner.login, revisionCount = #meta.history, files = f }
end

--- Uploads a list of files to Gist, updating a previous Gist if desired.
-- @tparam table files The files to upload to Gist. This table should be
-- structured with a key as file name and a string with the file contents. If
-- updating a Gist, files can be deleted by setting the data to textutils.json_null.
-- @tparam[opt] string description The description of the Gist. This is required
-- when updating a Gist, but is optional when uploading a Gist for the first
-- time. If you don't want to change the description when updating, you can get
-- the current description with gist.info() and pass in the description field.
-- @tparam[opt] string id The ID of the Gist to update. If nil, a new Gist will
-- be created.
-- @tparam[opt] boolean interactive Set this to true to allow asking for a PAK
-- if one is not available in the settings. If this is not specified, this
-- function will return nil if gist.id is not available in the settings.
-- @treturn string|nil The ID of the Gist, or nil on error.
-- @treturn string|nil The URL of the Gist, or a string on error.
function gist.put(files, description, id, interactive)
    expect(1, files, "table")
    expect(3, id, "string", "nil")
    expect(2, description, "string", id == nil and "nil" or nil)
    expect(4, interactive, "boolean", "nil")
    if id then
        if id:find("/") ~= nil then id = id:match("^([0-9A-Fa-f:]+)/.*$") end
        if id == nil or not id:match("^[0-9A-Fa-f][0-9A-Fa-f:]+[0-9A-Fa-f]$") then error("bad argument #3 to 'put' (invalid ID)", 2) end
        if id:find(":") ~= nil then id = id:gsub(":", "/") end
    end
    local data = { files = {}, public = true, description = description }
    for k, v in pairs(files) do if v == textutils.json_null then data.files[k] = v else data.files[k] = { content = v } end end
    local headers = { ["Content-Type"] = "application/json" }
    if not requestAuth(headers, interactive) then return nil, "Authentication required" end
    if interactive then write("Connecting to api.github.com... ") end
    local handle
    if id then handle = http.post{ url = "https://api.github.com/gists/" .. id, body = textutils.serializeJSON(data):gsub("\n", "n"), headers = headers, method = "PATCH" }
    else handle = http.post("https://api.github.com/gists", textutils.serializeJSON(data):gsub("\n", "n"), headers) end
    if handle == nil then if interactive then print("Failed.") end return nil, "Could not connect" end
    local resp = textutils.unserializeJSON(handle.readAll())
    if handle.getResponseCode() ~= 201 and handle.getResponseCode() ~= 200 or resp == nil then
        if interactive then print("Failed: " .. handle.getResponseCode() .. ": " .. (resp and resp.message or "Unknown error")) end
        handle.close()
        return nil, "Failed: " .. handle.getResponseCode() .. ": " .. (resp and resp.message or "Unknown error")
    end
    handle.close()
    if interactive then print("Success.") end
    return resp.id, resp.html_url
end

--- Deletes a Gist.
-- @tparam string id The Gist ID to delete.
-- @tparam[opt] boolean interactive Set this to true to allow asking for a PAK
-- if one is not available in the settings. If this is not specified, this
-- function will return false if gist.id is not available in the settings.
-- @treturn boolean Whether the request succeeded.
-- @treturn string|nil If an error occurred, a message describing the error.
function gist.delete(id, interactive)
    expect(1, id, "string")
    expect(2, interactive, "boolean", "nil")
    if id:find("/") ~= nil or id:find(":") ~= nil then id = id:match("^([0-9A-Fa-f]+)") end
    if id == nil or not id:match("^[0-9A-Fa-f][0-9A-Fa-f:]+[0-9A-Fa-f]$") then error("bad argument #1 to 'delete' (invalid ID)", 2) end
    local headers = {}
    if not requestAuth(headers, interactive) then return false, "Authentication required" end
    if interactive then write("Connecting to api.github.com... ") end
    local handle = http.post{ url = "https://api.github.com/gists/" .. id, headers = headers, method = "DELETE" }
    if handle == nil then if interactive then print("Failed.") end return false, "Could not connect" end
    if handle.getResponseCode() ~= 204 then
        local resp = textutils.unserializeJSON(handle.readAll())
        if interactive then print("Failed: " .. handle.getResponseCode() .. ": " .. (resp and resp.message or "Unknown error")) end
        handle.close()
        return false, "Failed: " .. handle.getResponseCode() .. ": " .. (resp and resp.message or "Unknown error")
    end
    handle.close()
    if interactive then print("Success.") end
    return true
end

return gist
