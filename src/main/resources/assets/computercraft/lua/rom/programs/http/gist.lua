--- gist.lua - Gist client for ComputerCraft
-- Made by JackMacWindows for CraftOS-PC and CC: Tweaked
--
-- @module gist

local expect = require and require("cc.expect").expect or dofile("/rom/modules/main/cc/expect.lua").expect

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
    if settings.get("gist.id") ~= nil then headers.Authorization = "token " .. settings.get("gist.id") elseif interactive then
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
        headers.Authorization = "token " .. pak
    end
end

-- User API - this can be loaded with os.loadAPI

-- ID can be either just the gist ID or a gist ID followed by a slash and a file name
-- * If a file name is specified, retrieves that file
-- * Otherwise, if there's only one file, retrieves that file
-- * Otherwise, if there's a file named 'init.lua', retrieves 'init.lua'
-- * Otherwise, if there's more than one file but only one *.lua file, retrieves the Lua file
-- * Otherwise, retrieves the first Lua file alphabetically (with a warning)
-- * Otherwise, fails

--- Retrieves one file from a Gist using the specified ID.
-- @tparam string id The Gist ID to download from. See above comments for more details.
-- @tparam[opt] any showMessages Set to any true value to show messages while connecting.
-- @treturn string|nil The contents of the specified Gist file, or nil on error.
-- @treturn string|nil The name of the file that was chosen to be downloaded, or nil on error.
function get(id, showMessages)
    expect(1, id, "string")
    local file
    if id:find("/") ~= nil then id, file = id:match("^([0-9A-Fa-f:]+)/(.+)$") end
    if id == nil or not id:match("^[0-9A-Fa-f][0-9A-Fa-f:]+[0-9A-Fa-f]$") then error("bad argument #1 to 'get' (invalid ID)", 2) end
    if id:find(":") ~= nil then id = id:gsub(":", "/") end
    if showMessages then write("Connecting to api.github.com... ") end
    local handle = http.get("https://api.github.com/gists/" .. id)
    if handle == nil then if showMessages then print("Failed.") end return nil end
    if handle.getResponseCode() ~= 200 then if showMessages then print("Failed.") end handle.close() return nil end
    local meta = textutils.unserializeJSON(handle.readAll())
    handle.close()
    if meta == nil or meta.files == nil then if showMessages then print("Failed.") end return nil end
    if showMessages then print("Success.") end
    if file then return getGistFile(meta.files[file]), file
    elseif next(meta.files, next(meta.files)) == nil then return getGistFile(meta.files[next(meta.files)]), next(meta.files)
    elseif meta.files["init.lua"] ~= nil then return getGistFile(meta.files["init.lua"]), "init.lua"
    else
        local luaFiles = {}
        for k in pairs(meta.files) do if k:match("%.lua$") then table.insert(luaFiles, k) end end
        table.sort(luaFiles)
        if #luaFiles == 0 then
            if showMessages then print("Error: Could not find any Lua files to download!") end
            return nil
        end
        if showMessages and #luaFiles > 1 then print("Warning: More than one Lua file detected, downloading the first one alphabetically.") end
        return getGistFile(meta.files[luaFiles[1]]), luaFiles[1]
    end
end

--- Runs a specified Gist. This is a wrapper for convenience.
-- @tparam string id The Gist ID to download from. See above comments for more details.
-- @tparam[opt] any showMessages Set to any true value to show messages while connecting.
-- @tparam[opt] any ... Any arguments to pass to the script,
-- @treturn any Any results returned from the script.
function run(id, ...)
    local data, name = get(id)
    if data == nil then return end
    local fn, err = load(data, name, "t", _ENV)
    if fn == nil then error(err) end
    local retval = table.pack(pcall(fn, ...))
    if not retval[1] then error(retval[2]) end
    return table.unpack(retval, 2)
end

