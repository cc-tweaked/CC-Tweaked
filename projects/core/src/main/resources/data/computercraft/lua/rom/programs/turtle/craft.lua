-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

if not turtle then
    printError("Requires a Turtle")
    return
end

if not turtle.craft then
    print("Requires a Crafty Turtle")
    return
end

local tArgs = { ... }
local nLimit = tonumber(tArgs[1])

if not nLimit and tArgs[1] ~= "all" then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " all|<number>")
    return
end

local nCrafted = 0
local nOldCount = turtle.getItemCount(turtle.getSelectedSlot())
if turtle.craft(nLimit) then
    local nNewCount = turtle.getItemCount(turtle.getSelectedSlot())
    if not nLimit or nOldCount <= nLimit then
        nCrafted = nNewCount
    else
        nCrafted = nOldCount - nNewCount
    end
end

if nCrafted > 1 then
    print(nCrafted .. " items crafted")
elseif nCrafted == 1 then
    print("1 item crafted")
else
    print("No items crafted")
end
