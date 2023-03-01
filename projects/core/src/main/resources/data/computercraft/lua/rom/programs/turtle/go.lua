-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

if not turtle then
    printError("Requires a Turtle")
    return
end

local tArgs = { ... }
if #tArgs < 1 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <direction> <distance>")
    return
end

local tHandlers = {
    ["fd"] = turtle.forward,
    ["forward"] = turtle.forward,
    ["forwards"] = turtle.forward,
    ["bk"] = turtle.back,
    ["back"] = turtle.back,
    ["up"] = turtle.up,
    ["dn"] = turtle.down,
    ["down"] = turtle.down,
    ["lt"] = turtle.turnLeft,
    ["left"] = turtle.turnLeft,
    ["rt"] = turtle.turnRight,
    ["right"] = turtle.turnRight,
}

local nArg = 1
while nArg <= #tArgs do
    local sDirection = tArgs[nArg]
    local nDistance = 1
    if nArg < #tArgs then
        local num = tonumber(tArgs[nArg + 1])
        if num then
            nDistance = num
            nArg = nArg + 1
        end
    end
    nArg = nArg + 1

    local fnHandler = tHandlers[string.lower(sDirection)]
    if fnHandler then
        while nDistance > 0 do
            if fnHandler() then
                nDistance = nDistance - 1
            elseif turtle.getFuelLevel() == 0 then
                print("Out of fuel")
                return
            else
                sleep(0.5)
            end
        end
    else
        print("No such direction: " .. sDirection)
        print("Try: forward, back, up, down")
        return
    end

end
