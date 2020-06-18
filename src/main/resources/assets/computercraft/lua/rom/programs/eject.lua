local translate = require("cc.translate").translate

-- Get arguments
local tArgs = { ... }
if #tArgs == 0 then
    print(translate("cc.eject.usage"))
    return
end

local sDrive = tArgs[1]

-- Check the disk exists
local bPresent = disk.isPresent(sDrive)
if not bPresent then
    print(translate("cc.eject.no_drive"):format(sDrive))
    return
end

disk.eject(sDrive)
