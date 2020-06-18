local translate = require("cc.translate").translate

local function printUsage()
    local drive = translate("cc.label.usage_drive")
    local text = translate("cc.label.usage_text")
    print(translate("cc.label.usage_title"))
    print("label get")
    print("label get " .. drive)
    print("label set " .. text)
    print("label set " .. drive .. " " .. text)
    print("label clear")
    print("label clear " .. drive)
end

local function checkDrive(sDrive)
    if peripheral.getType(sDrive) == "drive" then
        -- Check the disk exists
        local bData = disk.hasData(sDrive)
        if not bData then
            print(translate("cc.label.no_disk"):format(sDrive))
            return false
        end
    else
        print(translate("cc.label.no_drive"):format(sDrive))
        return false
    end
    return true
end

local function get(sDrive)
    if sDrive ~= nil then
        if checkDrive(sDrive) then
            local sLabel = disk.getLabel(sDrive)
            if sLabel then
                print(translate("cc.label.disk_label_print"):format(sLabel))
            else
                print(translate("cc.label.disk_label_empty"))
            end
        end
    else
        local sLabel = os.getComputerLabel()
        if sLabel then
            print(translate("cc.label.computer_label_print"):format(sLabel))
        else
            print(ranslate("cc.label.computer_label_empty"))
        end
    end
end

local function set(sDrive, sText)
    if sDrive ~= nil then
        if checkDrive(sDrive) then
            disk.setLabel(sDrive, sText)
            local sLabel = disk.getLabel(sDrive)
            if sLabel then
                print(translate("cc.label.disk_label_set"):format(sLabel))
            else
                print(translate("cc.label.disk_label_clear"))
            end
        end
    else
        os.setComputerLabel(sText)
        local sLabel = os.getComputerLabel()
        if sLabel then
            print(translate("cc.label.computer_label_set"):format(sLabel))
        else
            print(translate("cc.label.computer_label_clear"))
        end
    end
end

local tArgs = { ... }
local sCommand = tArgs[1]
if sCommand == "get" then
    -- Get a label
    if #tArgs == 1 then
        get(nil)
    elseif #tArgs == 2 then
        get(tArgs[2])
    else
        printUsage()
    end
elseif sCommand == "set" then
    -- Set a label
    if #tArgs == 2 then
        set(nil, tArgs[2])
    elseif #tArgs == 3 then
        set(tArgs[2], tArgs[3])
    else
        printUsage()
    end
elseif sCommand == "clear" then
    -- Clear a label
    if #tArgs == 1 then
        set(nil, nil)
    elseif #tArgs == 2 then
        set(tArgs[2], nil)
    else
        printUsage()
    end
else
    printUsage()
end
