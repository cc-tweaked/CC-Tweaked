
local tArgs = { ... }
if #tArgs < 1 then
    print( "Usage: type <path>" )
    return
end

local sPath = shell.resolve( tArgs[1] )
if fs.exists( sPath ) then
    if fs.isDir( sPath ) then
        print( "directory" )
    else
        print( "file" )
    end
else
    print( "No such path" )
end

