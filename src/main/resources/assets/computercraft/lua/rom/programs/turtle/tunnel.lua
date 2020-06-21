local translate = require("cc.translate").translate

if not turtle then
    printError(translate("cc.tunnel.requires_turtle"))
    return
end

local tArgs = { ... }
if #tArgs ~= 1 then
    print(translate("cc.tunnel.usage"))
    return
end

-- Mine in a quarry pattern until we hit something we can't dig
local length = tonumber(tArgs[1])
if length < 1 then
    print(translate("cc.tunnel.positive_number"))
    return
end
local collected = 0

local function collect()
    collected = collected + 1
    if math.fmod(collected, 25) == 0 then
        print(translate("cc.tunnel.mining_counter"):format(collected))
    end
end

local function tryDig()
    while turtle.detect() do
        if turtle.dig() then
            collect()
            sleep(0.5)
        else
            return false
        end
    end
    return true
end

local function tryDigUp()
    while turtle.detectUp() do
        if turtle.digUp() then
            collect()
            sleep(0.5)
        else
            return false
        end
    end
    return true
end

local function tryDigDown()
    while turtle.detectDown() do
        if turtle.digDown() then
            collect()
            sleep(0.5)
        else
            return false
        end
    end
    return true
end

local function refuel()
    local fuelLevel = turtle.getFuelLevel()
    if fuelLevel == "unlimited" or fuelLevel > 0 then
        return
    end

    local function tryRefuel()
        for n = 1, 16 do
            if turtle.getItemCount(n) > 0 then
                turtle.select(n)
                if turtle.refuel(1) then
                    turtle.select(1)
                    return true
                end
            end
        end
        turtle.select(1)
        return false
    end

    if not tryRefuel() then
        print(translate("cc.tunnel.more_fuel"))
        while not tryRefuel() do
            os.pullEvent("turtle_inventory")
        end
        print(translate("cc.tunnel.resume"))
    end
end

local function tryUp()
    refuel()
    while not turtle.up() do
        if turtle.detectUp() then
            if not tryDigUp() then
                return false
            end
        elseif turtle.attackUp() then
            collect()
        else
            sleep(0.5)
        end
    end
    return true
end

local function tryDown()
    refuel()
    while not turtle.down() do
        if turtle.detectDown() then
            if not tryDigDown() then
                return false
            end
        elseif turtle.attackDown() then
            collect()
        else
            sleep(0.5)
        end
    end
    return true
end

local function tryForward()
    refuel()
    while not turtle.forward() do
        if turtle.detect() then
            if not tryDig() then
                return false
            end
        elseif turtle.attack() then
            collect()
        else
            sleep(0.5)
        end
    end
    return true
end

print(translate("cc.tunnel.tunnelling"))

for n = 1, length do
    turtle.placeDown()
    tryDigUp()
    turtle.turnLeft()
    tryDig()
    tryUp()
    tryDig()
    turtle.turnRight()
    turtle.turnRight()
    tryDig()
    tryDown()
    tryDig()
    turtle.turnLeft()

    if n < length then
        tryDig()
        if not tryForward() then
            print(translate("cc.tunnel.abort"))
            break
        end
    else
        print(translate("cc.tunnel.complete"))
    end

end

--[[
print( "Returning to start..." )

-- Return to where we started
turtle.turnLeft()
turtle.turnLeft()
while depth > 0 do
    if turtle.forward() then
        depth = depth - 1
    else
        turtle.dig()
    end
end
turtle.turnRight()
turtle.turnRight()
]]

print(translate("cc.tunnel.complete"))
print(translate("cc.tunnel.mining_counter_total"):format(collected))
