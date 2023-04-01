-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local function printUsage()
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usages:")
    print(programName .. " get")
    print(programName .. " get <drive>")
    print(programName .. " set <text>")
    print(programName .. " set <drive> <text>")
    print(programName .. " clear")
    print(programName .. " clear <drive>")
end

local function checkDrive(sDrive)
    if peripheral.getType(sDrive) == "drive" then
        -- Check the disk exists
        local bData = disk.hasData(sDrive)
        if not bData then
            print("No disk in " .. sDrive .. " drive")
            return false
        end
    else
        print("No disk drive named " .. sDrive)
        return false
    end
    return true
end

local function get(sDrive)
    if sDrive ~= nil then
        if checkDrive(sDrive) then
            local sLabel = disk.getLabel(sDrive)
            if sLabel then
                print("Disk label is \"" .. sLabel .. "\"")
            else
                print("No Disk label")
            end
        end
    else
        local sLabel = os.getComputerLabel()
        if sLabel then
            print("Computer label is \"" .. sLabel .. "\"")
        else
            print("No Computer label")
        end
    end
end

local function set(sDrive, sText)
    if sDrive ~= nil then
        if checkDrive(sDrive) then
            disk.setLabel(sDrive, sText)
            local sLabel = disk.getLabel(sDrive)
            if sLabel then
                print("Disk label set to \"" .. sLabel .. "\"")
            else
                print("Disk label cleared")
            end
        end
    else
        os.setComputerLabel(sText)
        local sLabel = os.getComputerLabel()
        if sLabel then
            print("Computer label set to \"" .. sLabel .. "\"")
        else
            print("Computer label cleared")
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
