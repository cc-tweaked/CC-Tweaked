-- SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- Parse [SNBT] to Lua values.

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

[SNBT]: https://minecraft.wiki/w/NBT_format#SNBT_format

@local
]]

local sub, find, match, gsub, concat, tonumber = string.sub, string.find, string.match, string.gsub, table.concat, tonumber

--- Skip any whitespace
local function skip(str, pos)
    local _, last = find(str, "^[ \n\r\t]+", pos)
    if last then return last + 1 else return pos end
end

local escapes = {
    ["b"] = '\b', ["f"] = '\f', ["n"] = '\n', ["r"] = '\r', ["s"] = " ",
    ["t"] = '\t', ["\\"] = "\\", ["'"] = "'", ["\""] = "\"",
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

            if c == "x" then
                local num_str = match(str, "^%x%x", pos + 2)
                if not num_str then error_at(pos, "Malformed unicode escape %q.", sub(str, pos + 2, pos + 3)) end
                buf[n], n, pos = utf8.char(tonumber(num_str, 16)), n + 1, pos + 4
            elseif c == "u" then
                local num_str = match(str, "^%x%x%x%x", pos + 2)
                if not num_str then error_at(pos, "Malformed unicode escape %q.", sub(str, pos + 2, pos + 5)) end
                buf[n], n, pos = utf8.char(tonumber(num_str, 16)), n + 1, pos + 6
            elseif c == "U" then
                local num_str = match(str, "^%x%x%x%x%x%x%x%x", pos + 2)
                if not num_str then error_at(pos, "Malformed unicode escape %q.", sub(str, pos + 2, pos + 9)) end
                buf[n], n, pos = utf8.char(tonumber(num_str, 16)), n + 1, pos + 10
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

local function parse_int(str, pos, arr_int_width)
    local start = pos
    local c = sub(str, pos, pos)

    -- Parse the +- bit of the number
    local sign = c
    if sign == "+" or sign == "-" then
        pos = pos + 1
        c = sub(str, pos, pos)
    end

    -- Parse our 0x or 0b prefix.
    local radix, pattern = 10, "^[%d_]+"
    if c == "0" then
        pos = pos + 1
        c = sub(str, pos, pos)
        if c == "x" or c == "X" then
            radix, pattern = 16, "^[%x_]+"
        elseif c == "b" or c == "B" then
            radix, pattern = 2, "^[01_]+"
        else
            error_at(pos - 1, "Leading zero not allowed.")
        end

        pos = pos + 1
    end

    -- Parse the number, then check it is valid.
    local num_str = match(str, pattern, pos)
    if not num_str then return error_at(start, "Malformed number.") end

    if sub(num_str, 1, 1) == "_" or sub(num_str, -1, -1) == "_" then
        error_at(start, "Underscores only allowed between digits.")
    end

    local num = tonumber(gsub(num_str, "_", ""), radix)
    if not num then return error_at(start, "Malformed number.") end
    if sign == "-" then num = -num end

    pos = pos + #num_str

    -- Finally check our suffix.
    local _, last, signed, int_ty = find(str, "^([SsUu]?)([bBsSiIlL])", pos)
    if last then pos = last + 1 end

    -- If there's no signed suffix, then treat decimal numbers as signed, and
    -- hex/binary as unsigned.
    local is_signed
    if signed == nil or signed == "" then
        is_signed = radix == 10
    else
        is_signed = signed == "S" or signed == "s"
    end

    local int_width
    if int_ty == nil then int_width = arr_int_width or 32
    elseif int_ty == "I" or int_ty == "i" then int_width = 32
    elseif int_ty == "L" or int_ty == "l" then int_width = 64
    elseif int_ty == "S" or int_ty == "s" then int_width = 16
    else int_width = 8
    end

    if arr_int_width and int_width > arr_int_width then error_at(start, "Invalid integer type in array.") end

    if is_signed then
        if num >= -(2 ^ (int_width - 1)) and num < (2 ^ (int_width - 1)) then
            return num, pos
        end
    else
        local max = 2 ^ int_width
        if num >= 0 and num < max then
            if num >= (2 ^ (int_width - 1)) then num = num - max end
            return num, pos
        end
    end

    return error_at(start, "Integer not in range: %d.", num)
end

-- An "exact" parser for doubles would be very annoying (specifically handling
-- rules around underscores), so we approximate it with Lua patterns.
local double_patterns = {
    "^([+-]?[%d_]+%.[%d_]*[eE]?[-+%d_]*)[fFdD]?", -- Whole and decimal, and optional exponent and suffix.
    "^([+-]?%.[%d_]?[eE]?[-+%d_]*)[fFdD]?", -- Leading "." with optional exponent and suffix.
    "^([+-]?[%d_]+[eE][-+%d_]+)[fFdD]?", -- Required exponent, optional suffix.
    "^([+-]?[%d_]+[eE]?[-+%d_]*)[fFdD]", -- Optional exponent, required suffix.
}

local function parse_number(str, pos)
    for i = 1, #double_patterns do
        local _, last, num_str = find(str, double_patterns[i], pos)
        if last then
            local num = tonumber((gsub(num_str, "_", "")))
            if not num then error_at(pos, "Malformed number.") end
            return num, last + 1
        end
    end

    return parse_int(str, pos)
end

local function parse_ident(str, pos, err)
    local _, last, val = find(str, "^([%w_+.-]+)", pos)
    if not last then error_at(pos, err) end
    return val, last + 1
end

local arr_types = { I = 32, L = 64, B = 8 }
local function decode_impl(str, pos, opts)
    local c = sub(str, pos, pos)
    if c == '"' or c == "'" then return parse_string(str, pos + 1, c)
    elseif c == "+" or c == "-" or c == "." or (c >= "0" and c <= "9") then return parse_number(str, pos)
    elseif c == "_" or (c >= "A" and c <= "Z") or (c >= "a" and c <= "z") then
        local val, pos = parse_ident(str, pos, "Expected unquoted string.")
        if val == "true" then val = true
        elseif val == "false" then val = false
        end
        return val, pos
    elseif c == "{" then
        local obj = {}

        pos = skip(str, pos + 1)
        c = sub(str, pos, pos)

        while c ~= "}" do
            local key, value
            if c == "\"" or c == "'" then key, pos = parse_string(str, pos + 1, c)
            else key, pos = parse_ident(str, pos, "Expected object key.")
            end

            pos = skip(str, pos)

            c = sub(str, pos, pos)
            if c ~= ":" then return expected(pos, c, "':'") end

            value, pos = decode_impl(str, skip(str, pos + 1), opts)
            obj[key] = value

            -- Consume the next delimiter
            pos = skip(str, pos)
            c = sub(str, pos, pos)
            if c == "," then
                pos = skip(str, pos + 1)
                c = sub(str, pos, pos)
            else
                break
            end
        end

        if c ~= "}" then return expected(pos, c, "'}'") end
        return obj, pos + 1

    elseif c == "[" then
        local arr, n = {}, 1

        pos = skip(str, pos + 1)
        c = sub(str, pos, pos)

        local int_width
        if arr_types[c] and sub(str, pos + 1, pos + 1) == ";" then
            int_width = arr_types[c]
            pos = skip(str, pos + 2)
            c = sub(str, pos, pos)
        end

        if c == "]" then
            if opts.parse_empty_array ~= false then
                return textutils.empty_json_array, pos + 1
            else
                return {}, pos + 1
            end
        end

        while c ~= "]" do
            if int_width then
                arr[n], pos = parse_int(str, pos, int_width)
            else
                arr[n], pos = decode_impl(str, pos, opts)
            end
            n = n + 1

            -- Consume the next delimiter
            pos = skip(str, pos)
            c = sub(str, pos, pos)
            if c == "," then
                pos = skip(str, pos + 1)
                c = sub(str, pos, pos)
            else
                break
            end
        end

        if c ~= "]" then return expected(pos, c, "']'") end
        return arr, pos + 1
    elseif c == "" then error_at(pos, 'Unexpected end of input.')
    end

    error_at(pos, "Unexpected character %q.", c)
end

--[[- Parse a SNBT string to a Lua value.

@tparam string s The serialised string to deserialise.
@tparam[opt] { parse_null? = boolean, parse_empty_array? = boolean } options
Options which control how this SNBT value is parsed.
@return[1] The deserialised object
@treturn[2] nil If the object could not be deserialised.
@treturn string A message describing why the SNBT string is invalid.
]]
local function parse(s, options)
    local ok, res, pos = pcall(decode_impl, s, skip(s, 1), options)
    if not ok then
        if type(res) == "table" and getmetatable(res) == mt then
            return nil, ("Malformed SNBT at position %d: %s"):format(res.pos, res.msg)
        end

        error(res, 0)
    end

    pos = skip(s, pos)
    if pos <= #s then
        return nil, ("Malformed SNBT at position %d: Unexpected trailing character %q."):format(pos, sub(s, pos, pos))
    end
    return res
end

return { parse = parse }
