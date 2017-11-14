local tArgs = { ... }
if #tArgs < 2 then
    print( "Usage: rename <source> <destination>" )
    return
end

local sSource = shell.resolve( tArgs[1] )
local sDest = shell.resolve( tArgs[2] )

if fs.exists( sDest ) then
    printError( "Destination exists" )
end

fs.move( sSource, sDest )
