if not turtle then
    printError( "Requires a Turtle" )
    return
end

local tArgs = { ... }
if #tArgs < 1 then
    print( "Usage: turn <direction> <turns>" )
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
        local num = tonumber( tArgs[nArg + 1] )
        if num then
            nDistance = num
            nArg = nArg + 1
        end
    end
    nArg = nArg + 1

    local fnHandler = tHandlers[string.lower(sDirection)]
    if fnHandler then
        for n=1,nDistance do
            fnHandler( nArg )
        end
    else
        print( "No such direction: "..sDirection )
        print( "Try: left, right" )
        return
    end
end
