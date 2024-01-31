-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

-- Paint created by nitrogenfingers (edited by dan200)
-- http://www.youtube.com/user/NitrogenFingers

------------
-- Fields --
------------

-- The width and height of the terminal
local w, h = term.getSize()

-- The selected colours on the left and right mouse button, and the colour of the canvas
local leftColour, rightColour = colours.white, nil
local canvasColour = colours.black

-- The values stored in the canvas
local canvas = {}

-- The menu options
local mChoices = { "Save", "Exit" }

-- The message displayed in the footer bar
local fMessage = "Press Ctrl or click here to access menu"

-------------------------
-- Initialisation --
-------------------------

-- Determine if we can even run this
if not term.isColour() then
    print("Requires an Advanced Computer")
    return
end

-- Determines if the file exists, and can be edited on this computer
local tArgs = { ... }
if #tArgs == 0 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <path>")
    return
end
local sPath = shell.resolve(tArgs[1])
local bReadOnly = fs.isReadOnly(sPath)
if fs.exists(sPath) and fs.isDir(sPath) then
    print("Cannot edit a directory.")
    return
end

-- Create .nfp files by default
if not fs.exists(sPath) and not string.find(sPath, "%.") then
    local sExtension = settings.get("paint.default_extension")
    if sExtension ~= "" and type(sExtension) == "string" then
        sPath = sPath .. "." .. sExtension
    end
end


---------------
-- Functions --
---------------

local function getCanvasPixel(x, y)
    if canvas[y] then
        return canvas[y][x]
    end
    return nil
end

--[[
    Converts a colour value to a text character
    params: colour = the number to convert to a hex value
    returns: a string representing the chosen colour
]]
local function getCharOf(colour)
    -- Incorrect values always convert to nil
    if type(colour) == "number" then
        local value = math.floor(math.log(colour) / math.log(2)) + 1
        if value >= 1 and value <= 16 then
            return string.sub("0123456789abcdef", value, value)
        end
    end
    return " "
end

--[[
    Converts a text character to colour value
    params: char = the char (from string.byte) to convert to number
    returns: the colour number of the hex value
]]
local tColourLookup = {}
for n = 1, 16 do
    tColourLookup[string.byte("0123456789abcdef", n, n)] = 2 ^ (n - 1)
end
local function getColourOf(char)
    -- Values not in the hex table are transparent (canvas coloured)
    return tColourLookup[char]
end

--[[
    Loads the file into the canvas
    params: path = the path of the file to open
    returns: nil
]]
local function load(path)
    -- Load the file
    if fs.exists(path) then
        local file = fs.open(sPath, "r")
        local sLine = file.readLine()
        while sLine do
            local line = {}
            for x = 1, w - 2 do
                line[x] = getColourOf(string.byte(sLine, x, x))
            end
            table.insert(canvas, line)
            sLine = file.readLine()
        end
        file.close()
    end
end

