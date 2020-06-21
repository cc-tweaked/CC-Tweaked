local translate = require("cc.translate").translate

if not turtle then
    printError(translate("cc.turtle_unequip.requires_turtle"))
    return
end

local tArgs = { ... }
local function printUsage()
    print(translate("cc.turtle_unequip.usage"))
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
                    print(translate("cc.turtle_unequip.unequipped"))
                    return
                else
                    print(translate("cc.turtle_unequip.nothing"))
                    return
                end
            end
        end
    end
    print(translate("cc.turtle_unequip.space"))
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
