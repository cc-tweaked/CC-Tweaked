local translate = require("cc.translate").translate

if not turtle then
    printError(translate("cc.refuel.requires_turtle"))
    return
end

local tArgs = { ... }
local nLimit = 1
if #tArgs > 1 then
    print(translate("cc.refuel.usage"))
    return
elseif #tArgs > 0 then
    if tArgs[1] == "all" then
        nLimit = nil
    else
        nLimit = tonumber(tArgs[1])
        if not nLimit then
            print(translate("cc.refuel.invalid_limit"))
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
    print(translate("cc.refuel.current_level"):format(turtle.getFuelLevel()))
    if turtle.getFuelLevel() == turtle.getFuelLimit() then
        print(translate("cc.refuel.limit_reached"))
    end
else
    print(translate("cc.refuel.unlimeted"))
end
