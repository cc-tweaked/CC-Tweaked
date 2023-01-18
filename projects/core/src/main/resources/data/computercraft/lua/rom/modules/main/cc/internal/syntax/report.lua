--[[- A pretty-printer for syntax errors.

:::warning
This is an internal module and SHOULD NOT be used in your own code. It may
be removed or changed at any time.
:::

This consumes errors produced by @{cc.internal.syntax.errors} and the
accompanying source code, and displays the error to the terminal.

@local
]]

local pretty = require "cc.pretty"
local expect = require "cc.expect"
local wrap = require "cc.strings".wrap
local expect, field = expect.expect, expect.field

local function display(msg)
    if type(msg) == "table" then pretty.print(msg) else print(msg) end
end

local function get_len(msg)
    if type(msg) == "string" then return #msg end

    local kind = msg.tag
    if kind == "nil" or kind == "line" then
        return 0
    elseif kind == "text" then
        return #msg.text
    elseif kind == "concat" then
        local len = 0
        for i = 1, msg.n do
            len = len + get_len(msg[i])
        end
        return len
    else
        error("Unknown doc " .. kind)
    end
end

local function display_here(msg)
    local x = term.getCursorPos()
    local width, height = term.getSize()
    width = width - x + 1

    if type(msg) == "string" then
        local lines = wrap(msg, width)
        term.write(lines[1])
        for i = 2, #lines do
            local _, y = term.getCursorPos()
            if y >= height then
                term.scroll(1)
            else
                y = y + 1
            end
            term.setCursorPos(1, y)
            term.blit("\x95", "9", "f")
            term.setCursorPos(x, y)

            term.write(lines[i])
        end
        print()
    else
        pretty.print(msg)
    end
end

--[[-
@tparam string input The input string.
@tparam function get_pos A function to resolve a position to a line and column.
@tparam table message The message to display, as produced by @{cc.internal.syntax.errors}.
]]
return function(input, get_pos, message)
    expect(1, input, "string")
    expect(2, get_pos, "function")
    expect(3, message, "table")
    if #message == 0 then error("Message is empty", 2) end

    local start = pretty.text("\x95", colours.cyan)
    local error_colours, error_colour = { colours.red, colours.green, colours.magenta, colours.orange }, 1

    local drawn = false

    local function make_message(msg)
        expect(1, msg, "table")
        field(msg, "msg", "string", "table")
        field(msg, "col", "number")
        field(msg, "line", "number")
        field(msg, "end_col", "number")
        field(msg, "end_line", "number")

        local indicator, indicator_col
        if msg.line ~= msg.end_line then
            indicator = "\x83" -- TODO: Is this the best way?
            indicator_col = msg.col + 1
        else
            indicator = ("\x83"):rep(msg.end_col - msg.col + 1)
            indicator_col = math.floor((msg.end_col + msg.col) / 2)
        end

        local offset = indicator_col - msg.col + 1
        indicator = indicator:sub(1, offset - 1) ..
            string.char(bit32.bor(0x14, indicator:byte(offset))) ..
            indicator:sub(offset + 1)

        msg.indicator = indicator
        msg.indicator_col = indicator_col
        msg.colour = error_colours[error_colour]
        error_colour = (error_colour % #error_colours) + 1

        return msg
    end

    local current_line, current_bol, current_messages
    local function flush_messages()
        if not current_line then return end
        if drawn then print() else drawn = true end

        local contents = input:match("[^\r\n]*", current_bol)

        pretty.print(start .. pretty.text("Line " .. current_line, colours.cyan))
        pretty.print(start .. pretty.text(contents, colours.lightGrey))

        local current_col = 1
        pretty.write(start)
        for _, msg in ipairs(current_messages) do
            write((" "):rep(msg.col - current_col))
            pretty.write(pretty.text(msg.indicator, msg.colour))
            current_col = msg.end_col + 1
        end
        print()

        local max_col = current_messages[#current_messages].indicator_col
        for _, msg in ipairs(current_messages) do
            pretty.write(
                start ..
                (" "):rep(msg.indicator_col - 1) ..
                pretty.text("\x8d" .. ("\x8c"):rep(max_col - msg.indicator_col) .. " ", msg.colour)
            )
            display_here(msg.msg)
        end

        current_line, current_bol, current_messages = nil, nil, nil
    end

    for _, msg in ipairs(message) do
        if type(msg) == "table" and msg.tag == "annotate" then
            local line, col, bol = get_pos(msg.start_pos)
            local end_line, end_col = get_pos(msg.end_pos)

            local full_msg = make_message {
                msg = msg.msg, line = line, col = col, end_line = end_line, end_col = end_col,
            }

            if current_line == line then
                current_messages[#current_messages + 1] = full_msg
            else
                flush_messages()
                current_line, current_bol, current_messages = line, bol, { full_msg }
            end
        else
            flush_messages()
            if drawn then print() else drawn = true end
            display(msg)
        end
    end

    flush_messages()
end
