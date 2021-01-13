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
    local bAudio = disk.hasAudio(sDrive)
    if bAudio then
        print("Audio medium")

        local title = disk.getAudioTitle(sDrive)
        if title then
            print("Audio is titled \"" .. title .. "\"")
        end
        return
    end

    local bData = disk.hasData(sDrive)
    if not bData then
        print("No disk in drive " .. sDrive)
        return
    end

    local id = disk.getID(sDrive)
    if id then
        print("The disk is #" .. id)
    else
        print("Non-disk data medium")
    end

    local label = disk.getLabel(sDrive)
    if label then
        print("Labelled \"" .. label .. "\"")
    end
end
