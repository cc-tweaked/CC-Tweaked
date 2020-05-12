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
    local bData = disk.hasData(sDrive)
    if not bData then
        print("No disk in drive " .. sDrive)
        return
    end

    print("The disk is #" .. disk.getID(sDrive))

    local label = disk.getLabel(sDrive)
    if label then
        print("The disk is labelled \"" .. label .. "\"")
    end
end