--[[
    Saves the current canvas to file
    params: path = the path of the file to save
    returns: true if save was successful, false otherwise
]]
local function save(path)
    -- Open file
    local sDir = string.sub(sPath, 1, #sPath - #fs.getName(sPath))
    if not fs.exists(sDir) then
        fs.makeDir(sDir)
    end

    local file, err = fs.open(path, "w")
    if not file then
        return false, err
    end

    -- Encode (and trim)
    local tLines = {}
    local nLastLine = 0
    for y = 1, h - 1 do
        local sLine = ""
        local nLastChar = 0
        for x = 1, w - 2 do
            local c = getCharOf(getCanvasPixel(x, y))
            sLine = sLine .. c
            if c ~= " " then
                nLastChar = x
            end
        end
        sLine = string.sub(sLine, 1, nLastChar)
        tLines[y] = sLine
        if #sLine > 0 then
            nLastLine = y
        end
    end

    -- Save out
    for n = 1, nLastLine do
           file.writeLine(tLines[n])
    end
    file.close()
    return true
end

--[[
    Draws colour picker sidebar, the palette and the footer
    returns: nil
]]
local function drawInterface()
    -- Footer
    term.setCursorPos(1, h)
    term.setBackgroundColour(colours.black)
    term.setTextColour(colours.yellow)
    term.clearLine()
    term.write(fMessage)

    -- Colour Picker
    for i = 1, 16 do
        term.setCursorPos(w - 1, i)
        term.setBackgroundColour(2 ^ (i - 1))
        term.write("  ")
    end

    term.setCursorPos(w - 1, 17)
    term.setBackgroundColour(canvasColour)
    term.setTextColour(colours.grey)
    term.write("\127\127")

    -- Left and Right Selected Colours
    do
        term.setCursorPos(w - 1, 18)
        if leftColour ~= nil then
            term.setBackgroundColour(leftColour)
            term.write(" ")
        else
            term.setBackgroundColour(canvasColour)
            term.setTextColour(colours.grey)
            term.write("\127")
        end
        if rightColour ~= nil then
            term.setBackgroundColour(rightColour)
            term.write(" ")
        else
            term.setBackgroundColour(canvasColour)
            term.setTextColour(colours.grey)
            term.write("\127")
        end
    end

    -- Padding
    term.setBackgroundColour(canvasColour)
    for i = 20, h - 1 do
        term.setCursorPos(w - 1, i)
        term.write("  ")
    end
end

--[[
    Converts a single pixel of a single line of the canvas and draws it
    returns: nil
]]
local function drawCanvasPixel(x, y)
    local pixel = getCanvasPixel(x, y)
    if pixel then
        term.setBackgroundColour(pixel or canvasColour)
        term.setCursorPos(x, y)
        term.write(" ")
    else
        term.setBackgroundColour(canvasColour)
        term.setTextColour(colours.grey)
        term.setCursorPos(x, y)
        term.write("\127")
    end
end

local color_hex_lookup = {}
for i = 0, 15 do
    color_hex_lookup[2 ^ i] = string.format("%x", i)
end

--[[
    Converts each colour in a single line of the canvas and draws it
    returns: nil
]]
local function drawCanvasLine(y)
    local text, fg, bg = "", "", ""
    for x = 1, w - 2 do
        local pixel = getCanvasPixel(x, y)
        if pixel then
            text = text .. " "
            fg = fg .. "0"
            bg = bg .. color_hex_lookup[pixel or canvasColour]
        else
            text = text .. "\127"
            fg = fg .. color_hex_lookup[colours.grey]
            bg = bg .. color_hex_lookup[canvasColour]
        end
    end

    term.setCursorPos(1, y)
    term.blit(text, fg, bg)
end

--[[
    Converts each colour in the canvas and draws it
    returns: nil
]]
local function drawCanvas()
    for y = 1, h - 1 do
        drawCanvasLine(y)
    end
end

local function termResize()
    w, h = term.getSize()
    drawCanvas()
    drawInterface()
end

local menu_choices = {
    Save = function()
        if bReadOnly then
            fMessage = "Access denied"
            return false
        end
        local success, err = save(sPath)
        if success then
            fMessage = "Saved to " .. sPath
        else
            if err then
                fMessage = "Error saving to " .. err
            else
                fMessage = "Error saving to " .. sPath
            end
        end
        return false
    end,
    Exit = function()
        require "cc.internal.event".discard_char() -- Consume stray "char" events from pressing Ctrl then E separately.
        return true
    end,
}

--[[
    Draws menu options and handles input from within the menu.
    returns: true if the program is to be exited; false otherwise
]]
local function accessMenu()
    -- Selected menu option
    local selection = 1

    term.setBackgroundColour(colours.black)

    while true do
        -- Draw the menu
        term.setCursorPos(1, h)
        term.clearLine()
        term.setTextColour(colours.white)
        for k, v in pairs(mChoices) do
            if selection == k then
                term.setTextColour(colours.yellow)
                term.write("[")
                term.setTextColour(colours.white)
                term.write(v)
                term.setTextColour(colours.yellow)
                term.write("]")
                term.setTextColour(colours.white)
            else
                term.write(" " .. v .. " ")
            end
        end

        -- Handle input in the menu
        local id, param1, param2, param3 = os.pullEvent()
        if id == "key" then
            local key = param1

            -- Handle menu shortcuts.
            for _, menu_item in ipairs(mChoices) do
                local k = keys[menu_item:sub(1, 1):lower()]
                if k and k == key then
                    return menu_choices[menu_item]()
                end
            end

            if key == keys.right then
                -- Move right
                selection = selection + 1
                if selection > #mChoices then
                    selection = 1
                end

            elseif key == keys.left and selection > 1 then
                -- Move left
                selection = selection - 1
                if selection < 1 then
                    selection = #mChoices
                end

            elseif key == keys.enter or key == keys.numPadEnter then
                -- Select an option
                return menu_choices[mChoices[selection]]()
            elseif key == keys.leftCtrl or keys == keys.rightCtrl then
                -- Cancel the menu
                return false
            end
        elseif id == "mouse_click" then
            local cx, cy = param2, param3
            if cy ~= h then return false end -- Exit the menu

            local nMenuPosEnd = 1
            local nMenuPosStart = 1
            for _, sMenuItem in ipairs(mChoices) do
                nMenuPosEnd = nMenuPosEnd + #sMenuItem + 1
                if cx > nMenuPosStart and cx < nMenuPosEnd then
                    return menu_choices[sMenuItem]()
                end
                nMenuPosEnd = nMenuPosEnd + 1
                nMenuPosStart = nMenuPosEnd
            end
        elseif id == "term_resize" then
            termResize()
        end
    end
end

--[[
    Runs the main thread of execution. Draws the canvas and interface, and handles
    mouse and key events.
    returns: nil
]]
local function handleEvents()
    local programActive = true
    while programActive do
        local id, p1, p2, p3 = os.pullEvent()
        if id == "mouse_click" or id == "mouse_drag" then
            if p2 >= w - 1 and p3 >= 1 and p3 <= 17 then
                if id ~= "mouse_drag" then
                    -- Selecting an items in the colour picker
                    if p3 <= 16 then
                        if p1 == 1 then
                            leftColour = 2 ^ (p3 - 1)
                        else
                            rightColour = 2 ^ (p3 - 1)
                        end
                    else
                        if p1 == 1 then
                            leftColour = nil
                        else
                            rightColour = nil
                        end
                    end
                    --drawCanvas()
                    drawInterface()
                end
            elseif p2 < w - 1 and p3 <= h - 1 then
                -- Clicking on the canvas
                local paintColour = nil
                if p1 == 1 then
                    paintColour = leftColour
                elseif p1 == 2 then
                    paintColour = rightColour
                end
                if not canvas[p3] then
                    canvas[p3] = {}
                end
                canvas[p3][p2] = paintColour

                drawCanvasPixel(p2, p3)
            elseif p3 == h and id == "mouse_click" then
                -- Open menu
                programActive = not accessMenu()
                drawInterface()
            end
        elseif id == "key" then
            if p1 == keys.leftCtrl or p1 == keys.rightCtrl then
                programActive = not accessMenu()
                drawInterface()
            end
        elseif id == "term_resize" then
            termResize()
        end
    end
end

-- Init
load(sPath)
drawCanvas()
drawInterface()

-- Main loop
handleEvents()

-- Shutdown
term.setBackgroundColour(colours.black)
term.setTextColour(colours.white)
term.clear()
term.setCursorPos(1, 1)
