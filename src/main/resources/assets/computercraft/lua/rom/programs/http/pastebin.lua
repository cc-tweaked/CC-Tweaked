local function printUsage()
    print( "Usages:" )
    print( "pastebin put <filename>" )
    print( "pastebin get <code> <filename>" )
    print( "pastebin run <code> <arguments>" )
end

if not http then
    printError( "Pastebin requires http API" )
    printError( "Set http_enable to true in ComputerCraft.cfg" )
    return
end

local pastebin = require('http.pastebin')

local tArgs = { ... }
local sCommand = tArgs[1]

if sCommand == "put" then
    -- Upload a file to pastebin.com

    if #tArgs < 2 then
        printUsage()
        return
    end

    -- Determine file to upload
    local sFile = tArgs[2]
    local sPath = shell.resolve( sFile )
    if not fs.exists( sPath ) or fs.isDir( sPath ) then
        print( "No such file" )
        return
    end

    print( "Connecting to pastebin.com... " )

    local resp, msg = pastebin.put(sPath)

    if resp then
        print( "Uploaded as " .. resp )
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

    local sCode = pastebin.parseCode(tArgs[2])
    if not sCode then
        return false, "Invalid pastebin code. The code is the ID at the end of the pastebin.com URL."
    end

    -- Determine file to download
    local sFile = tArgs[3]
    local sPath = shell.resolve( sFile )
    if fs.exists( sPath ) then
        printError( "File already exists" )
        return
    end

    print( "Connecting to pastebin.com... " )

    local resp, msg = pastebin.get(sCode, sPath)

    if resp then
        print( "Downloaded as " .. sPath )
    else
        printError( msg )
    end

elseif sCommand == "run" then
    -- Download and run a file from pastebin.com

    if #tArgs < 2 then
        printUsage()
        return
    end

    local sCode = pastebin.parseCode(tArgs[2])
    if not sCode then
        return false, "Invalid pastebin code. The code is the ID at the end of the pastebin.com URL."
    end

    print( "Connecting to pastebin.com... " )

    local res, msg = pastebin.download(sCode)
    if not res then
        printError( msg )
        return res, msg
    end

    res, msg = load(res, sCode, "t", _ENV)
    if not res then
        printError( msg )
        return res, msg
    end

    res, msg =  pcall(res, table.unpack(tArgs, 3))
    if not res then
        printError( msg )
    end
else

    printUsage()
    return
end

