local tArgs = { ... }
if #tArgs < 1 then
    print( "Usage: mkdir <path>" )
    return
end

local sNewDir = shell.resolve( tArgs[1] )

if fs.exists( sNewDir ) and not fs.isDir(sNewDir) then
    printError( "Destination exists" )
    return
end

fs.makeDir( sNewDir )

