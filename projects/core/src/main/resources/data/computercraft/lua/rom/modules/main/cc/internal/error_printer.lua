-- SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- A pretty-printer for Lua errors.

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

This consumes a list of messages and "annotations" and displays the error to the
terminal.

@see cc.internal.syntax.errors For errors produced by the parser.
@local
]]

local pretty = require "cc.pretty"
local expect = require "cc.expect"
local expect, field = expect.expect, expect.field
local wrap = require "cc.strings".wrap

--- Write a message to the screen.
-- @tparam cc.pretty.Doc|string msg The message to write.
local function display(msg)
    if type(msg) == "table" then pretty.print(msg) else print(msg) end
end

-- Write a message to the screen, aligning to the current cursor position.
-- @tparam cc.pretty.Doc|string msg The message to write.
local function display_here(msg, preamble)
    expect(1, msg, "string", "table")
    local x = term.getCursorPos()
    local width, height = term.getSize()
    width = width - x + 1

    local function newline()
        local _, y = term.getCursorPos()
        if y >= height then
            term.scroll(1)
        else
            y = y + 1
        end

        preamble(y)
        term.setCursorPos(x, y)
    end

    if type(msg) == "string" then
        local lines = wrap(msg, width)
        term.write(lines[1])
        for i = 2, #lines do
            newline()
            term.write(lines[i])
        end
    else
        local def_colour = term.getTextColour()
        local function display_impl(doc)
            expect(1, doc, "table")
            local kind = doc.tag
            if kind == "nil" then return
            elseif kind == "text" then
                -- TODO: cc.strings.wrap doesn't support a leading indent. We should
                -- fix that!
                -- Might also be nice to add a wrap_iter, which returns an iterator over
                -- start_pos, end_pos instead.

                if doc.colour then term.setTextColour(doc.colour) end
                local x1 = term.getCursorPos()

                local lines = wrap((" "):rep(x1 - x) .. doc.text, width)
                term.write(lines[1]:sub(x1 - x + 1))
                for i = 2, #lines do
                    newline()
                    term.write(lines[i])
                end

                if doc.colour then term.setTextColour(def_colour) end
            elseif kind == "concat" then
                for i = 1, doc.n do display_impl(doc[i]) end
            else
                error("Unknown doc " .. kind)
            end
        end
        display_impl(msg)
    end
    print()
end

--- A list of colours we can use for error messages.
local error_colours = { colours.red, colours.green, colours.magenta, colours.orange }

--- The accent line used to denote a block of code.
local code_accent = pretty.text("\x95", colours.cyan)

--[[-
@tparam { get_pos = function, get_line = function } context
    The context where the error was reported. This effectively acts as a view
    over the underlying source, exposing the following functions:
    - `get_pos`: Get the line and column of an opaque position.
    - `get_line`: Get the source code for an opaque position.
@tparam table message The message to display, as produced by [`cc.internal.syntax.errors`].
]]
return function(context, message)
    expect(1, context, "table")
    expect(2, message, "table")
    field(context, "get_pos", "function")
    field(context, "get_line", "function")

    if #message == 0 then error("Message is empty", 2) end

    local error_colour = 1
    local width = term.getSize()

    for msg_idx = 1, #message do
        if msg_idx > 1 then print() end

        local msg = message[msg_idx]
        if type(msg) == "table" and msg.tag == "annotate" then
            local line, col = context.get_pos(msg.start_pos)
            local end_line, end_col = context.get_pos(msg.end_pos)
            local contents = context.get_line(msg.start_pos)

            -- Pick a starting column. We pick the left-most position which fits
            -- in one of the following:
            --  - 10 characters after the start column.
            --  - 5 characters after the end column.
            --  - The end of the line.
            if line ~= end_line then end_col = #contents end
            local start_col = math.max(1, math.min(col + 10, end_col + 5, #contents + 1) - width + 1)

            -- Pick a colour for this annotation.
            local colour = colours.toBlit(error_colours[error_colour])
            error_colour = (error_colour % #error_colours) + 1

            -- Print the line number and snippet of code. We display french
            -- quotes on either side of the string if it is truncated.
            local str_start, str_end = start_col, start_col + width - 2
            local prefix, suffix = "", ""
            if start_col > 1 then
                str_start = str_start + 1
                prefix = pretty.text("\xab", colours.grey)
            end
            if str_end < #contents then
                str_end = str_end - 1
                suffix = pretty.text("\xbb", colours.grey)
            end

            pretty.print(code_accent .. pretty.text("Line " .. line, colours.cyan))
            pretty.print(code_accent .. prefix .. pretty.text(contents:sub(str_start, str_end), colours.lightGrey) .. suffix)

            -- Print a line highlighting the region of text.
            local _, y = term.getCursorPos()
            pretty.write(code_accent)

            local indicator_end = end_col
            if end_col > str_end then indicator_end = str_end end

            local indicator_len = indicator_end - col + 1
            term.setCursorPos(col - start_col + 2, y)
            term.blit(("\x83"):rep(indicator_len), colour:rep(indicator_len), ("f"):rep(indicator_len))
            print()

            -- And then print the annotation's message, if present.
            if msg.msg ~= "" then
                term.blit("\x95", colour, "f")
                display_here(msg.msg, function(y)
                    term.setCursorPos(1, y)
                    term.blit("\x95", colour, "f")
                end)
            end
        else
            display(msg)
        end
    end
end
