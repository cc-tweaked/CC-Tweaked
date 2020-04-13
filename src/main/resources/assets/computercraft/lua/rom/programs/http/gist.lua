-- gist.lua - Gist client for ComputerCraft
-- Made by JackMacWindows for CraftOS-PC and CC: Tweaked

-- The following code consists of a JSON library.

local json
do

--
-- json.lua
--
-- Copyright (c) 2019 rxi
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy of
-- this software and associated documentation files (the "Software"), to deal in
-- the Software without restriction, including without limitation the rights to
-- use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
-- of the Software, and to permit persons to whom the Software is furnished to do
-- so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in all
-- copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
-- SOFTWARE.
--

json = { _version = "0.1.2" }

-------------------------------------------------------------------------------
-- Encode
-------------------------------------------------------------------------------

local encode

local escape_char_map = {
  [ "\\" ] = "\\\\",
  [ "\"" ] = "\\\"",
  [ "\b" ] = "\\b",
  [ "\f" ] = "\\f",
  [ "\n" ] = "\\n",
  [ "\r" ] = "\\r",
  [ "\t" ] = "\\t",
}

local escape_char_map_inv = { [ "\\/" ] = "/" }
for k, v in pairs(escape_char_map) do
  escape_char_map_inv[v] = k
end


local function escape_char(c)
  return escape_char_map[c] or string.format("\\u%04x", c:byte())
end


local function encode_nil(val)
  return "null"
end


local function encode_table(val, stack)
  local res = {}
  stack = stack or {}

  -- Circular reference?
  if stack[val] then error("circular reference") end

  stack[val] = true

  if rawget(val, 1) ~= nil or next(val) == nil then
    -- Treat as array -- check keys are valid and it is not sparse
    local n = 0
    for k in pairs(val) do
      if type(k) ~= "number" then
        error("invalid table: mixed or invalid key types")
      end
      n = n + 1
    end
    if n ~= #val then
      error("invalid table: sparse array")
    end
    -- Encode
    for _, v in ipairs(val) do
      table.insert(res, encode(v, stack))
    end
    stack[val] = nil
    return "[" .. table.concat(res, ",") .. "]"

  else
    -- Treat as an object
    for k, v in pairs(val) do
      if type(k) ~= "string" then
        error("invalid table: mixed or invalid key types")
      end
      table.insert(res, encode(k, stack) .. ":" .. encode(v, stack))
    end
    stack[val] = nil
    return "{" .. table.concat(res, ",") .. "}"
  end
end


local function encode_string(val)
  return '"' .. val:gsub('[%z\1-\31\\"]', escape_char) .. '"'
end


local function encode_number(val)
  -- Check for NaN, -inf and inf
  if val ~= val or val <= -math.huge or val >= math.huge then
    error("unexpected number value '" .. tostring(val) .. "'")
  end
  return string.format("%.14g", val)
end


local type_func_map = {
  [ "nil"     ] = encode_nil,
  [ "table"   ] = encode_table,
  [ "string"  ] = encode_string,
  [ "number"  ] = encode_number,
  [ "boolean" ] = tostring,
}


encode = function(val, stack)
  local t = type(val)
  local f = type_func_map[t]
  if f then
    return f(val, stack)
  end
  error("unexpected type '" .. t .. "'")
end


function json.encode(val)
  return ( encode(val) )
end


-------------------------------------------------------------------------------
-- Decode
-------------------------------------------------------------------------------

local parse

local function create_set(...)
  local res = {}
  for i = 1, select("#", ...) do
    res[ select(i, ...) ] = true
  end
  return res
end

local space_chars   = create_set(" ", "\t", "\r", "\n")
local delim_chars   = create_set(" ", "\t", "\r", "\n", "]", "}", ",")
local escape_chars  = create_set("\\", "/", '"', "b", "f", "n", "r", "t", "u")
local literals      = create_set("true", "false", "null")

local literal_map = {
  [ "true"  ] = true,
  [ "false" ] = false,
  [ "null"  ] = nil,
}


local function next_char(str, idx, set, negate)
  for i = idx, #str do
    if set[str:sub(i, i)] ~= negate then
      return i
    end
  end
  return #str + 1
end


local function decode_error(str, idx, msg)
  local line_count = 1
  local col_count = 1
  for i = 1, idx - 1 do
    col_count = col_count + 1
    if str:sub(i, i) == "\n" then
      line_count = line_count + 1
      col_count = 1
    end
  end
  error( string.format("%s at line %d col %d", msg, line_count, col_count) )
end


