local tArgs = { ... }

if #tArgs == 0 then
    printError( "Usage: webrun <url>" )
    return
end

if not http then
    printError( "webrun requires http API" )
    printError( "Set http_enable to true in ComputerCraft.cfg" )
    return
end

local response, err = http.get( tArgs[1] )

if not response then
    printError( err )
    return
end

local sProgram = response.readAll()
response.close()

local func, err = load( sProgram, nil, "t", _ENV)

if not func then
    printError( err )
    return
end

table.remove( tArgs, 1 )

func( table.unpack( tArgs ) )
