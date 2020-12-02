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

local sFile = help.lookup(sTopic)
local file = sFile ~= nil and io.open(sFile) or nil
if not file then
    printError("No help available")
end

local contents = file:read("*a"):gsub("(\n *)[-*]( +)", "%1\7%2")
file:close()

local width, height = term.getSize()
local buffer = window.create(term.current(), 1, 1, width, height, false)
local old_term = term.redirect(buffer)

local print_height = print(contents) + 1

-- If we fit within the screen, just display without pagination.
if print_height <= height then
    term.redirect(old_term)
    print(contents)
    return
end

local function draw_buffer(width)
    buffer.reposition(1, 1, width, print_height)
    buffer.clear()
    buffer.setCursorPos(1, 1)
    print(contents)
    term.redirect(old_term)
end

local offset = 0

local function draw()
    for y = 1, height - 1 do
        term.setCursorPos(1, y)
        if y + offset > print_height then
             -- Should only happen if we resize the terminal to a larger one
             -- than actually needed for the current text.
            term.clearLine()
        else
            term.blit(buffer.getLine(y + offset))
        end
    end
end

local function draw_menu()
    term.setTextColor(colors.yellow)
    term.setCursorPos(1, height)
    term.clearLine()

    local tag = "Help: " .. sTopic
    term.write("Help: " .. sTopic)

    if width >= #tag + 15 then
        term.setCursorPos(width - 14, height)
        term.write("Press Q to exit")
    end
end

draw_buffer(width)
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
            offset = math.min(offset + height, 0)
            draw()
        elseif param == keys.pageDown and offset < print_height - height then
            offset = math.max(offset - height, print_height - height)
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
            buffer.setCursorPos(1, 1)
            buffer.reposition(1, 1, new_width, print_height)
            term.redirect(buffer)
            print_height = print(contents) + 1
            draw_buffer(new_width)
        end

        width, height = new_width, new_height
        offset = math.max(math.min(offset, print_height - height), 0)
        draw()
        draw_menu()
    end
end

term.redirect(old_term)
term.setCursorPos(1, 1)
term.clear()
