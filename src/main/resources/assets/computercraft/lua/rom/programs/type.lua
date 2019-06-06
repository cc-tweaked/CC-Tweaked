
local tArgs = { ... }
if #tArgs < 1 then
    print( "Usage: type <paths>" )
    return
end

for k, v in ipairs( tArgs ) do
    local sPath = shell.resolve( v )
    if fs.exists( sPath ) then
        if fs.isDir( sPath ) then
            print( "/" .. sPath .. ": Directory" )
        else
            print( "/" .. sPath .. ": File" )
        end
    else
        print( "/" .. sPath .. ": No such path" )
    end
end

