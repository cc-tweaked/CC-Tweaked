
local function printUsage()
    print( "Usages:" )
    print( "pastebin put <filename>" )
    print( "pastebin get <code> <filename>" )
    print( "pastebin run <code> <arguments>" )
end

local tArgs = { ... }
if #tArgs < 2 then
    printUsage()
    return
end

if not http then
    printError( "Pastebin requires http API" )
    printError( "Set http_enable to true in ComputerCraft.cfg" )
    return
end

local pastebin = require('http.pastebin')

local sCommand = tArgs[1]
if sCommand == "put" then
    -- Upload a file to pastebin.com
    -- Determine file to upload
    local sFile = tArgs[2]
    local sPath = shell.resolve( sFile )
    if not fs.exists( sPath ) or fs.isDir( sPath ) then
        print( "No such file" )
        return
    end

    print( "Connecting to pastebin.com... " )

    local resp, msg = pastebin.put(sFile)

    if resp then
        print( "Uploaded as "..msg )
        print( "Run \"pastebin get "..resp.."\" to download anywhere" )

    else
        printError( msg )
    end

elseif sCommand == "get" then
    -- Download a file from pastebin.com
    if #tArgs < 3 then
        printUsage()
        return
    end

    print( "Connecting to pastebin.com... " )

    -- Determine file to download
    local sCode = tArgs[2]
    local sFile = tArgs[3]
    local sPath = shell.resolve( sFile )
    if fs.exists( sPath ) then
        printError( "File already exists" )
        return
    end

    local resp, msg = pastebin.get(sCode, sPath)

    if resp then
        print( "Downloaded as "..resp )
    else
        printError( msg )
    end

elseif sCommand == "run" then
    local sCode = tArgs[2]

    print( "Connecting to pastebin.com... " )

    local resp, msg = pastebin.run(sCode, table.unpack(tArgs, 3))
    if not resp then
        printError( msg )
    end
else

    printUsage()
    return
end
