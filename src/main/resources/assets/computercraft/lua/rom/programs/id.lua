local translate = require("cc.translate").translate

local sDrive = nil
local tArgs = { ... }
if #tArgs > 0 then
    sDrive = tostring(tArgs[1])
end

if sDrive == nil then
    print(translate("cc.id.computer_id"):format(os.getComputerID()))

    local label = os.getComputerLabel()
    if label then
        print(translate("cc.id.computer_label"):format(label))
    end

else
    local bData = disk.hasData(sDrive)
    if not bData then
        print(translate("cc.id.no_disk"):format(sDrive))
        return
    end

    print(translate("cc.id.disk_id"):format(disk.getID(sDrive)))

    local label = disk.getLabel(sDrive)
    if label then
        print(translate("cc.id.disk_label"):format(label))
    end
end
