-- SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--- Various utilities for working with strings and text.
--
-- @module cc.strings
-- @since 1.95.0
-- @see textutils For additional string related utilities.

local expect = require("cc.expect")
local expect, range = expect.expect, expect.range

local utf8_len = utf8.len
local utf8_offset = utf8.offset
local string_sub = string.sub

local function text_len(text)
    return utf8_len(text) or #text
end

local function text_sub(text, i, j)
    local len = utf8_len(text)
    if not len then
        return string_sub(text, i, j)
    end

    i = i or 1
    j = j or len

    if i < 1 then i = 1 end
    if j > len then j = len end
    if i > j or i > len then return "" end

    local start_byte = utf8_offset(text, i)
    if not start_byte then return "" end

    local end_byte = utf8_offset(text, j + 1)
    if end_byte then
        return string_sub(text, start_byte, end_byte - 1)
    else
        return string_sub(text, start_byte)
    end
end

--[[- Wraps a block of text, so that each line fits within the given width.

This may be useful if you want to wrap text before displaying it to a
[`monitor`] or [`printer`] without using [print][`_G.print`].

@tparam string text The string to wrap.
@tparam[opt] number width The width to constrain to, defaults to the width of
the terminal.
@treturn { string... } The wrapped input string as a list of lines.
@usage Wrap a string and write it to the terminal.

    term.clear()
    local lines = require "cc.strings".wrap("This is a long piece of text", 10)
    for i = 1, #lines do
      term.setCursorPos(1, i)
      term.write(lines[i])
    end
]]
local function wrap(text, width)
    expect(1, text, "string")
    expect(2, width, "number", "nil")
    width = width or term.getSize()
    range(width, 1)

    local lines, lines_n, current_line = {}, 0, ""
    local function push_line()
        lines_n = lines_n + 1
        lines[lines_n] = current_line
        current_line = ""
    end

    local pos, length = 1, #text
    local sub, match = string.sub, string.match
    while pos <= length do
        local head = sub(text, pos, pos)
        if head == " " or head == "\t" then
            local whitespace = match(text, "^[ \t]+", pos)
            current_line = current_line .. whitespace
            pos = pos + #whitespace
        elseif head == "\n" then
            push_line()
            pos = pos + 1
        else
            local word = match(text, "^[^ \t\n]+", pos)
            pos = pos + #word
            if text_len(word) > width then
                -- Print a multiline word
                while text_len(word) > 0 do
                    local space_remaining = width - text_len(current_line) - 1
                    if space_remaining <= 0 then
                        push_line()
                        space_remaining = width
                    end

                    current_line = current_line .. text_sub(word, 1, space_remaining)
                    word = text_sub(word, space_remaining + 1)
                end
            else
                -- Print a word normally
                if width - text_len(current_line) < text_len(word) then push_line() end
                current_line = current_line .. word
            end
        end
    end

    push_line()

    -- Trim whitespace longer than width.
    for k, line in pairs(lines) do
        line = text_sub(line, 1, width)
        lines[k] = line
    end

    return lines
end

--- Makes the input string a fixed width. This either truncates it, or pads it
-- with spaces.
--
-- @tparam string line The string to normalise.
-- @tparam[opt] number width The width to constrain to, defaults to the width of
-- the terminal.
--
-- @treturn string The string with a specific width.
-- @usage require "cc.strings".ensure_width("a short string", 20)
-- @usage require "cc.strings".ensure_width("a rather long string which is truncated", 20)
local function ensure_width(line, width)
    expect(1, line, "string")
    expect(2, width, "number", "nil")
    width = width or term.getSize()

    line = text_sub(line, 1, width)
    local line_len = text_len(line)
    if line_len < width then
        line = line .. (" "):rep(width - line_len)
    end

    return line
end

--[[- Split a string into parts, each separated by a deliminator.

For instance, splitting the string `"a b c"` with the deliminator `" "`, would
return a table with three strings: `"a"`, `"b"`, and `"c"`.

By default, the deliminator is given as a [Lua pattern][pattern]. Passing `true`
to the `plain` argument will cause the deliminator to be treated as a litteral
string.

[pattern]: https://www.lua.org/manual/5.3/manual.html#6.4.1

@tparam string str The string to split.
@tparam string deliminator The pattern to split this string on.
@tparam[opt=false] boolean plain Treat the deliminator as a plain string, rather than a pattern.
@tparam[opt] number limit The maximum number of elements in the returned list.
@treturn { string... } The list of split strings.

@usage Split a string into words.

    require "cc.strings".split("This is a sentence.", "%s+")

@usage Split a string by "-" into at most 3 elements.

    require "cc.strings".split("a-separated-string-of-sorts", "-", true, 3)

@see table.concat To join strings together.

@since 1.112.0
]]
local function split(str, deliminator, plain, limit)
    expect(1, str, "string")
    expect(2, deliminator, "string")
    expect(3, plain, "boolean", "nil")
    expect(4, limit, "number", "nil")

    local out, out_n, pos = {}, 0, 1
    while not limit or out_n < limit - 1 do
        local start, finish = str:find(deliminator, pos, plain)
        if not start then break end

        out_n = out_n + 1
        out[out_n] = str:sub(pos, start - 1)

        -- Require us to advance by at least one character.
        if finish < start then error("separator is empty", 2) end

        pos = finish + 1
    end

    if pos == 1 then return { str } end

    out[out_n + 1] = str:sub(pos)
    return out
end

return {
    wrap = wrap,
    ensure_width = ensure_width,
    split = split,
}
