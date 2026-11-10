-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--- Helpful utilities for formatting and manipulating strings.
--
-- @module textutils
-- @since 1.2

local require = dofile("rom/modules/main/cc/internal/tiny_require.lua")

local expect = require("cc.expect")
local expect, field = expect.expect, expect.field
local wrap = require("cc.strings").wrap

--- Slowly writes string text at current cursor position,
-- character-by-character.
--
-- Like [`_G.write`], this does not insert a newline at the end.
--
-- @tparam string text The the text to write to the screen
-- @tparam[opt] number rate The number of characters to write each second,
-- Defaults to 20.
-- @usage textutils.slowWrite("Hello, world!")
-- @usage textutils.slowWrite("Hello, world!", 5)
-- @since 1.3
function slowWrite(text, rate)
    expect(2, rate, "number", "nil")
    rate = rate or 20
    if rate < 0 then
        error("Rate must be positive", 2)
    end
    local to_sleep = 1 / rate

    local wrapped_lines = wrap(tostring(text), (term.getSize()))
    local wrapped_str = table.concat(wrapped_lines, "\n")

    for n = 1, #wrapped_str do
        sleep(to_sleep)
        write(wrapped_str:sub(n, n))
    end
end

--- Slowly prints string text at current cursor position,
-- character-by-character.
--
-- Like [`print`], this inserts a newline after printing.
--
-- @tparam string sText The the text to write to the screen
-- @tparam[opt] number nRate The number of characters to write each second,
-- Defaults to 20.
-- @usage textutils.slowPrint("Hello, world!")
-- @usage textutils.slowPrint("Hello, world!", 5)
function slowPrint(sText, nRate)
    slowWrite(sText, nRate)
    print()
end

--- Takes input time and formats it in a more readable format such as `6:30 PM`.
--
-- @tparam number nTime The time to format, as provided by [`os.time`].
-- @tparam[opt] boolean bTwentyFourHour Whether to format this as a 24-hour
-- clock (`18:30`) rather than a 12-hour one (`6:30 AM`)
-- @treturn string The formatted time
-- @usage Print the current in-game time as a 12-hour clock.
--
--     textutils.formatTime(os.time())
-- @usage Print the local time as a 24-hour clock.
--
--     textutils.formatTime(os.time("local"), true)
function formatTime(nTime, bTwentyFourHour)
    expect(1, nTime, "number")
    expect(2, bTwentyFourHour, "boolean", "nil")
    local sTOD = nil
    if not bTwentyFourHour then
        if nTime >= 12 then
            sTOD = "PM"
        else
            sTOD = "AM"
        end
        if nTime >= 13 then
            nTime = nTime - 12
        end
    end

    local nHour = math.floor(nTime)
    local nMinute = math.floor((nTime - nHour) * 60)
    if sTOD then
        return string.format("%d:%02d %s", nHour == 0 and 12 or nHour, nMinute, sTOD)
    else
        return string.format("%d:%02d", nHour, nMinute)
    end
end

local function makePagedScroll(_term, _nFreeLines)
    local nativeScroll = _term.scroll
    local nFreeLines = _nFreeLines or 0
    return function(_n)
        for _ = 1, _n do
            nativeScroll(1)

            if nFreeLines <= 0 then
                local _, h = _term.getSize()
                _term.setCursorPos(1, h)
                _term.write("Press any key to continue")
                os.pullEvent("key")
                _term.clearLine()
                _term.setCursorPos(1, h)
            else
                nFreeLines = nFreeLines - 1
            end
        end
    end
end

