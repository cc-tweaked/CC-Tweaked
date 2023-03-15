-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tArgs = { ... }

local function printUsage()
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usages:")
    print(programName)
    print(programName .. " screen")
    print(programName .. " palette")
    print(programName .. " all")
end

local function clear()
    term.clear()
    term.setCursorPos(1, 1)
end

local function resetPalette()
    for i =  0, 15 do
        term.setPaletteColour(math.pow(2, i), term.nativePaletteColour(math.pow(2, i)))
    end
end

local sCommand = tArgs[1] or "screen"
if sCommand == "screen" then
    clear()
elseif sCommand == "palette" then
    resetPalette()
elseif sCommand == "all" then
    clear()
    resetPalette()
else
    printUsage()
end
