-- gist.lua - Gist client for ComputerCraft
-- Made by JackMacWindows for CraftOS-PC, modified for CC: Tweaked

-- The following code is a JSON decoder. It was retreived from
-- https://gist.github.com/tylerneylon/59f4bcf316be525b30ab, and is in the public domain.

local json = {}
do

  -- Internal functions.
  -- Returns pos, did_find; there are two cases:
  -- 1. Delimiter found: pos = pos after leading space + delim; did_find = true.
  -- 2. Delimiter not found: pos = pos after leading space;     did_find = false.
  -- This throws an error if err_if_missing is true and the delim is not found.
  local function skip_delim(str, pos, delim, err_if_missing)
    pos = pos + #str:match('^%s*', pos)
    if str:sub(pos, pos) ~= delim then
      if err_if_missing then
        error('Expected ' .. delim .. ' near position ' .. pos)
      end
      return pos, false
    end
    return pos + 1, true
  end

  -- Expects the given pos to be the first character after the opening quote.
  -- Returns val, pos; the returned pos is after the closing quote character.
  local function parse_str_val(str, pos, val)
    val = val or ''
    local early_end_error = 'End of input found while parsing string.'
    if pos > #str then error(early_end_error) end
    local c = str:sub(pos, pos)
    if c == '"'  then return val, pos + 1 end
    if c ~= '\\' then return parse_str_val(str, pos + 1, val .. c) end
    -- We must have a \ character.
    local esc_map = {b = '\b', f = '\f', n = '\n', r = '\r', t = '\t'}
    local nextc = str:sub(pos + 1, pos + 1)
    if not nextc then error(early_end_error) end
    return parse_str_val(str, pos + 2, val .. (esc_map[nextc] or nextc))
  end

  -- Returns val, pos; the returned pos is after the number's final character.
  local function parse_num_val(str, pos)
    local num_str = str:match('^-?%d+%.?%d*[eE]?[+-]?%d*', pos)
    local val = tonumber(num_str)
    if not val then error('Error parsing number at position ' .. pos .. '.') end
    return val, pos + #num_str
  end

  json.null = {}

  function json.decode(str, pos, end_delim)
    pos = pos or 1
    if pos > #str then error('Reached unexpected end of input.') end
    pos = pos + #str:match('^%s*', pos)  -- Skip whitespace.
    local first = str:sub(pos, pos)
    if first == '{' then  -- Parse an object.
      local obj, key, delim_found = {}, true, true
      pos = pos + 1
      while true do
        key, pos = json.decode(str, pos, '}')
        if key == nil then return obj, pos end
        if not delim_found then error('Comma missing between object items.') end
        pos = skip_delim(str, pos, ':', true)  -- true -> error if missing.
        obj[key], pos = json.decode(str, pos)
        pos, delim_found = skip_delim(str, pos, ',')
      end
    elseif first == '[' then  -- Parse an array.
      local arr, val, delim_found = {}, true, true
      pos = pos + 1
      while true do
        val, pos = json.decode(str, pos, ']')
        if val == nil then return arr, pos end
        if not delim_found then error('Comma missing between array items.') end
        arr[#arr + 1] = val
        pos, delim_found = skip_delim(str, pos, ',')
      end
    elseif first == '"' then  -- Parse a string.
      return parse_str_val(str, pos + 1)
    elseif first == '-' or first:match('%d') then  -- Parse a number.
      return parse_num_val(str, pos)
    elseif first == end_delim then  -- End of an object or array.
      return nil, pos + 1
    else  -- Parse true, false, or null.
      local literals = {['true'] = true, ['false'] = false, ['null'] = json.null}
      for lit_str, lit_val in pairs(literals) do
        local lit_end = pos + #lit_str - 1
        if str:sub(pos, lit_end) == lit_str then return lit_val, lit_end + 1 end
      end
      local pos_info_str = 'position ' .. pos .. ': ' .. str:sub(pos, pos + 10)
      error('Invalid json syntax starting at ' .. pos_info_str)
    end
  end

end -- end of library

-- Actual program

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

-- ID can be either just the gist ID or a gist ID followed by a slash and a file name
-- * If a file name is specified, retrieves that file
-- * Otherwise, if there's only one file, retrieves that file
-- * Otherwise, if there's a file named 'init.lua', retrieves 'init.lua'
-- * Otherwise, if there's more than one file but only one *.lua file, retrieves the Lua file
-- * Otherwise, retrieves the first Lua file alphabetically (with a warning)
-- * Otherwise, fails
local function getGistData(id)
    local file
    if id:find("/") ~= nil then id, file = id:match("^([0-9A-Fa-f:]+)/(.+)$") end
    if id:find(":") ~= nil then id = id:gsub(":", "/") end
    write("Connecting to api.github.com... ")
    local handle = http.get("https://api.github.com/gists/" .. id)
    if handle == nil then print("Failed.") return nil end
    if handle.getResponseCode() ~= 200 then print("Failed.") handle.close() return nil end
    local meta = json.decode(handle.readAll())
    handle.close()
    if meta == nil or meta.files == nil then print("Failed.") return nil end
    print("Success.")
    if file then return getGistFile(meta.files[file]), file
    elseif next(meta.files, next(meta.files)) == nil then return getGistFile(meta.files[next(meta.files)]), next(meta.files)
    elseif meta.files["init.lua"] ~= nil then return getGistFile(meta.files["init.lua"]), "init.lua"
    else
        local luaFiles = {}
        for k in pairs(meta.files) do if k:match("%.lua$") then table.insert(luaFiles, k) end end
        table.sort(luaFiles)
        if #luaFiles == 0 then
            print("Error: Could not find any Lua files to download!")
            return nil
        end
        if #luaFiles > 1 then print("Warning: More than one Lua file detected, downloading the first one alphabetically.") end
        return getGistFile(meta.files[luaFiles[1]]), luaFiles[1]
    end
end

local function setTextColor(c) if term.isColor() then term.setTextColor(c) elseif c == colors.white then term.setTextColor(c) else term.setTextColor(colors.lightGray) end end

local function requestAuth(headers)
    if settings.get("gist.id") ~= nil then headers.Authorization = "token " .. settings.get("gist.id") else
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

local args = {...}

local helpstr = "Usages:\ngist put <filenames...> [-- description...]\ngist edit <id> <filenames...> [-- description]\ngist delete <id>\ngist get <id> <filename>\ngist run <id> [arguments...]\ngist info <id>"

if #args < 2 then
    print(helpstr)
    return 1
end

if not http then
    printError("Gist requires http API")
    printError("Set http_enable to true in ComputerCraft's configuration'")
    return 2
end

if args[1] == "get" then
    if #args < 3 then print(helpstr) return 1 end
    local data = getGistData(args[2])
    if data == nil then return 3 end
    local file = fs.open(shell.resolve(args[3]), "w")
    file.write(data)
    file.close()
    print("Downloaded as " .. shell.resolve(args[3]))
elseif args[1] == "run" then
    local data, name = getGistData(args[2])
    if data == nil then return 3 end
    local fn, err = load(data, name, "t", _ENV)
    if fn == nil then error(err) end
    local ok, msg = pcall(fn, table.unpack(args, 3))
    if not ok then error(msg) end
elseif args[1] == "put" then
    local data = {files = {}, public = true}
    local i = 2
    while args[i] ~= nil and args[i] ~= "--" do
        if data.files[fs.getName(args[i])] then print("Cannot upload files with duplicate names.") return 2 end
        local file = fs.open(shell.resolve(args[i]), "r")
        if file == nil then print("Could not read " .. shell.resolve(args[i]) .. ".") return 2 end
        data.files[fs.getName(args[i])] = {content = file.readAll()}
        file.close()
        i = i + 1
    end
    if args[i] == "--" then data.description = table.concat({table.unpack(args, i + 1)}, " ") end
    local jsonfiles = ""
    for k, v in pairs(data.files) do jsonfiles = jsonfiles .. (jsonfiles == "" and "" or ",\n") .. ("    \"%s\": {\n      \"content\": %s\n    }"):format(k, textutils.serializeJSON(v.content)) end
    if jsonfiles == "" then print("No such file") return 2 end
    local jsondata = ([[{
  "description": %s,
  "public": true,
  "files": {
%s
  }
}]]):format(data.description and '"' .. data.description .. '"' or "null", jsonfiles)
    local headers = {["Content-Type"] = "application/json"}
    requestAuth(headers)
    write("Connecting to api.github.com... ")
    local handle = http.post("https://api.github.com/gists", jsondata, headers)
    if handle == nil then print("Failed.") return 3 end
    local resp = json.decode(handle.readAll())
    if handle.getResponseCode() ~= 201 or resp == nil then print("Failed: " .. handle.getResponseCode() .. ": " .. (resp and textutils.serializeJSON(resp) or "Unknown error")) handle.close() return 3 end
    handle.close()
    print("Success.\nUploaded as " .. resp.html_url .. "\nRun 'gist get " .. resp.id .. "' to download anywhere")
elseif args[1] == "info" then
    local id = args[2]
    if id:find("/") ~= nil then id = id:match("^([0-9A-Fa-f:]+)/.+$") end
    if id:find(":") ~= nil then id = id:gsub(":", "/") end
    write("Connecting to api.github.com... ")
    local handle = http.get("https://api.github.com/gists/" .. id)
    if handle == nil then print("Failed.") return 3 end
    if handle.getResponseCode() ~= 200 then print("Failed.") handle.close() return 3 end
    local meta = json.decode(handle.readAll())
    handle.close()
    if meta == nil or meta.files == nil then print("Failed.") return 3 end
    local f = {}
    for k in pairs(meta.files) do table.insert(f, k) end
    table.sort(f)
    print("Success.")
    setTextColor(colors.yellow)
    write("Description: ")
    setTextColor(colors.white)
    print(meta.description)
    setTextColor(colors.yellow)
    write("Author: ")
    setTextColor(colors.white)
    print(meta.owner.login)
    setTextColor(colors.yellow)
    write("Revisions: ")
    setTextColor(colors.white)
    print(#meta.history)
    setTextColor(colors.yellow)
    print("Files in this Gist:")
    setTextColor(colors.white)
    textutils.tabulate(f)
elseif args[1] == "edit" then
    if #args < 3 then print(helpstr) return 1 end
    local data = {files = {}, public = true}
    local id = args[2]
    if id:find("/") ~= nil then id = id:match("^([0-9A-Fa-f:]+)/.+$") end
    if id:find(":") ~= nil then id = id:gsub(":", "/") end
    local i = 3
    while args[i] ~= nil and args[i] ~= "--" do
        if data.files[fs.getName(args[i])] then error("Cannot upload files with duplicate names.") end
        local file = fs.open(shell.resolve(args[i]), "r")
        if file == nil then data.files[fs.getName(args[i])] = {} else
            data.files[fs.getName(args[i])] = {content = file.readAll()}
            file.close()
        end
        i = i + 1
    end
    if args[i] == "--" then data.description = table.concat({table.unpack(args, i + 1)}, " ") else
        write("Connecting to api.github.com... ")
        local handle = http.get("https://api.github.com/gists/" .. id)
        if handle == nil then print("Failed.") return 3 end
        if handle.getResponseCode() ~= 200 then print("Failed.") handle.close() return 3 end
        local meta = json.decode(handle.readAll())
        handle.close()
        if meta == nil or meta.files == nil then print("Failed.") return 3 end
        data.description = meta.description
        print("Success.")
    end
    -- Get authorization
    local headers = {["Content-Type"] = "application/json"}
    requestAuth(headers)
    local jsonfiles = ""
    for k, v in pairs(data.files) do jsonfiles = jsonfiles .. (jsonfiles == "" and "" or ",\n") .. (v.content == nil and ("    \"%s\": null"):format(k) or ("    \"%s\": {\n      \"content\": %s\n    }"):format(k, textutils.serializeJSON(v.content))) end
    local jsondata = ([[{
  "description": %s,
  "public": true,
  "files": {
%s
  }
}]]):format(data.description and '"' .. data.description .. '"' or "null", jsonfiles)
    write("Connecting to api.github.com... ")
    local handle = http.post{url = "https://api.github.com/gists/" .. id, body = jsondata, headers = headers, method = "PATCH"}
    if handle == nil then print("Failed.") return 3 end
    local resp = json.decode(handle.readAll())
    if handle.getResponseCode() ~= 200 or resp == nil then print("Failed: " .. handle.getResponseCode() .. ": " .. (resp and textutils.serializeJSON(resp) or "Unknown error")) handle.close() return 3 end
    handle.close()
    print("Success.\nUploaded as " .. resp.html_url .. "\nRun 'gist get " .. resp.id .. "' to download anywhere")
elseif args[1] == "delete" then
    local id = args[2]
    if id:find("/") ~= nil or id:find(":") ~= nil then id = id:match("^([0-9A-Fa-f]+)") end
    local headers = {}
    requestAuth(headers)
    write("Connecting to api.github.com... ")
    local handle = http.post{url = "https://api.github.com/gists/" .. id, headers = headers, method = "DELETE"}
    if handle == nil then print("Failed.") return 3 end
    if handle.getResponseCode() ~= 204 then print("Failed: " .. handle.getResponseCode() .. ".") handle.close() return 3 end
    handle.close()
    print("Success.")
    print("The requested Gist has been deleted.")
else print(helpstr) return 1 end
