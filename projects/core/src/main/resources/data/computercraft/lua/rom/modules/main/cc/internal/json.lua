-- SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- Parse [JSON] to Lua values.

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

[JSON]: https://www.json.org/json-en.html

@local
]]

local sub, find, match, concat, tonumber = string.sub, string.find, string.match, table.concat, tonumber

--- Skip any whitespace
local function skip(str, pos)
    local _, last = find(str, "^[ \n\r\t]+", pos)
    if last then return last + 1 else return pos end
end

local escapes = {
    ["b"] = '\b', ["f"] = '\f', ["n"] = '\n', ["r"] = '\r', ["t"] = '\t',
    ["\""] = "\"", ["/"] = "/", ["\\"] = "\\",
}

local mt = {}

local function error_at(pos, msg, ...)
    if select('#', ...) > 0 then msg = msg:format(...) end
    error(setmetatable({ pos = pos, msg = msg }, mt))
end

local function expected(pos, actual, exp)
    if actual == "" then actual = "end of input" else actual = ("%q"):format(actual) end
    error_at(pos, "Unexpected %s, expected %s.", actual, exp)
end

local function parse_string(str, pos, terminate)
    local buf, n = {}, 1

    -- We attempt to match all non-special characters at once using Lua patterns, as this
    -- provides a significant speed boost. This is all characters >= " " except \ and the
    -- terminator (' or ").
    local char_pat = "^[ !#-[%]^-\255]+"
    if terminate == "'" then char_pat = "^[ -&(-[%]^-\255]+" end

    while true do
        local c = sub(str, pos, pos)
        if c == "" then error_at(pos, "Unexpected end of input, expected '\"'.") end
        if c == terminate then break end

        if c == "\\" then
            -- Handle the various escapes
            c = sub(str, pos + 1, pos + 1)
            if c == "" then error_at(pos, "Unexpected end of input, expected escape sequence.") end

            if c == "u" then
                local num_str = match(str, "^%x%x%x%x", pos + 2)
                if not num_str then error_at(pos, "Malformed unicode escape %q.", sub(str, pos + 2, pos + 5)) end
                buf[n], n, pos = utf8.char(tonumber(num_str, 16)), n + 1, pos + 6
            else
                local unesc = escapes[c]
                if not unesc then error_at(pos + 1, "Unknown escape character %q.", c) end
                buf[n], n, pos = unesc, n + 1, pos + 2
            end
        elseif c >= " " then
            local _, finish = find(str, char_pat, pos)
            buf[n], n = sub(str, pos, finish), n + 1
            pos = finish + 1
        else
            error_at(pos + 1, "Unescaped whitespace %q.", c)
        end
    end

    return concat(buf, "", 1, n - 1), pos + 1
end

local function parse_number(str, pos)
    local _, last, num_str = find(str, '^(-?%d+%.?%d*[eE]?[+-]?%d*)', pos)
    local val = tonumber(num_str)
    if not val then error_at(pos, "Malformed number %q.", num_str) end

    return val, last + 1
end

local function decode_impl(str, pos, opts)
    local c = sub(str, pos, pos)
    if c == '"' then return parse_string(str, pos + 1, '"')
    elseif c == "-" or c >= "0" and c <= "9" then return parse_number(str, pos)
    elseif c == "t" then
        if sub(str, pos + 1, pos + 3) == "rue" then return true, pos + 4 end
    elseif c == 'f' then
        if sub(str, pos + 1, pos + 4) == "alse" then return false, pos + 5 end
    elseif c == 'n' then
        if sub(str, pos + 1, pos + 3) == "ull" then
            if opts.parse_null then
                return textutils.json_null, pos + 4
            else
                return nil, pos + 4
            end
        end
    elseif c == "{" then
        local obj = {}

        pos = skip(str, pos + 1)
        c = sub(str, pos, pos)

        if c == "" then return error_at(pos, "Unexpected end of input, expected '}'.") end
        if c == "}" then return obj, pos + 1 end

        while true do
            local key, value
            if c == "\"" then key, pos = parse_string(str, pos + 1, "\"")
            else return expected(pos, c, "object key")
            end

            pos = skip(str, pos)

            c = sub(str, pos, pos)
            if c ~= ":" then return expected(pos, c, "':'") end

            value, pos = decode_impl(str, skip(str, pos + 1), opts)
            obj[key] = value

            -- Consume the next delimiter
            pos = skip(str, pos)
            c = sub(str, pos, pos)
            if c == "}" then break
            elseif c == "," then pos = skip(str, pos + 1)
            else return expected(pos, c, "',' or '}'")
            end

            c = sub(str, pos, pos)
        end

        return obj, pos + 1

    elseif c == "[" then
        local arr, n = {}, 1

        pos = skip(str, pos + 1)
        c = sub(str, pos, pos)

        if c == "" then return expected(pos, c, "']'") end
        if c == "]" then
            if opts.parse_empty_array ~= false then
                return textutils.empty_json_array, pos + 1
            else
                return {}, pos + 1
            end
        end

        while true do
            n, arr[n], pos = n + 1, decode_impl(str, pos, opts)

            -- Consume the next delimiter
            pos = skip(str, pos)
            c = sub(str, pos, pos)
            if c == "]" then break
            elseif c == "," then pos = skip(str, pos + 1)
            else return expected(pos, c, "',' or ']'")
            end
        end

        return arr, pos + 1
    elseif c == "" then error_at(pos, 'Unexpected end of input.')
    end

    error_at(pos, "Unexpected character %q.", c)
end

--[[- Parse a JSON string to a Lua value.

@tparam string s The serialised string to deserialise.
@tparam[opt] { parse_null? = boolean, parse_empty_array? = boolean } options
Options which control how this JSON value is parsed.
@return[1] The deserialised object
@treturn[2] nil If the object could not be deserialised.
@treturn string A message describing why the JSON string is invalid.
]]
local function parse(s, options)
    local ok, res, pos = pcall(decode_impl, s, skip(s, 1), options)
    if not ok then
        if type(res) == "table" and getmetatable(res) == mt then
            return nil, ("Malformed JSON at position %d: %s"):format(res.pos, res.msg)
        end

        error(res, 0)
    end

    pos = skip(s, pos)
    if pos <= #s then
        return nil, ("Malformed JSON at position %d: Unexpected trailing character %q."):format(pos, sub(s, pos, pos))
    end
    return res
end

return { parse = parse }
