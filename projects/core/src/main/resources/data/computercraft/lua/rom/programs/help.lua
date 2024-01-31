-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tArgs = { ... }
local sTopic
if #tArgs > 0 then
    sTopic = tArgs[1]
else
    sTopic = "intro"
end

if sTopic == "index" then
    print("Help topics available:")
    local tTopics = help.topics()
    textutils.pagedTabulate(tTopics)
    return
end

local strings = require "cc.strings"

local function min_of(a, b, default)
    if not a and not b then return default end
    if not a then return b end
    if not b then return a end
    return math.min(a, b)
end

--[[- Parse a markdown string, extracting headings and highlighting some basic
constructs.

The implementation of this is horrible. SquidDev shouldn't be allowed to write
parsers, especially ones they think might be "performance critical".
]]
local function parse_markdown(text)
    local len = #text
    local oob = len + 1

    -- Some patterns to match headers and bullets on the start of lines.
    -- The `%f[^\n\0]` is some wonderful logic to match the start of a line /or/
    -- the start of the document.
    local heading = "%f[^\n\0](#+ +)([^\n]*)"
    local bullet = "%f[^\n\0]( *)[.*]( +)"
    local code = "`([^`]+)`"

    local new_text, fg, bg = "", "", ""
    local function append(txt, fore, back)
        new_text = new_text .. txt
        fg = fg .. (fore or "0"):rep(#txt)
        bg = bg .. (back or "f"):rep(#txt)
    end

    local next_header = text:find(heading)
    local next_bullet = text:find(bullet)
    local next_block = min_of(next_header, next_bullet, oob)

    local next_code, next_code_end = text:find(code)

    local sections = {}

    local start = 1
    while start <= len do
        if start == next_block then
            if start == next_header then
                local _, fin, head, content = text:find(heading, start)
                sections[#new_text + 1] = content
                append(head .. content, "4", "f")
                start = fin + 1

                next_header = text:find(heading, start)
            else
                local _, fin, space, content = text:find(bullet, start)
                append(space .. "\7" .. content)
                start = fin + 1

                next_bullet = text:find(bullet, start)
            end

            next_block = min_of(next_header, next_bullet, oob)
        elseif next_code and next_code_end < next_block then
            -- Basic inline code blocks
            if start < next_code then append(text:sub(start, next_code - 1)) end
            local content = text:match(code, next_code)
            append(content, "0", "7")

            start = next_code_end + 1
            next_code, next_code_end = text:find(code, start)
        else
            -- Normal text
            append(text:sub(start, next_block - 1))
            start = next_block

            -- Rescan for a new code block
            if next_code then next_code, next_code_end = text:find(code, start) end
        end
    end

    return new_text, fg, bg, sections
end

local function word_wrap_basic(text, width)
    local lines, fg, bg = strings.wrap(text, width), {}, {}
    local fg_line, bg_line = ("0"):rep(width), ("f"):rep(width)

    -- Normalise the strings suitable for use with blit. We could skip this and
    -- just use term.write, but saves us a clearLine call.
    for k, line in pairs(lines) do
        lines[k] = strings.ensure_width(line, width)
        fg[k] = fg_line
        bg[k] = bg_line
    end

    return lines, fg, bg, {}
end

local function word_wrap_markdown(text, width)
    -- Add in styling for Markdown-formatted text.
    local text, fg, bg, sections = parse_markdown(text)

    local lines = strings.wrap(text, width)
    local fglines, bglines, section_list, section_n = {}, {}, {}, 1

    -- Normalise the strings suitable for use with blit. We could skip this and
    -- just use term.write, but saves us a clearLine call.
    local start = 1
    for k, line in pairs(lines) do
        -- I hate this with a burning passion, but it works!
        local pos = text:find(line, start, true)
        lines[k], fglines[k], bglines[k] =
            strings.ensure_width(line, width),
            strings.ensure_width(fg:sub(pos, pos + #line), width),
            strings.ensure_width(bg:sub(pos, pos + #line), width)

        if sections[pos] then
            section_list[section_n], section_n = { content = sections[pos], offset = k - 1 }, section_n + 1
        end

        start = pos + 1
    end

    return lines, fglines, bglines, section_list
end

local sFile = help.lookup(sTopic)
local file = sFile ~= nil and io.open(sFile) or nil
if not file then
    printError("No help available")
    return
end

local contents = file:read("*a")
file:close()
-- Trim trailing newlines from the file to avoid displaying a blank line.
if contents:sub(-1) == "\n" then contents:sub(1, -2) end

local word_wrap = sFile:sub(-3) == ".md" and word_wrap_markdown or word_wrap_basic
local width, height = term.getSize()
local content_height = height - 1 -- Height of the content box.
local lines, fg, bg, sections = word_wrap(contents, width)
local print_height = #lines

-- If we fit within the screen, just display without pagination.
if print_height <= content_height then
    local _, y = term.getCursorPos()
    for i = 1, print_height do
        if y + i - 1 > height then
            term.scroll(1)
            term.setCursorPos(1, height)
        else
            term.setCursorPos(1, y + i - 1)
        end

        term.blit(lines[i], fg[i], bg[i])
    end
    return
end

local current_section = nil
local offset = 0

--- Find the currently visible section, or nil if this document has no sections.
--
-- This could potentially be a binary search, but right now it's not worth it.
local function find_section()
    for i = #sections, 1, -1 do
        if sections[i].offset <= offset then
            return i
        end
    end
end

local function draw_menu()
    term.setTextColor(colors.yellow)
    term.setCursorPos(1, height)
    term.clearLine()

    local tag = "Help: " .. sTopic
    if current_section then
        tag = tag .. (" (%s)"):format(sections[current_section].content)
    end
    term.write(tag)

    if width >= #tag + 16 then
        term.setCursorPos(width - 14, height)
        term.write("Press Q to exit")
    end
end


local function draw()
    for y = 1, content_height do
        term.setCursorPos(1, y)
        if y + offset > print_height then
            -- Should only happen if we resize the terminal to a larger one
            -- than actually needed for the current text.
            term.clearLine()
        else
            term.blit(lines[y + offset], fg[y + offset], bg[y + offset])
        end
    end

    local new_section = find_section()
    if new_section ~= current_section then
        current_section = new_section
        draw_menu()
    end
end

draw()
draw_menu()

while true do
    local event, param = os.pullEventRaw()
    if event == "key" then
        if param == keys.up and offset > 0 then
            offset = offset - 1
            draw()
        elseif param == keys.down and offset < print_height - content_height then
            offset = offset + 1
            draw()
        elseif param == keys.pageUp and offset > 0 then
            offset = math.max(offset - content_height + 1, 0)
            draw()
        elseif param == keys.pageDown and offset < print_height - content_height then
            offset = math.min(offset + content_height - 1, print_height - content_height)
            draw()
        elseif param == keys.home then
            offset = 0
            draw()
        elseif param == keys.left and current_section and current_section > 1 then
            offset = sections[current_section - 1].offset
            draw()
        elseif param == keys.right and current_section and current_section < #sections then
            offset = sections[current_section + 1].offset
            draw()
        elseif param == keys["end"] then
            offset = print_height - content_height
            draw()
        elseif param == keys.q then
            require "cc.internal.event".discard_char()
            break
        end
    elseif event == "mouse_scroll" then
        if param < 0 and offset > 0 then
            offset = offset - 1
            draw()
        elseif param > 0 and offset <= print_height - content_height then
            offset = offset + 1
            draw()
        end
    elseif event == "term_resize" then
        local new_width, new_height = term.getSize()

        if new_width ~= width then
            lines, fg, bg = word_wrap(contents, new_width)
            print_height = #lines
        end

        width, height = new_width, new_height
        content_height = height - 1
        offset = math.max(math.min(offset, print_height - content_height), 0)
        draw()
        draw_menu()
    elseif event == "terminate" then
        break
    end
end

term.setCursorPos(1, 1)
term.clear()