local function codepoint_to_utf8(n)
  -- http://scripts.sil.org/cms/scripts/page.php?site_id=nrsi&id=iws-appendixa
  local f = math.floor
  if n <= 0x7f then
    return string.char(n)
  elseif n <= 0x7ff then
    return string.char(f(n / 64) + 192, n % 64 + 128)
  elseif n <= 0xffff then
    return string.char(f(n / 4096) + 224, f(n % 4096 / 64) + 128, n % 64 + 128)
  elseif n <= 0x10ffff then
    return string.char(f(n / 262144) + 240, f(n % 262144 / 4096) + 128,
                       f(n % 4096 / 64) + 128, n % 64 + 128)
  end
  error( string.format("invalid unicode codepoint '%x'", n) )
end


local function parse_unicode_escape(s)
  local n1 = tonumber( s:sub(3, 6),  16 )
  local n2 = tonumber( s:sub(9, 12), 16 )
  -- Surrogate pair?
  if n2 then
    return codepoint_to_utf8((n1 - 0xd800) * 0x400 + (n2 - 0xdc00) + 0x10000)
  else
    return codepoint_to_utf8(n1)
  end
end


local function parse_string(str, i)
  local has_unicode_escape = false
  local has_surrogate_escape = false
  local has_escape = false
  local last
  for j = i + 1, #str do
    local x = str:byte(j)

    if x < 32 then
      decode_error(str, j, "control character in string")
    end

    if last == 92 then -- "\\" (escape char)
      if x == 117 then -- "u" (unicode escape sequence)
        local hex = str:sub(j + 1, j + 5)
        if not hex:find("%x%x%x%x") then
          decode_error(str, j, "invalid unicode escape in string")
        end
        if hex:find("^[dD][89aAbB]") then
          has_surrogate_escape = true
        else
          has_unicode_escape = true
        end
      else
        local c = string.char(x)
        if not escape_chars[c] then
          decode_error(str, j, "invalid escape char '" .. c .. "' in string")
        end
        has_escape = true
      end
      last = nil

    elseif x == 34 then -- '"' (end of string)
      local s = str:sub(i + 1, j - 1)
      if has_surrogate_escape then
        s = s:gsub("\\u[dD][89aAbB]..\\u....", parse_unicode_escape)
      end
      if has_unicode_escape then
        s = s:gsub("\\u....", parse_unicode_escape)
      end
      if has_escape then
        s = s:gsub("\\.", escape_char_map_inv)
      end
      return s, j + 1

    else
      last = x
    end
  end
  decode_error(str, i, "expected closing quote for string")
end


local function parse_number(str, i)
  local x = next_char(str, i, delim_chars)
  local s = str:sub(i, x - 1)
  local n = tonumber(s)
  if not n then
    decode_error(str, i, "invalid number '" .. s .. "'")
  end
  return n, x
end


local function parse_literal(str, i)
  local x = next_char(str, i, delim_chars)
  local word = str:sub(i, x - 1)
  if not literals[word] then
    decode_error(str, i, "invalid literal '" .. word .. "'")
  end
  return literal_map[word], x
end


local function parse_array(str, i)
  local res = {}
  local n = 1
  i = i + 1
  while 1 do
    local x
    i = next_char(str, i, space_chars, true)
    -- Empty / end of array?
    if str:sub(i, i) == "]" then
      i = i + 1
      break
    end
    -- Read token
    x, i = parse(str, i)
    res[n] = x
    n = n + 1
    -- Next token
    i = next_char(str, i, space_chars, true)
    local chr = str:sub(i, i)
    i = i + 1
    if chr == "]" then break end
    if chr ~= "," then decode_error(str, i, "expected ']' or ','") end
  end
  return res, i
end


local function parse_object(str, i)
  local res = {}
  i = i + 1
  while 1 do
    local key, val
    i = next_char(str, i, space_chars, true)
    -- Empty / end of object?
    if str:sub(i, i) == "}" then
      i = i + 1
      break
    end
    -- Read key
    if str:sub(i, i) ~= '"' then
      decode_error(str, i, "expected string for key")
    end
    key, i = parse(str, i)
    -- Read ':' delimiter
    i = next_char(str, i, space_chars, true)
    if str:sub(i, i) ~= ":" then
      decode_error(str, i, "expected ':' after key")
    end
    i = next_char(str, i + 1, space_chars, true)
    -- Read value
    val, i = parse(str, i)
    -- Set
    res[key] = val
    -- Next token
    i = next_char(str, i, space_chars, true)
    local chr = str:sub(i, i)
    i = i + 1
    if chr == "}" then break end
    if chr ~= "," then decode_error(str, i, "expected '}' or ','") end
  end
  return res, i
end