--- Retrieves a table of all files from a Gist.
-- @tparam string id The Gist ID to download.
-- @tparam[opt] any showMessages Set to any true value to show messages while connecting.
-- @treturn table|nil A key-value list of all files in the Gist, or nil on error.
function getAll(id, showMessages)
    expect(1, id, "string")
    if id:find("/") ~= nil then id = id:match("^([0-9A-Fa-f:]+)/.*$") end
    if id == nil or not id:match("^[0-9A-Fa-f][0-9A-Fa-f:]+[0-9A-Fa-f]$") then error("bad argument #1 to 'getAll' (invalid ID)", 2) end
    if id:find(":") ~= nil then id = id:gsub(":", "/") end
    if showMessages then write("Connecting to api.github.com... ") end
    local handle = http.get("https://api.github.com/gists/" .. id)
    if handle == nil then if showMessages then print("Failed.") end return nil end
    if handle.getResponseCode() ~= 200 then if showMessages then print("Failed.") end handle.close() return nil end
    local meta = textutils.unserializeJSON(handle.readAll())
    handle.close()
    if meta == nil or meta.files == nil then if showMessages then print("Failed.") end return nil end
    if showMessages then print("Success.") end
    local retval = {}
    for k, v in pairs(meta.files) do retval[k] = getGistFile(v) end
    return retval
end

--- Returns some information about a Gist.
-- @tparam string id The Gist ID to get info about.
-- @tparam[opt] any showMessages Set to any true value to show messages while connecting.
-- @treturn table|nil A table of information about the Gist. The table may
-- contain the following entries:
--  - description: The description for the Gist.
--  - author: The username of the author of the Gist.
--  - revisionCount: The number of revisions that have been made to the Gist.
--  - files: A list of all file names in the Gist, sorted alphabetically.
function info(id, showMessages)
    expect(1, id, "string")
    if id:find("/") ~= nil then id = id:match("^([0-9A-Fa-f:]+)/.*$") end
    if id == nil or not id:match("^[0-9A-Fa-f][0-9A-Fa-f:]+[0-9A-Fa-f]$") then error("bad argument #1 to 'info' (invalid ID)", 2) end
    if id:find(":") ~= nil then id = id:gsub(":", "/") end
    if showMessages then write("Connecting to api.github.com... ") end
    local handle = http.get("https://api.github.com/gists/" .. id)
    if handle == nil then if showMessages then print("Failed.") end return nil end
    if handle.getResponseCode() ~= 200 then if showMessages then print("Failed.") end handle.close() return nil end
    local meta = textutils.unserializeJSON(handle.readAll())
    handle.close()
    if meta == nil or meta.files == nil then if showMessages then print("Failed.") end return nil end
    local f = {}
    for k in pairs(meta.files) do table.insert(f, k) end
    table.sort(f)
    if showMessages then print("Success.") end
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
-- @treturn string|nil The URL of the Gist, or nil on error.
function put(files, description, id, interactive)
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
    requestAuth(headers, interactive)
    if headers.Authorization == nil then return nil end
    if interactive then write("Connecting to api.github.com... ") end
    local handle
    if id then handle = http.post{ url = "https://api.github.com/gists/" .. id, body = textutils.serializeJSON(data):gsub("\n", "n"), headers = headers, method = "PATCH" }
    else handle = http.post("https://api.github.com/gists", textutils.serializeJSON(data):gsub("\n", "n"), headers) end
    if handle == nil then if interactive then print("Failed.") end return nil end
    local resp = textutils.unserializeJSON(handle.readAll())
    if handle.getResponseCode() ~= 201 and handle.getResponseCode() ~= 200 or resp == nil then
        if interactive then print("Failed: " .. handle.getResponseCode() .. ": " .. (resp and textutils.serializeJSON(resp) or "Unknown error")) end
        handle.close()
        return nil
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
function delete(id, interactive)
    expect(1, id, "string")
    expect(2, interactive, "boolean", "nil")
    if id:find("/") ~= nil or id:find(":") ~= nil then id = id:match("^([0-9A-Fa-f]+)") end
    if id == nil or not id:match("^[0-9A-Fa-f][0-9A-Fa-f:]+[0-9A-Fa-f]$") then error("bad argument #1 to 'delete' (invalid ID)", 2) end
    local headers = {}
    requestAuth(headers, interactive)
    if interactive then write("Connecting to api.github.com... ") end
    local handle = http.post{ url = "https://api.github.com/gists/" .. id, headers = headers, method = "DELETE" }
    if handle == nil then if interactive then print("Failed.") end return false end
    if handle.getResponseCode() ~= 204 then if interactive then print("Failed: " .. handle.getResponseCode() .. ".") end handle.close() return false end
    handle.close()
    if interactive then print("Success.") end
    return true
