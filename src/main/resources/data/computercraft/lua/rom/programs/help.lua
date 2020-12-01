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
if file then
    local sContents = file:read("*a")
    file:close()

    local w, h = term.getSize()
    local oldterm = term.redirect(window.create(term.current(), 1, 1, w, h, false))
    local len = print(sContents)
    term.redirect(oldterm)
    local win = window.create(term.current(), 1, 1, w, len)
    oldterm = term.redirect(win)
    write(sContents)
    local yPos = 1
    while true do
        local ev = { os.pullEvent() }
        if ev[1] == "key" then
           if ev[2] == keys.up and yPos < 1 then
               yPos = yPos + 1
               win.reposition(1, yPos)
           elseif ev[2] == keys.down and yPos > -len + h + 1 then
               yPos = yPos - 1
               win.reposition(1, yPos)
            elseif ev[2] == keys.pageUp and yPos < 1 then
                yPos = math.min(yPos + h, 1)
                win.reposition(1, yPos)
            elseif ev[2] == keys.pageDown and yPos > -len + h + 1 then
                yPos = math.max(yPos - h, -len + h + 1)
               win.reposition(1, yPos)
           elseif ev[2] == keys.home then
                yPos = 1
               win.reposition(1, yPos)
            elseif ev[2] == keys["end"] then
                yPos = -len + h + 1
                win.reposition(1, yPos)
           elseif ev[2] == keys.q then break end
       elseif ev[1] == "mouse_scroll" then
           if ev[2] == -1 and yPos < 1 then
                yPos = yPos + 1
                win.reposition(1, yPos)
           elseif ev[2] == 1 and yPos > -len + h + 1 then
                yPos = yPos - 1
                win.reposition(1, yPos)
            end
        end
    end
    term.redirect(oldterm)
    term.setCursorPos(1, 1)
    term.clear()
else
    print("No help available")
end