--[[- Prints a given string to the display.

If the action can be completed without scrolling, it acts much the same as
[`print`]; otherwise, it will throw up a "Press any key to continue" prompt at
the bottom of the display. Each press will cause it to scroll down and write a
single line more before prompting again, if need be.

@tparam string text The text to print to the screen.
@tparam[opt] number free_lines The number of lines which will be
automatically scrolled before the first prompt appears (meaning free_lines +
1 lines will be printed). This can be set to the cursor's y position - 2 to
always try to fill the screen. Defaults to 0, meaning only one line is
displayed before prompting.
@treturn number The number of lines printed.

@usage Generates several lines of text and then prints it, paging once the
bottom of the terminal is reached.

    local lines = {}
    for i = 1, 30 do lines[i] = ("This is line #%d"):format(i) end
    local message = table.concat(lines, "\n")

    local width, height = term.getCursorPos()
    textutils.pagedPrint(message, height - 2)
]]
function pagedPrint(text, free_lines)
    expect(2, free_lines, "number", "nil")
    -- Setup a redirector
    local oldTerm = term.current()
    local newTerm = {}
    for k, v in pairs(oldTerm) do
        newTerm[k] = v
    end

    newTerm.scroll = makePagedScroll(oldTerm, free_lines)
    term.redirect(newTerm)

    -- Print the text
    local result
    local ok, err = pcall(function()
        if text ~= nil then
            result = print(text)
        else
            result = print()
        end
    end)

    -- Removed the redirector
    term.redirect(oldTerm)

    -- Propagate errors
    if not ok then
        error(err, 0)
    end
    return result
end

