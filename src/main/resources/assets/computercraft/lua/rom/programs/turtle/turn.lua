local translate = require("cc.translate").translate

if not turtle then
    printError(translate("cc.turn.requires_turtle"))
    return
end

local tArgs = { ... }
if #tArgs < 1 then
    print(translate("cc.turn.usage"))
    return
end

local tHandlers = {
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
        for _ = 1, nDistance do
            fnHandler(nArg)
        end
    else
        print(translate("cc.turn.unknown_direction"):format(sDirection))
        return
    end
end
