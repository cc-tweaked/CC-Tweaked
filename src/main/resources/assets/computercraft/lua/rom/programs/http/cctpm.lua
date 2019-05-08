local cctpm = require("cctpm")

local tArgs = { ... }

local ok, err = cctpm.readRepos()
if not ok then
    print(err)
end

if tArgs[1] == "install" then
    if not tArgs[2] then
        print("Usage: cctpm install <package>")
        return
    end
    local ok, err = cctpm.install( tArgs[2] )
    if not ok then
        printError( err )
    end
elseif tArgs[1] == "remove" then
    if not tArgs[2] then
        print("Usage: cctpm remove <package>")
        return
    end
    local ok, err = cctpm.remove( tArgs[2] )
    if not ok then
        printError( err )
    end
elseif tArgs[1] == "upgrade" then
    local tList = cctpm.getUpgradeable()
    if #tList == 0 then
        print( "All Packages are up to date!" )
        return
    end
    print( "The following Packages can be upgradet:" )
    for k, v in ipairs( tList ) do
        write( v )
    end
    print( "\nDo you want to Continue? [Y|N]" )
    while true do
        local ev, key = os.pullEvent("key_up")
        if key == keys.n then
            return
        elseif key == keys.y then
            break
        end
    end
    for k, v in ipairs( tList ) do
        local ok, err = cctpm.install( v, true )
        if not ok then
            printError( err )
        end
    end
elseif tArgs[1] == "list" then
    textutils.pagedTabulate( cctpm.list() )
elseif tArgs[1] == "installed" then
    textutils.pagedTabulate( cctpm.getInstalledPackages() )
elseif tArgs[1] == "info" then
    if not tArgs[2] then
        print("Usage: cctpm info <package>")
        return
    end
    local tInfo = cctpm.getPackageInfo( tArgs[2] )
    if not tInfo then
        print( 'Package "'..tArgs[2]..'" not found' )
        return
    end
    print( tInfo.name )
    print( tInfo.description )
    if tInfo.installed then
        print( "Installed: Yes" )
    else
        print( "Installed: No" )
    end
    if #tInfo.dependencies == 0 then
        print( "Dependencies: None" )
    else
        write( "Dependencies:" )
        for k, v in ipairs( tInfo.dependencies ) do
            write( v )
        end
        print()
    end
else 
    print("Unknown Argument")
end
