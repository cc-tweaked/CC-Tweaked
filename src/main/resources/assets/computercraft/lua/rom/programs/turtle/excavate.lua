local translate = require("cc.translate").translate

if not turtle then
    printError(translate("cc.excavate.requires_turtle"))
    return
end

local tArgs = { ... }
if #tArgs ~= 1 then
    print(translate("cc.excavate.usage"))
    return
end

-- Mine in a quarry pattern until we hit something we can't dig
local size = tonumber(tArgs[1])
if size < 1 then
    print(translate("cc.excavate.positive_number"))
    return
end

local depth = 0
local unloaded = 0
local collected = 0

local xPos, zPos = 0, 0
local xDir, zDir = 0, 1

local goTo -- Filled in further down
local refuel -- Filled in further down

local function unload(_bKeepOneFuelStack)
    print(translate("cc.excavate.unloading_items"))
    for n = 1, 16 do
        local nCount = turtle.getItemCount(n)
        if nCount > 0 then
            turtle.select(n)
            local bDrop = true
            if _bKeepOneFuelStack and turtle.refuel(0) then
                bDrop = false
                _bKeepOneFuelStack = false
            end
            if bDrop then
                turtle.drop()
                unloaded = unloaded + nCount
            end
        end
    end
    collected = 0
    turtle.select(1)
end

local function returnSupplies()
    local x, y, z, xd, zd = xPos, depth, zPos, xDir, zDir
    print(translate("cc.excavate.retutn_to_surface"))
    goTo(0, 0, 0, 0, -1)

    local fuelNeeded = 2 * (x + y + z) + 1
    if not refuel(fuelNeeded) then
        unload(true)
        print(translate("cc.excavate.waiting_for_fuel"))
        while not refuel(fuelNeeded) do
            os.pullEvent("turtle_inventory")
        end
    else
        unload(true)
    end

    print(translate("cc.excavate.resume_mining"))
    goTo(x, y, z, xd, zd)
end

local function collect()
    local bFull = true
    local nTotalItems = 0
    for n = 1, 16 do
        local nCount = turtle.getItemCount(n)
        if nCount == 0 then
            bFull = false
        end
        nTotalItems = nTotalItems + nCount
    end

    if nTotalItems > collected then
        collected = nTotalItems
        if math.fmod(collected + unloaded, 50) == 0 then
            print(translate("cc.excavate.mining_counter"):format(collected + unloaded))
        end
    end

    if bFull then
        print(translate("cc.excavate.no_empty_slots"))
        return false
    end
    return true
end

function refuel(ammount)
    local fuelLevel = turtle.getFuelLevel()
    if fuelLevel == "unlimited" then
        return true
    end

    local needed = ammount or xPos + zPos + depth + 2
    if turtle.getFuelLevel() < needed then
        for n = 1, 16 do
            if turtle.getItemCount(n) > 0 then
                turtle.select(n)
                if turtle.refuel(1) then
                    while turtle.getItemCount(n) > 0 and turtle.getFuelLevel() < needed do
                        turtle.refuel(1)
                    end
                    if turtle.getFuelLevel() >= needed then
                        turtle.select(1)
                        return true
                    end
                end
            end
        end
        turtle.select(1)
        return false
    end

    return true
end

local function tryForwards()
    if not refuel() then
        print(translate("cc.excavate.no_fuel"))
        returnSupplies()
    end

    while not turtle.forward() do
        if turtle.detect() then
            if turtle.dig() then
                if not collect() then
                    returnSupplies()
                end
            else
                return false
            end
        elseif turtle.attack() then
            if not collect() then
                returnSupplies()
            end
        else
            sleep(0.5)
        end
    end

    xPos = xPos + xDir
    zPos = zPos + zDir
    return true
end

local function tryDown()
    if not refuel() then
        print(translate("cc.excavate.no_fuel"))
        returnSupplies()
    end

    while not turtle.down() do
        if turtle.detectDown() then
            if turtle.digDown() then
                if not collect() then
                    returnSupplies()
                end
            else
                return false
            end
        elseif turtle.attackDown() then
            if not collect() then
                returnSupplies()
            end
        else
            sleep(0.5)
        end
    end

    depth = depth + 1
    if math.fmod(depth, 10) == 0 then
        print(translate("cc.excavate.descended_metres"):format(depth))
    end

    return true
end

local function turnLeft()
    turtle.turnLeft()
    xDir, zDir = -zDir, xDir
end

local function turnRight()
    turtle.turnRight()
    xDir, zDir = zDir, -xDir
end

function goTo(x, y, z, xd, zd)
    while depth > y do
        if turtle.up() then
            depth = depth - 1
        elseif turtle.digUp() or turtle.attackUp() then
            collect()
        else
            sleep(0.5)
        end
    end

    if xPos > x then
        while xDir ~= -1 do
            turnLeft()
        end
        while xPos > x do
            if turtle.forward() then
                xPos = xPos - 1
            elseif turtle.dig() or turtle.attack() then
                collect()
            else
                sleep(0.5)
            end
        end
    elseif xPos < x then
        while xDir ~= 1 do
            turnLeft()
        end
        while xPos < x do
            if turtle.forward() then
                xPos = xPos + 1
            elseif turtle.dig() or turtle.attack() then
                collect()
            else
                sleep(0.5)
            end
        end
    end

    if zPos > z then
        while zDir ~= -1 do
            turnLeft()
        end
        while zPos > z do
            if turtle.forward() then
                zPos = zPos - 1
            elseif turtle.dig() or turtle.attack() then
                collect()
            else
                sleep(0.5)
            end
        end
    elseif zPos < z then
        while zDir ~= 1 do
            turnLeft()
        end
        while zPos < z do
            if turtle.forward() then
                zPos = zPos + 1
            elseif turtle.dig() or turtle.attack() then
                collect()
            else
                sleep(0.5)
            end
        end
    end

    while depth < y do
        if turtle.down() then
            depth = depth + 1
        elseif turtle.digDown() or turtle.attackDown() then
            collect()
        else
            sleep(0.5)
        end
    end

    while zDir ~= zd or xDir ~= xd do
        turnLeft()
    end
end

if not refuel() then
    print(translate("cc.excavate.no_fuel"))
    return
end

print(translate("cc.excavate.excavating"))

local reseal = false
turtle.select(1)
if turtle.digDown() then
    reseal = true
end

local alternate = 0
local done = false
while not done do
    for n = 1, size do
        for _ = 1, size - 1 do
            if not tryForwards() then
                done = true
                break
            end
        end
        if done then
            break
        end
        if n < size then
            if math.fmod(n + alternate, 2) == 0 then
                turnLeft()
                if not tryForwards() then
                    done = true
                    break
                end
                turnLeft()
            else
                turnRight()
                if not tryForwards() then
                    done = true
                    break
                end
                turnRight()
            end
        end
    end
    if done then
        break
    end

    if size > 1 then
        if math.fmod(size, 2) == 0 then
            turnRight()
        else
            if alternate == 0 then
                turnLeft()
            else
                turnRight()
            end
            alternate = 1 - alternate
        end
    end

    if not tryDown() then
        done = true
        break
    end
end

print(translate("cc.excavate.retutn_to_surface"))

-- Return to where we started
goTo(0, 0, 0, 0, -1)
unload(false)
goTo(0, 0, 0, 0, 1)

-- Seal the hole
if reseal then
    turtle.placeDown()
end

print(translate("cc.excavate.mining_counter_total"):format(collected + unloaded))
