-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local sDrive = nil
local tArgs = { ... }
if #tArgs > 0 then
    sDrive = tostring(tArgs[1])
end

if sDrive == nil then
    print("This is computer #" .. os.getComputerID())

    local label = os.getComputerLabel()
    if label then
        print("This computer is labelled \"" .. label .. "\"")
    end

else
    if disk.hasAudio(sDrive) then
        local title = disk.getAudioTitle(sDrive)
        if title then
            print("Has audio track \"" .. title .. "\"")
        else
            print("Has untitled audio")
        end
        return
    end

    if not disk.hasData(sDrive) then
        print("No disk in drive " .. sDrive)
        return
    end

    local id = disk.getID(sDrive)
    if id then
        print("The disk is #" .. id)
    else
        print("Non-disk data source")
    end

    local label = disk.getLabel(sDrive)
    if label then
        print("Labelled \"" .. label .. "\"")
    end
end
