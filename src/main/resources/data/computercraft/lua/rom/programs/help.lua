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
local function word_wrap(text, width)
    local lines = strings.wrap(text, width)

    -- Normalise the strings suitable for use with blit. We could skip this and
    -- just use term.write, but saves us a clearLine call.
    for k, line in pairs(lines) do
        lines[k] = strings.ensure_width(line, width)
    end

    return lines
end

local sFile = help.lookup(sTopic)
local file = sFile ~= nil and io.open(sFile) or nil
if not file then
    printError("No help available")
    return
end

local contents = file:read("*a"):gsub("(\n *)[-*]( +)", "%1\7%2")
file:close()

local width, height = term.getSize()
local lines = word_wrap(contents, width)
local print_height = #lines

-- If we fit within the screen, just display without pagination.
if print_height <= height then
    print(contents)
    return
end

local offset = 0

local function draw()
    local fg, bg = ("0"):rep(width), ("f"):rep(width)
    for y = 1, height - 1 do
        term.setCursorPos(1, y)
        if y + offset > print_height then
            -- Should only happen if we resize the terminal to a larger one
            -- than actually needed for the current text.
            term.clearLine()
        else
            term.blit(lines[y + offset], fg, bg)
        end
    end
end

local function draw_menu()
    term.setTextColor(colors.yellow)
    term.setCursorPos(1, height)
    term.clearLine()

    local tag = "Help: " .. sTopic
    term.write("Help: " .. sTopic)

    if width >= #tag + 16 then
        term.setCursorPos(width - 14, height)
        term.write("Press Q to exit")
    end
end

draw()
draw_menu()

while true do
    local event, param = os.pullEvent()
    if event == "key" then
        if param == keys.up and offset > 0 then
            offset = offset - 1
            draw()
        elseif param == keys.down and offset < print_height - height then
            offset = offset + 1
            draw()
        elseif param == keys.pageUp and offset > 0 then
            offset = math.max(offset - height + 2, 0)
            draw()
        elseif param == keys.pageDown and offset < print_height - height then
            offset = math.min(offset + height - 2, print_height - height)
            draw()
        elseif param == keys.home then
            offset = 0
            draw()
        elseif param == keys["end"] then
            offset = print_height - height
            draw()
        elseif param == keys.q then
            sleep(0) -- Super janky, but consumes stray "char" events.
            break
        end
    elseif event == "mouse_scroll" then
        if param < 0 and offset > 0 then
            offset = offset - 1
            draw()
        elseif param > 0 and offset < print_height - height then
            offset = offset + 1
            draw()
        end
    elseif event == "term_resize" then
        local new_width, new_height = term.getSize()

        if new_width ~= width then
            lines = word_wrap(contents, new_width)
            print_height = #lines
        end

        width, height = new_width, new_height
        offset = math.max(math.min(offset, print_height - height), 0)
        draw()
        draw_menu()
    end
end

term.setCursorPos(1, 1)
term.clear()
