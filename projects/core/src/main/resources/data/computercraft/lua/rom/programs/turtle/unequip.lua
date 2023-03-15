-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

if not turtle then
    printError("Requires a Turtle")
    return
end

local tArgs = { ... }
local function printUsage()
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <side>")
end

if #tArgs ~= 1 then
    printUsage()
    return
end

local function unequip(fnEquipFunction)
    for nSlot = 1, 16 do
        local nOldCount = turtle.getItemCount(nSlot)
        if nOldCount == 0 then
            turtle.select(nSlot)
            if fnEquipFunction() then
                local nNewCount = turtle.getItemCount(nSlot)
                if nNewCount > 0 then
                    print("Item unequipped")
                    return
                else
                    print("Nothing to unequip")
                    return
                end
            end
        end
    end
    print("No space to unequip item")
end

local sSide = tArgs[1]
if sSide == "left" then
    unequip(turtle.equipLeft)
elseif sSide == "right" then
    unequip(turtle.equipRight)
else
    printUsage()
    return
end