end

-- Program part - run this with shell

if shell then

    local args = { ... }

    local function readFile(filename, files, isEditing)
        if fs.isDir(shell.resolve(filename)) then
            for _, v in ipairs(fs.list(shell.resolve(filename))) do if readFile(fs.combine(filename, v), files, isEditing) then return true end end
        else
            if files[fs.getName(filename)] then print("Cannot upload files with duplicate names.") return true end
            local file = fs.open(shell.resolve(filename), "rb")
            if file == nil then
                if not isEditing then print("Could not read " .. filename .. ".") return true
                else files[fs.getName(filename)] = textutils.json_null end
            else
                files[fs.getName(filename)] = file.readAll()
                file.close()
            end
        end
    end

    local function getFiles(isEditing)
        local files = {}
        local i = isEditing and 3 or 2
        while args[i] ~= nil and args[i] ~= "--" do
            if readFile(args[i], files, isEditing) then return nil end
            i = i + 1
        end
        if args[i] == "--" then return files, table.concat({ table.unpack(args, i + 1) }, " ") end
        return files
    end

    local helpstr = "Usages:\ngist put <files...> [-- description...]\ngist edit <id> <files...> [-- description]\ngist delete <id>\ngist get <id> <filename>\ngist run <id> [arguments...]\ngist info <id>"

    if #args < 2 then
        print(helpstr)
        return 1
    end

    if not http then
        printError("Gist requires http API")
        printError("Set http_enable to true in ComputerCraft's configuration")
        return 2
    end

    if args[1] == "get" then
        if #args < 3 then print(helpstr) return 1 end
        if args[3]:sub(#args[3]) == "/" or fs.isDir(shell.resolve(args[3])) then
            fs.makeDir(shell.resolve(args[3]))
            local files = getAll(args[2], true)
            if files == nil then return 3 end
            for k, v in pairs(files) do
                local file = fs.open(shell.resolve(fs.combine(args[3], k)), "wb")
                file.write(v)
                file.close()
            end
            print("Downloaded all files to " .. shell.resolve(args[3]))
        else
            local data = get(args[2], true)
            if data == nil then return 3 end
            local file = fs.open(shell.resolve(args[3]), "wb")
            file.write(data)
            file.close()
            print("Downloaded as " .. shell.resolve(args[3]))
        end
    elseif args[1] == "run" then
        local data, name = get(args[2], true)
        if data == nil then return end
        local fn, err = load(data, name, "t", _ENV)
        if fn == nil then error(err) end
        local retval = table.pack(pcall(fn, table.unpack(args, 3)))
        if not retval[1] then printError(retval[2])
        else return table.unpack(retval, 2) end
    elseif args[1] == "put" then
        local files, description = getFiles(false)
        if files == nil then return end
        local id, html_url = put(files, description, nil, true)
        if id ~= nil then print("Uploaded as " .. html_url .. "\nRun 'gist get " .. id .. "' to download anywhere") end
    elseif args[1] == "info" then
        local tab = info(args[2], true)
        if tab == nil then return end
        setTextColor(colors.yellow)
        write("Description: ")
        setTextColor(colors.white)
        print(tab.description)
        setTextColor(colors.yellow)
        write("Author: ")
        setTextColor(colors.white)
        print(tab.author)
        setTextColor(colors.yellow)
        write("Revisions: ")
        setTextColor(colors.white)
        print(tab.revisionCount)
        setTextColor(colors.yellow)
        print("Files in this Gist:")
        setTextColor(colors.white)
        textutils.tabulate(tab.files)
    elseif args[1] == "edit" then
        if #args < 3 then print(helpstr) return 1 end
        local files, description = getFiles(true)
        if files == nil then return end
        if not description then description = info(args[2], true).description end
        local _, html_url = put(files, description, args[2], true)
        if html_url then print("Uploaded as " .. html_url .. "\nRun 'gist get " .. args[2] .. "' to download anywhere") end
    elseif args[1] == "delete" then
        if delete(args[2], true) then print("The requested Gist has been deleted.") end
    else print(helpstr) return 1 end

else return { get = get, getAll = getAll, run = run, info = info, put = put, delete = delete } end
