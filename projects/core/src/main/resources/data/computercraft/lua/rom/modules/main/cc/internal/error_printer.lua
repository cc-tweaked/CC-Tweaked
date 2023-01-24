--[[- A pretty-printer for Lua errors.

:::warning
This is an internal module and SHOULD NOT be used in your own code. It may
be removed or changed at any time.
:::

This consumes errors produced by @{cc.internal.syntax.errors} and the
accompanying source code, and displays the error to the terminal.

@local
]]

local pretty = require "cc.pretty"
local expect = require "cc.expect".expect
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
        print()
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
end

--- A list of colours we can use for error messages.
local error_colours = { colours.red, colours.green, colours.magenta, colours.orange }

--- The accent line used to denote a block of code.
local code_accent = pretty.text("\x95", colours.cyan)

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

    local error_colour = 1
    local width = term.getSize()

    local msg_idx, msg_n = 1, #message
    while msg_idx <= msg_n do
        if msg_idx > 1 then print() end

        local msg = message[msg_idx]
        if type(msg) == "table" and msg.tag == "annotate" then
            local current_line, first_col, current_bol = get_pos(msg.start_pos)
            local contents = input:match("[^\r\n]*", current_bol)

            -- Find the highest minimum column we could possibly start from, and thus
            -- the last column we could bundle into this line.
            local min_col = math.max(contents:find("%S") or 1, first_col - 5)
            -- TODO: If the screen is less than 35(?) characters, swap over to numbered
            -- messages.
            local max_col = min_col + (width - 25)

            -- Gather all annotations that we can fit on this line.
            local annotations = {}
            while msg_idx <= msg_n do
                local msg = message[msg_idx]
                if type(msg) ~= "table" or msg.tag ~= "annotate" then break end

                local line, col = get_pos(msg.start_pos)
                if line ~= current_line or col > max_col then break end

                local end_line, end_col = get_pos(msg.end_pos)
                annotations[#annotations + 1] = {
                    msg = msg.msg, colour = colours.toBlit(error_colours[error_colour]),
                    col = col, end_line = end_line, end_col = end_col,
                }
                error_colour = (error_colour % #error_colours) + 1

                msg_idx = msg_idx + 1
            end

            -- Now find the appropriate region of our line to display.
            local last_col = annotations[#annotations].col
            -- TODO: Would be nice to be smarter with how we pick our starting position.
            local start_col = math.max(1, last_col - (max_col - min_col))

            -- We do some additional work to display indicators if the line is truncated.
            local str_start, str_end = start_col, start_col + width - 2
            local prefix, suffix = "", ""
            if start_col > 1 then
                str_end = str_end - 1
                start_col = start_col - 1
                prefix = pretty.text("\xab", colours.grey)
            end
            if str_end < #contents then
                str_end = str_end - 1
                suffix = pretty.text("\xbb", colours.grey)
            end

            -- Print the line number and snippet of code.
            pretty.print(code_accent .. pretty.text("Line " .. current_line, colours.cyan))
            pretty.print(code_accent .. prefix .. pretty.text(contents:sub(str_start, str_end), colours.lightGrey) .. suffix)

            -- Print the annotations' underlines.
            local _, y = term.getCursorPos()
            pretty.write(code_accent)
            for _, annotation in ipairs(annotations) do
                local indicator_end = annotation.end_col
                if current_line ~= annotation.end_line or annotation.end_col > str_end then
                    indicator_end = str_end
                end

                local indicator_len = indicator_end - annotation.col + 1
                local indicator_start = annotation.col - start_col + 2

                local fg, bg = annotation.colour, "f"
                local indicator, indicator_fg, indicator_bg
                if annotation.msg ~= "" then
                    -- If we've got a message, find where to place our downward
                    -- connector. Rendering the string is simple, just a little nasty
                    -- due to the connector being inverted.
                    local indicator_offset
                    if indicator_end == str_end then
                        indicator_offset = 0
                    else
                        indicator_offset = math.ceil(indicator_len / 2) - 1
                    end
                    annotation.indicator_col = indicator_start + indicator_offset

                    indicator = ("\x83"):rep(indicator_offset) .. "\x94" .. ("\x83"):rep(indicator_len - indicator_offset - 1)
                    indicator_bg = bg:rep(indicator_offset) .. fg .. bg:rep(indicator_len - indicator_offset - 1)
                    indicator_fg = fg:rep(indicator_offset) .. bg .. fg:rep(indicator_len - indicator_offset - 1)
                else
                    -- If we've no message, we don't need a connector, so can just draw
                    -- a straight line.
                    indicator, indicator_fg, indicator_bg = ("\x83"):rep(indicator_len), fg:rep(indicator_len), bg:rep(indicator_len)
                end

                term.setCursorPos(indicator_start, y)
                term.blit(indicator, indicator_fg, indicator_bg)
            end
            print()

            -- Print the message for each annotation.
            local last_col = annotations[#annotations].indicator_col
            for i, annotation in ipairs(annotations) do
                if annotation.msg ~= "" then
                    local _, y = term.getCursorPos()
                    pretty.write(code_accent)

                    -- Start off by drawing our connector all the way to the right.
                    local indicator_col = annotation.indicator_col
                    local len = last_col - indicator_col + 2
                    local indicator = "\x8a" .. ("\x8c"):rep(len - 2) .. "\x84"
                    local indicator_bg = ("f"):rep(len)
                    local indicator_fg = annotation.colour:rep(len)
                    term.setCursorPos(indicator_col, y)
                    term.blit(indicator, indicator_fg, indicator_bg)

                    -- Then print the message. For each additional line of our message,
                    -- we need to display the code accent and any remaining connectors.
                    display_here(annotation.msg, function(y)
                        term.setCursorPos(1, y)
                        pretty.write(code_accent)

                        for j = i + 1, #annotations do
                            local annotation = annotations[j]
                            if annotation.msg ~= "" then
                                term.setCursorPos(annotation.indicator_col, y)
                                term.blit("\x95", "f", annotation.colour)
                            end
                        end
                    end)
                end
            end
        else
            display(msg)
            msg_idx = msg_idx + 1
        end
    end
end
