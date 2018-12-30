
-- Get arguments
local tArgs = { ... }
if #tArgs == 0 then
    print( "Usage: eject <drive>" )
    return
end

local sDrive = tArgs[1]

-- Check the disk exists
local bPresent = disk.isPresent( sDrive )
if not bPresent then
    print( "Nothing in "..sDrive.." drive" )
    return
end

disk.eject( sDrive )
