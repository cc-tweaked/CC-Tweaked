
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

    local success, msg = pastebin.put(sFile)

    if success then
        print( "Uploaded as "..msg )
        print( "Run \"pastebin get "..msg.."\" to download anywhere" )

    else
        print( msg )
    end

elseif sCommand == "get" then
    -- Download a file from pastebin.com
    if #tArgs < 3 then
        printUsage()
        return
    end

    -- Determine file to download
    local sCode = tArgs[2]
    local sFile = tArgs[3]
    local sPath = shell.resolve( sFile )
    if fs.exists( sPath ) then
        print( "File already exists" )
        return
    end

    local success, msg = pastebin.get(sCode, sPath)

    if success then
        print( "Downloaded as "..sFile )
    else
        print(msg)
    end
elseif sCommand == "run" then
    local sCode = tArgs[2]

    local success, msg = pastebin.run(sCode, table.unpack(tArgs, 3))
    if not success then
        print(msg)
    end
else
    printUsage()
    return
end
