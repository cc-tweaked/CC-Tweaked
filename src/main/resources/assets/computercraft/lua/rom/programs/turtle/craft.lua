local translate = require("cc.translate").translate

if not turtle then
    printError(translate("cc.craft.requires_turtle"))
    return
end

if not turtle.craft then
    print(translate("cc.craft.requires_crafty_turtle"))
    return
end

local tArgs = { ... }
local nLimit = nil
if #tArgs < 1 then
    print(translate("cc.craft.usage"))
    return
else
    nLimit = tonumber(tArgs[1])
end

local nCrafted = 0
local nOldCount = turtle.getItemCount(turtle.getSelectedSlot())
if turtle.craft(nLimit) then
    local nNewCount = turtle.getItemCount(turtle.getSelectedSlot())
    if nOldCount <= nLimit then
        nCrafted = nNewCount
    else
        nCrafted = nOldCount - nNewCount
    end
end

if nCrafted > 1 then
    print(translate("cc.craft.more_items"):format(nCrafted))
elseif nCrafted == 1 then
    print(translate("cc.craft.one_item"))
else
    print(translate("cc.craft.no_item"))
end
