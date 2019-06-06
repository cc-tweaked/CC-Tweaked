
local function printUsage()
    print( "Usage:" )
    print( "wget [run] <url> [filename]" )
end

local tArgs = { ... }
if #tArgs < 1 then
    printUsage()
    return
end

if not http then
    printError( "wget requires http API" )
    printError( "Set http_enable to true in ComputerCraft.cfg" )
    return
end

local function getFilename( sUrl )
    sUrl = sUrl:gsub( "[#?].*" , "" ):gsub( "/+$" , "" )
    return sUrl:match( "/([^/]+)$" )
end

local function get( sUrl )
    write( "Connecting to " .. sUrl .. "... " )

    local response = http.get( sUrl , nil , true )
    if not response then
        print( "Failed." )
        return nil
    end

    print( "Success." )

    local sResponse = response.readAll()
    response.close()
    return sResponse
end

-- Determine file to download
local sUrl
if tArgs[1] == "run" then
    if tArgs[2] == nil then
        printUsage()
        return
    else
      sUrl = tArgs[2]
    end
else
    sUrl = tArgs[1]
end

--Check if the URL is valid
local ok, err = http.checkURL( sUrl )
if not ok then
    printError( err or "Invalid URL." )
    return
end

local sFile = tArgs[2] or getFilename( sUrl )
local sPath = shell.resolve( sFile )
if fs.exists( sPath ) and tArgs[1] ~= "run" then
    print( "File already exists" )
    return
end

-- Do the get
local res = get( sUrl )
if res then
    if tArgs[1] == "run" then
        local func, err = load( res, sFile, "t", _ENV)
        if not func then
            printError( err )
            return
        end
        
        table.remove( tArgs, 1 )
        table.remove( tArgs, 1 )        
        func( table.unpack( tArgs ) )
    else
      local file = fs.open( sPath, "wb" )
      file.write( res )
      file.close()

      print( "Downloaded as "..sFile )
    end
end

