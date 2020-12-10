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

local function word_wrap(text, width)
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
                if width - #current_line - 1 < #word then push_line() end
                current_line = current_line .. word
            end
        end
    end

    push_line()

    -- Normalise the strings suitable for use with blit. We could skip this and
    -- just use term.write, but saves us a clearLine call.
    for k, line in pairs(lines) do
        line = line:sub(1, width)
        if #line < width then line = line .. (" "):rep(width - #line) end
        lines[k] = line
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
