-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

if not turtle then
    printError("Requires a Turtle")
    return
end

local tArgs = { ... }
local nLimit = 1
if #tArgs > 1 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " [number]")
    return
elseif #tArgs > 0 then
    if tArgs[1] == "all" then
        nLimit = nil
    else
        nLimit = tonumber(tArgs[1])
        if not nLimit then
            print("Invalid limit, expected a number or \"all\"")
            return
        end
    end
end

if turtle.getFuelLevel() ~= "unlimited" then
    for n = 1, 16 do
        -- Stop if we've reached the limit, or are fully refuelled.
        if nLimit and nLimit <= 0 or turtle.getFuelLevel() >= turtle.getFuelLimit() then
            break
        end

        local nCount = turtle.getItemCount(n)
        if nCount > 0 then
            turtle.select(n)
            if turtle.refuel(nLimit) and nLimit then
                local nNewCount = turtle.getItemCount(n)
                nLimit = nLimit - (nCount - nNewCount)
            end
        end
    end
    print("Fuel level is " .. turtle.getFuelLevel())
    if turtle.getFuelLevel() == turtle.getFuelLimit() then
        print("Fuel limit reached")
    end
else
    print("Fuel level is unlimited")
end
