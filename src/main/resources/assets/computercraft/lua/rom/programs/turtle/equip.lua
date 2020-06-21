local translate = require("cc.translate").translate

if not turtle then
    printError(translate("cc.turtle_equip.requires_turtle"))
    return
end

local tArgs = { ... }
local function printUsage()
    print(translate("cc.turtle_equip.usage"))
end

if #tArgs ~= 2 then
    printUsage()
    return
end

local function equip(nSlot, fnEquipFunction)
    turtle.select(nSlot)
    local nOldCount = turtle.getItemCount(nSlot)
    if nOldCount == 0 then
        print(translate("cc.turtle_equip.nothing"))
    elseif fnEquipFunction() then
        local nNewCount = turtle.getItemCount(nSlot)
        if nNewCount > 0 then
            print(translate("cc.turtle_equip.swapped"))
        else
            print(translate("cc.turtle_equip.equiped"))
        end
    else
        print(translate("cc.turtle_equip.not_equippable"))
    end
end

local nSlot = tonumber(tArgs[1])
local sSide = tArgs[2]
if sSide == "left" then
    equip(nSlot, turtle.equipLeft)
elseif sSide == "right" then
    equip(nSlot, turtle.equipRight)
else
    printUsage()
    return
end
