--- Varous string utilities that might go into textutils if we weren't avoiding
-- globals.

local expect = require "cc.expect".expect

--- Converts a long string into a table of strings of length shorter than a
-- given width.
--
-- It attemps to split on whitespace and converts \n into the next table entry.
--
-- @tparam string text The string to wrap.
--
-- @tparam[opt] number width The width to contrain to, defaults to the width of
-- the terminal.
--
-- @treturn { string... } The wrapped input string.
-- @usage strings.wrap( "long string", 5 )
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

    -- trim trailing white space
    for k, line in pairs(lines) do
        line = line:sub(1, width)
        lines[k] = line
    end

    return lines
end

--- Makes the input string a fixed width.
--
-- This is done by adding spaces or truncating the string.
--
-- @tparam string line The string to normalise.
--
-- @tparam[opt] number width The width to contrain to, defaults to the width of
-- the terminal.
--
-- @treturn { string... } The wrapped input string.
-- @usage strings.normalise( "long string", 5 )
-- @usage strings.normalize( "long string", 5 )
local function normalise(line, width)
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
    normalise = normalise,
    normalize = normalise,
}
