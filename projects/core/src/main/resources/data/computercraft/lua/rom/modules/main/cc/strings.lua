--- Various utilities for working with strings and text.
--
-- @module cc.strings
-- @since 1.95.0
-- @see textutils For additional string related utilities.

local expect = (require and require("cc.expect") or dofile("rom/modules/main/cc/expect.lua")).expect

--[[- Wraps a block of text, so that each line fits within the given width.

This may be useful if you want to wrap text before displaying it to a
@{monitor} or @{printer} without using @{_G.print|print}.

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
            if #word > width then
                -- Print a multiline word
                while #word > 0 do
                    local space_remaining = width - #current_line - 1
                    if space_remaining <= 0 then
                        push_line()
                        space_remaining = width
                    end

                    current_line = current_line .. sub(word, 1, space_remaining)
                    word = sub(word, space_remaining + 1)
                end
            else
                -- Print a word normally
                if width - #current_line < #word then push_line() end
                current_line = current_line .. word
            end
        end
    end

    push_line()

    -- Trim whitespace longer than width.
    for k, line in pairs(lines) do
        line = line:sub(1, width)
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

    line = line:sub(1, width)
    if #line < width then
        line = line .. (" "):rep(width - #line)
    end

    return line
end

return {
    wrap = wrap,
    ensure_width = ensure_width,
}