local char_func_map = {
  [ '"' ] = parse_string,
  [ "0" ] = parse_number,
  [ "1" ] = parse_number,
  [ "2" ] = parse_number,
  [ "3" ] = parse_number,
  [ "4" ] = parse_number,
  [ "5" ] = parse_number,
  [ "6" ] = parse_number,
  [ "7" ] = parse_number,
  [ "8" ] = parse_number,
  [ "9" ] = parse_number,
  [ "-" ] = parse_number,
  [ "t" ] = parse_literal,
  [ "f" ] = parse_literal,
  [ "n" ] = parse_literal,
  [ "[" ] = parse_array,
  [ "{" ] = parse_object,
}


parse = function(str, idx)
  local chr = str:sub(idx, idx)
  local f = char_func_map[chr]
  if f then
    return f(str, idx)
  end
  decode_error(str, idx, "unexpected character '" .. chr .. "'")
end


function json.decode(str)
  if type(str) ~= "string" then
    error("expected argument of type string, got " .. type(str))
  end
  local res, idx = parse(str, next_char(str, 1, space_chars, true))
  idx = next_char(str, idx, space_chars, true)
  if idx <= #str then
    decode_error(str, idx, "trailing garbage")
  end
  return res
end

end -- end of libraries

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
    if _G.config ~= nil then printError("Set http_enable to true in the CraftOS-PC configuration")
    else printError("Set http_enable to true in ComputerCraft.cfg") end
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
    for k, v in pairs(data.files) do jsonfiles = jsonfiles .. (jsonfiles == "" and "" or ",\n") .. ("    \"%s\": {\n      \"content\": %s\n    }"):format(k, json.encode(v.content)) end
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
    if handle.getResponseCode() ~= 201 or resp == nil then print("Failed: " .. handle.getResponseCode() .. ": " .. (resp and json.encode(resp) or "Unknown error")) handle.close() return 3 end
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
    for k, v in pairs(data.files) do jsonfiles = jsonfiles .. (jsonfiles == "" and "" or ",\n") .. (v.content == nil and ("    \"%s\": null"):format(k) or ("    \"%s\": {\n      \"content\": %s\n    }"):format(k, json.encode(v.content))) end
    local jsondata = ([[{
  "description": %s,
  "public": true,
  "files": {
%s
  }
}]]):format(data.description and '"' .. data.description .. '"' or "null", jsonfiles)
    write("Connecting to api.github.com... ")
    local handle
    if http.patch ~= nil then handle = http.patch("https://api.github.com/gists/" .. id, jsondata, headers)
    elseif http.websocket ~= nil then
        local _url = "https://api.github.com/gists/" .. id
        local ok, err = http.request(_url, jsondata, headers, false, "PATCH")
        if ok then
            while true do
                local event, param1, param2, param3 = os.pullEvent()
                if event == "http_success" and param1 == _url then
                    handle = param2
                    break
                elseif event == "http_failure" and param1 == _url then
                    print("Failed: ", param2, param3)
                    return 3
                end
            end
        else print("Failed: " .. err) return 3 end
    else print("Failed: This version of ComputerCraft doesn't support the PATCH method. Update to CC: Tweaked or a compatible emulator (CraftOS-PC, CCEmuX) to use 'edit'.") return 3 end
    if handle == nil then print("Failed.") return 3 end
    local resp = json.decode(handle.readAll())
    if handle.getResponseCode() ~= 200 or resp == nil then print("Failed: " .. handle.getResponseCode() .. ": " .. (resp and json.encode(resp) or "Unknown error")) handle.close() return 3 end
    handle.close()
    print("Success. Uploaded as " .. resp.html_url .. "\nRun 'gist get " .. resp.id .. "' to download anywhere")
elseif args[1] == "delete" then
    local id = args[2]
    if id:find("/") ~= nil or id:find(":") ~= nil then id = id:match("^([0-9A-Fa-f]+)") end
    local headers = {}
    requestAuth(headers)
    local handle
    write("Connecting to api.github.com... ")
    if http.delete ~= nil then handle = http.delete("https://api.github.com/gists/" .. id, nil, headers)
    elseif http.websocket ~= nil then
        local _url = "https://api.github.com/gists/" .. id
        local ok, err = http.request(_url, nil, headers, false, "DELETE")
        if ok then
            while true do
                local event, param1, param2, param3 = os.pullEvent()
                if event == "http_success" and param1 == _url then
                    handle = param2
                    break
                elseif event == "http_failure" and param1 == _url then
                    print("Failed: ", param2, param3)
                    return 3
                end
            end
        else print("Failed: " .. err) return 3 end
    else print("Failed: This version of ComputerCraft doesn't support the PATCH method. Update to CC: Tweaked or a compatible emulator (CraftOS-PC, CCEmuX) to use 'edit'.") return 3 end
    if handle == nil then print("Failed.") return 3 end
    if handle.getResponseCode() ~= 204 then print("Failed: " .. handle.getResponseCode() .. ".") handle.close() return 3 end
    handle.close()
    print("Success.")
    print("The requested Gist has been deleted.")
else print(helpstr) return 1 end