
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

local function get(paste)
    write( "Connecting to pastebin.com... " )
    -- Add a cache buster so that spam protection is re-checked
    local cacheBuster = tostring(math.random())
    local response = http.get(
        "https://pastebin.com/raw/"..textutils.urlEncode( paste ).."?cb="..cacheBuster
    )

    if response then
        -- If spam protection is activated, we get redirected to /paste with Content-Type: text/html
        local headers = response.getResponseHeaders()
        if not headers["Content-Type"] or not headers["Content-Type"]:find( "^text/plain" ) then
            printError( "\nFailed to get paste. Please complete the captcha in a web browser: https://pastebin.com/"..textutils.urlEncode( paste ) )
            return
        end

        print( "Success." )

        local sResponse = response.readAll()
        response.close()
        return sResponse
    else
        print( "Failed." )
    end
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

    -- Read in the file
    local sName = fs.getName( sPath )
    local file = fs.open( sPath, "r" )
    local sText = file.readAll()
    file.close()

    -- POST the contents to pastebin
    write( "Connecting to pastebin.com... " )
    local key = "0ec2eb25b6166c0c27a394ae118ad829"
    local response = http.post(
        "https://pastebin.com/api/api_post.php",
        "api_option=paste&"..
        "api_dev_key="..key.."&"..
        "api_paste_format=lua&"..
        "api_paste_name="..textutils.urlEncode(sName).."&"..
        "api_paste_code="..textutils.urlEncode(sText)
    )

    if response then
        print( "Success." )

        local sResponse = response.readAll()
        response.close()

        local sCode = string.match( sResponse, "[^/]+$" )
        print( "Uploaded as "..sResponse )
        print( "Run \"pastebin get "..sCode.."\" to download anywhere" )

    else
        print( "Failed." )
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

    -- GET the contents from pastebin
    local res = get(sCode)
    if res then
        local file = fs.open( sPath, "w" )
        file.write( res )
        file.close()

        print( "Downloaded as "..sFile )
    end
elseif sCommand == "run" then
    local sCode = tArgs[2]

    local res = get(sCode)
    if res then
        local func, err = load(res, sCode, "t", _ENV)
        if not func then
            printError( err )
            return
        end
        local success, msg = pcall(func, select(3, ...))
        if not success then
            printError( msg )
        end
    end
else
    printUsage()
    return
end