local function tabulateCommon(bPaged, ...)
    local tAll = table.pack(...)
    for i = 1, tAll.n do
        expect(i, tAll[i], "number", "table")
    end

    local w, h = term.getSize()
    local nMaxLen = w / 8
    for n, t in ipairs(tAll) do
        if type(t) == "table" then
            for nu, sItem in pairs(t) do
                local ty = type(sItem)
                if ty ~= "string" and ty ~= "number" then
                    error("bad argument #" .. n .. "." .. nu .. " (string expected, got " .. ty .. ")", 3)
                end
                nMaxLen = math.max(#tostring(sItem) + 1, nMaxLen)
            end
        end
    end
    local nCols = math.floor(w / nMaxLen)
    local nLines = 0
    local function newLine()
        if bPaged and nLines >= h - 3 then
            pagedPrint()
        else
            print()
        end
        nLines = nLines + 1
    end

    local function drawCols(_t)
        local nCol = 1
        for _, s in ipairs(_t) do
            if nCol > nCols then
                nCol = 1
                newLine()
            end

            local cx, cy = term.getCursorPos()
            cx = 1 + (nCol - 1) * nMaxLen
            term.setCursorPos(cx, cy)
            term.write(s)

            nCol = nCol + 1
        end
        print()
    end

    local previous_colour = term.getTextColour()
    for _, t in ipairs(tAll) do
        if type(t) == "table" then
            if #t > 0 then
                drawCols(t)
            end
        elseif type(t) == "number" then
            term.setTextColor(t)
        end
    end
    term.setTextColor(previous_colour)
end

--[[- Prints tables in a structured form.

This accepts multiple arguments, either a table or a number. When
encountering a table, this will be treated as a table row, with each column
width being auto-adjusted.

When encountering a number, this sets the text color of the subsequent rows to it.

@tparam {string...}|number ... The rows and text colors to display.
@since 1.3
@usage

    textutils.tabulate(
      colors.orange, { "1", "2", "3" },
      colors.lightBlue, { "A", "B", "C" }
    )
]]
function tabulate(...)
    return tabulateCommon(false, ...)
end

--[[- Prints tables in a structured form, stopping and prompting for input should
the result not fit on the terminal.

This functions identically to [`textutils.tabulate`], but will prompt for user
input should the whole output not fit on the display.

@tparam {string...}|number ... The rows and text colors to display.
@see textutils.tabulate
@see textutils.pagedPrint
@since 1.3

@usage Generates a long table, tabulates it, and prints it to the screen.

    local rows = {}
    for i = 1, 30 do rows[i] = {("Row #%d"):format(i), math.random(1, 400)} end

    textutils.pagedTabulate(colors.orange, {"Column", "Value"}, colors.lightBlue, table.unpack(rows))
]]
function pagedTabulate(...)
    return tabulateCommon(true, ...)
end

local g_tLuaKeywords = {
    ["and"] = true,
    ["break"] = true,
    ["do"] = true,
    ["else"] = true,
    ["elseif"] = true,
    ["end"] = true,
    ["false"] = true,
    ["for"] = true,
    ["function"] = true,
    ["if"] = true,
    ["in"] = true,
    ["local"] = true,
    ["nil"] = true,
    ["not"] = true,
    ["or"] = true,
    ["repeat"] = true,
    ["return"] = true,
    ["then"] = true,
    ["true"] = true,
    ["until"] = true,
    ["while"] = true,
}

--- A version of the ipairs iterator which ignores metamethods
local function inext(tbl, i)
    i = (i or 0) + 1
    local v = rawget(tbl, i)
    if v == nil then return nil else return i, v end
end

local serialize_infinity = math.huge
local function serialize_impl(t, tracking, indent, opts)
    local sType = type(t)
    if sType == "table" then
        if tracking[t] ~= nil then
            if tracking[t] == false then
                error("Cannot serialize table with repeated entries", 0)
            else
                error("Cannot serialize table with recursive entries", 0)
            end
        end
        tracking[t] = true

        local result
        if next(t) == nil then
            -- Empty tables are simple
            result = "{}"
        else
            -- Other tables take more work
            local open, sub_indent, open_key, close_key, equal, comma = "{\n", indent .. "  ", "[ ", " ] = ", " = ", ",\n"
            if opts.compact then
                open, sub_indent, open_key, close_key, equal, comma = "{", "", "[", "]=", "=", ","
            end

            result = open
            local seen_keys = {}
            for k, v in inext, t do
                seen_keys[k] = true
                result = result .. sub_indent .. serialize_impl(v, tracking, sub_indent, opts) .. comma
            end
            for k, v in next, t do
                if not seen_keys[k] then
                    local sEntry
                    if type(k) == "string" and not g_tLuaKeywords[k] and string.match(k, "^[%a_][%a%d_]*$") then
                        sEntry = k .. equal .. serialize_impl(v, tracking, sub_indent, opts) .. comma
                    else
                        sEntry = open_key .. serialize_impl(k, tracking, sub_indent, opts) .. close_key .. serialize_impl(v, tracking, sub_indent, opts) .. comma
                    end
                    result = result .. sub_indent .. sEntry
                end
            end
            result = result .. indent .. "}"
        end

        if opts.allow_repetitions then
            tracking[t] = nil
        else
            tracking[t] = false
        end
        return result

    elseif sType == "string" then
        return string.format("%q", t)

    elseif sType == "number" then
        if t ~= t then --nan
            return "0/0"
        elseif t == serialize_infinity then
            return "1/0"
        elseif t == -serialize_infinity then
            return "-1/0"
        else
            return tostring(t)
        end

    elseif sType == "boolean" or sType == "nil" then
        return tostring(t)

    else
        error("Cannot serialize type " .. sType, 0)

    end
end

local function mk_tbl(str, name)
    local msg = "attempt to mutate textutils." .. name
    return setmetatable({}, {
        __newindex = function() error(msg, 2) end,
        __tostring = function() return str end,
    })
end

--- A table representing an empty JSON array, in order to distinguish it from an
-- empty JSON object.
--
-- The contents of this table should not be modified.
--
-- @usage textutils.serialiseJSON(textutils.empty_json_array)
-- @see textutils.serialiseJSON
-- @see textutils.unserialiseJSON
empty_json_array = mk_tbl("[]", "empty_json_array")

--- A table representing the JSON null value.
--
-- The contents of this table should not be modified.
--
-- @usage textutils.serialiseJSON(textutils.json_null)
-- @see textutils.serialiseJSON
-- @see textutils.unserialiseJSON
json_null = mk_tbl("null", "json_null")

local serializeJSONString
do
    local function hexify(c)
        return ("\\u00%02X"):format(c:byte())
    end

    local map = {
        ["\""] = "\\\"",
        ["\\"] = "\\\\",
        ["\b"] = "\\b",
        ["\f"] = "\\f",
        ["\n"] = "\\n",
        ["\r"] = "\\r",
        ["\t"] = "\\t",
    }
    for i = 0, 0x1f do
        local c = string.char(i)
        if map[c] == nil then map[c] = hexify(c) end
    end

    serializeJSONString = function(s, options)
        if options and options.unicode_strings and s:find("[\x80-\xff]") then
            local retval = '"'
            for _, code in utf8.codes(s) do
                if code > 0xFFFF then
                    -- Encode the codepoint as a UTF-16 surrogate pair
                    code = code - 0x10000
                    local high, low = bit32.extract(code, 10, 10) + 0xD800, bit32.extract(code, 0, 10) + 0xDC00
                    retval = retval .. ("\\u%04X\\u%04X"):format(high, low)
                elseif code <= 0x5C and map[string.char(code)] then -- 0x5C = `\`, don't run `string.char` if we don't need to
                    retval = retval .. map[string.char(code)]
                elseif code < 0x20 or code >= 0x7F then
                    retval = retval .. ("\\u%04X"):format(code)
                else
                    retval = retval .. string.char(code)
                end
            end
            return retval .. '"'
        else
            return ('"%s"'):format(s:gsub("[\0-\x1f\"\\]", map):gsub("[\x7f-\xff]", hexify))
        end
    end
end

local function serializeJSONImpl(t, tracking, options)
    local sType = type(t)
    if t == empty_json_array then return "[]"
    elseif t == json_null then return "null"

    elseif sType == "table" then
        if tracking[t] ~= nil then
            if tracking[t] == false then
                error("Cannot serialize table with repeated entries", 0)
            else
                error("Cannot serialize table with recursive entries", 0)
            end
        end
        tracking[t] = true

        local result
        if next(t) == nil then
            -- Empty tables are simple
            result = "{}"
        else
            -- Other tables take more work
            local sObjectResult = "{"
            local sArrayResult = "["
            local nObjectSize = 0
            local nArraySize = 0
            local largestArrayIndex = 0
            local bNBTStyle = options.nbt_style
            for k, v in pairs(t) do
                if type(k) == "string" then
                    local sEntry
                    if bNBTStyle then
                        sEntry = tostring(k) .. ":" .. serializeJSONImpl(v, tracking, options)
                    else
                        sEntry = serializeJSONString(k, options) .. ":" .. serializeJSONImpl(v, tracking, options)
                    end
                    if nObjectSize == 0 then
                        sObjectResult = sObjectResult .. sEntry
                    else
                        sObjectResult = sObjectResult .. "," .. sEntry
                    end
                    nObjectSize = nObjectSize + 1
                elseif type(k) == "number" and k > largestArrayIndex then --the largest index is kept to avoid losing half the array if there is any single nil in that array
                    largestArrayIndex = k
                end
            end
            for k = 1, largestArrayIndex, 1 do --the array is read up to the very last valid array index, ipairs() would stop at the first nil value and we would lose any data after.
                local sEntry
                if t[k] == nil then --if the array is nil at index k the value is "null" as to keep the unused indexes in between used ones.
                    sEntry = "null"
                else -- if the array index does not point to a nil we serialise it's content.
                    sEntry = serializeJSONImpl(t[k], tracking, options)
                end
                if nArraySize == 0 then
                    sArrayResult = sArrayResult .. sEntry
                else
                    sArrayResult = sArrayResult .. "," .. sEntry
                end
                nArraySize = nArraySize + 1
            end
            sObjectResult = sObjectResult .. "}"
            sArrayResult = sArrayResult .. "]"
            if nObjectSize > 0 or nArraySize == 0 then
                result = sObjectResult
            else
                result = sArrayResult
            end
        end

        if options.allow_repetitions then
            tracking[t] = nil
        else
            tracking[t] = false
        end
        return result

    elseif sType == "string" then
        return serializeJSONString(t, options)

    elseif sType == "number" or sType == "boolean" then
        return tostring(t)

    else
        error("Cannot serialize type " .. sType, 0)

    end
end


--[[- Converts a serialised JSON string back into a reassembled Lua object.

This may be used with [`textutils.serializeJSON`], or when communicating
with command blocks or web APIs.

If a `null` value is encountered, it is converted into `nil`. It can be converted
into [`textutils.json_null`] with the `parse_null` option.

If an empty array is encountered, it is converted into [`textutils.empty_json_array`].
It can be converted into a new empty table with the `parse_empty_array` option.

@tparam string s The serialised string to deserialise.
@tparam[opt] { nbt_style? = boolean, parse_null? = boolean, parse_empty_array? = boolean } options
Options which control how this JSON object is parsed.

- `nbt_style`: When true, this will accept [stringified NBT][nbt] strings,
    as produced by many commands.

    Unlike JSON, the format of SNBT strings changes between Minecraft versions.
    This function should parse all SNBT strings returned by commands, but may
    not accept *all* valid SNBT strings. For instance, the `bool` and `uuid`
    functions are not currently supported.
- `parse_null`: When true, `null` will be parsed as [`json_null`], rather than
    `nil`.
- `parse_empty_array`: When false, empty arrays will be parsed as a new table.
    By default (or when this value is true), they are parsed as [`empty_json_array`].

[nbt]: https://minecraft.wiki/w/NBT_format#SNBT_format
@return[1] The deserialised object
@treturn[2] nil If the object could not be deserialised.
@treturn string A message describing why the JSON string is invalid.
@since 1.87.0
@changed 1.100.6 Added `parse_empty_array` option
@see textutils.json_null Use to serialize a JSON `null` value.
@see textutils.empty_json_array Use to serialize a JSON empty array.
@usage Unserialise a basic JSON object

    textutils.unserialiseJSON('{"name": "Steve", "age": null}')

@usage Unserialise a basic JSON object, returning null values as [`json_null`].

    textutils.unserialiseJSON('{"name": "Steve", "age": null}', { parse_null = true })
]]
local function unserialise_json(s, options)
    expect(1, s, "string")
    expect(2, options, "table", "nil")

    if options then
        field(options, "nbt_style", "boolean", "nil")
        field(options, "parse_null", "boolean", "nil")
        field(options, "parse_empty_array", "boolean", "nil")
    else
        options = {}
    end

    if options.nbt_style then
        return require("cc.internal.snbt").parse(s, options)
    else
        return require("cc.internal.json").parse(s, options)
    end
end

--[[- Convert a Lua object into a textual representation, suitable for
saving in a file or pretty-printing.

@param t The object to serialise
@tparam { compact? = boolean, allow_repetitions? = boolean } opts Options for serialisation.
 - `compact`: Do not emit indentation and other whitespace between terms.
 - `allow_repetitions`: Relax the check for recursive tables, allowing them to appear multiple
   times (as long as tables do not appear inside themselves).

@treturn string The serialised representation
@throws If the object contains a value which cannot be
serialised. This includes functions and tables which appear multiple
times.
@see cc.pretty.pretty_print An alternative way to display a table, often more
suitable for pretty printing.
@since 1.3
@changed 1.97.0 Added `opts` argument.
@usage Serialise a basic table.

    textutils.serialise({ 1, 2, 3, a = 1, ["another key"] = { true } })

@usage Demonstrates some of the other options

    local tbl = { 1, 2, 3 }
    print(textutils.serialise({ tbl, tbl }, { allow_repetitions = true }))

    print(textutils.serialise(tbl, { compact = true }))
]]
function serialize(t, opts)
    local tTracking = {}
    expect(2, opts, "table", "nil")

    if opts then
        field(opts, "compact", "boolean", "nil")
        field(opts, "allow_repetitions", "boolean", "nil")
    else
        opts = {}
    end
    return serialize_impl(t, tTracking, "", opts)
end

serialise = serialize -- GB version

--- Converts a serialised string back into a reassembled Lua object.
--
-- This is mainly used together with [`textutils.serialise`].
--
-- @tparam string s The serialised string to deserialise.
-- @return[1] The deserialised object
-- @treturn[2] nil If the object could not be deserialised.
-- @since 1.3
function unserialize(s)
    expect(1, s, "string")
    local func = load("return " .. s, "unserialize", "t", {})
    if func then
        local ok, result = pcall(func)
        if ok then
            return result
        end
    end
    return nil
end

unserialise = unserialize -- GB version

--[[- Returns a JSON representation of the given data.

This is largely intended for interacting with various functions from the
[`commands`] API, though may also be used in making [`http`] requests.

Lua has a rather different data model to Javascript/JSON. As a result, some Lua
values do not serialise cleanly into JSON.

 - Lua tables can contain arbitrary key-value pairs, but JSON only accepts arrays,
   and objects (which require a string key). When serialising a table, if it only
   has numeric keys, then it will be treated as an array. Otherwise, the table will
   be serialised to an object using the string keys. Non-string keys (such as numbers
   or tables) will be dropped.

   A consequence of this is that an empty table will always be serialised to an object,
   not an array. [`textutils.empty_json_array`] may be used to express an empty array.

 - Lua strings are a sequence of raw bytes, and do not have any specific encoding.
   However, JSON strings must be valid unicode. By default, non-ASCII characters in a
   string are serialised to their unicode code point (for instance, `"\xfe"` is
   converted to `"\u00fe"`). The `unicode_strings` option may be set to treat all input
   strings as UTF-8.

 - Lua does not distinguish between missing keys (`undefined` in JS) and ones explicitly
   set to `null`. As a result `{ x = nil }` is serialised to `{}`. [`textutils.json_null`]
   may be used to get an explicit null value (`{ x = textutils.json_null }` will serialise
   to `{"x": null}`).

@param[1] t The value to serialise. Like [`textutils.serialise`], this should not
contain recursive tables or functions.
@tparam[1,opt] {
    nbt_style? = boolean,
    unicode_strings? = boolean,
    allow_repetitions? = boolean
} options Options for serialisation.
 - `nbt_style`: Whether to produce NBT-style JSON (non-quoted keys) instead of standard JSON.
 - `unicode_strings`: Whether to treat strings as containing UTF-8 characters instead of
    using the default 8-bit character set.
 - `allow_repetitions`: Relax the check for recursive tables, allowing them to appear multiple
   times (as long as tables do not appear inside themselves).

@param[2] t The value to serialise. Like [`textutils.serialise`], this should not
contain recursive tables or functions.
@tparam[2] boolean bNBTStyle Whether to produce NBT-style JSON (non-quoted keys)
instead of standard JSON.

@treturn string The JSON representation of the input.
@throws If the object contains a value which cannot be serialised. This includes
functions and tables which appear multiple times.

@usage Serialise a simple object

    textutils.serialiseJSON({ values = { 1, "2", true } })

@usage Serialise an object to a NBT-style string

    textutils.serialiseJSON({ values = { 1, "2", true } }, { nbt_style = true })

@since 1.7
@changed 1.106.0 Added `options` overload and `unicode_strings` option.
@changed 1.109.0 Added `allow_repetitions` option.

@see textutils.json_null Use to serialise a JSON `null` value.
@see textutils.empty_json_array Use to serialise a JSON empty array.
]]
function serializeJSON(t, options)
    expect(1, t, "table", "string", "number", "boolean")
    expect(2, options, "table", "boolean", "nil")
    if type(options) == "boolean" then
        options = { nbt_style = options }
    elseif type(options) == "table" then
        field(options, "nbt_style", "boolean", "nil")
        field(options, "unicode_strings", "boolean", "nil")
        field(options, "allow_repetitions", "boolean", "nil")
    else
        options = {}
    end

    local tTracking = {}
    return serializeJSONImpl(t, tTracking, options)
end

serialiseJSON = serializeJSON -- GB version

unserializeJSON = unserialise_json
unserialiseJSON = unserialise_json

--- Replaces certain characters in a string to make it safe for use in URLs or POST data.
--
-- @tparam string str The string to encode
-- @treturn string The encoded string.
-- @usage print("https://example.com/?view=" .. textutils.urlEncode("some text&things"))
-- @since 1.31
function urlEncode(str)
    expect(1, str, "string")
    local gsub, byte, format, band, arshift = string.gsub, string.byte, string.format, bit32.band, bit32.arshift

    str = gsub(str, "\n", "\r\n")
    str = gsub(str, "[^A-Za-z0-9%-%_%.]", function(c)
        if c == " " then return "+" end

        local n = byte(c)
        if n < 128 then
            -- ASCII
            return format("%%%02X", n)
        else
            -- Non-ASCII (encode as UTF-8)
            return format("%%%02X%%%02X", 192 + band(arshift(n, 6), 31), 128 + band(n, 63))
        end
    end)
    return str
end

local tEmpty = {}

--- Provides a list of possible completions for a partial Lua expression.
--
-- If the completed element is a table, suggestions will have `.` appended to
-- them. Similarly, functions have `(` appended to them.
--
-- @tparam string sSearchText The partial expression to complete, such as a
-- variable name or table index.
--
-- @tparam[opt] table tSearchTable The table to find variables in, defaulting to
-- the global environment ([`_G`]). The function also searches the "parent"
-- environment via the `__index` metatable field.
--
-- @treturn { string... } The (possibly empty) list of completions.
-- @see shell.setCompletionFunction
-- @see _G.read
-- @usage textutils.complete( "pa", _ENV )
-- @since 1.74
function complete(sSearchText, tSearchTable)
    expect(1, sSearchText, "string")
    expect(2, tSearchTable, "table", "nil")

    if g_tLuaKeywords[sSearchText] then return tEmpty end
    local nStart = 1
    local nDot = string.find(sSearchText, ".", nStart, true)
    local tTable = tSearchTable or _ENV
    while nDot do
        local sPart = string.sub(sSearchText, nStart, nDot - 1)
        local value = tTable[sPart]
        if type(value) == "table" then
            tTable = value
            nStart = nDot + 1
            nDot = string.find(sSearchText, ".", nStart, true)
        else
            return tEmpty
        end
    end
    local nColon = string.find(sSearchText, ":", nStart, true)
    if nColon then
        local sPart = string.sub(sSearchText, nStart, nColon - 1)
        local value = tTable[sPart]
        if type(value) == "table" then
            tTable = value
            nStart = nColon + 1
        else
            return tEmpty
        end
    end

    local sPart = string.sub(sSearchText, nStart)
    local nPartLength = #sPart

    local tResults = {}
    local tSeen = {}
    while tTable do
        for k, v in pairs(tTable) do
            if not tSeen[k] and type(k) == "string" then
                if string.find(k, sPart, 1, true) == 1 then
                    if not g_tLuaKeywords[k] and string.match(k, "^[%a_][%a%d_]*$") then
                        local sResult = string.sub(k, nPartLength + 1)
                        if nColon then
                            if type(v) == "function" then
                                table.insert(tResults, sResult .. "(")
                            elseif type(v) == "table" then
                                local tMetatable = getmetatable(v)
                                if tMetatable and (type(tMetatable.__call) == "function" or  type(tMetatable.__call) == "table") then
                                    table.insert(tResults, sResult .. "(")
                                end
                            end
                        else
                            if type(v) == "function" then
                                sResult = sResult .. "("
                            elseif type(v) == "table" and next(v) ~= nil then
                                sResult = sResult .. "."
                            end
                            table.insert(tResults, sResult)
                        end
                    end
                end
            end
            tSeen[k] = true
        end
        local tMetatable = getmetatable(tTable)
        if tMetatable and type(tMetatable.__index) == "table" then
            tTable = tMetatable.__index
        else
            tTable = nil
        end
    end

    table.sort(tResults)
    return tResults
end
