local cctpm = {}

cctpm_cache = {}

local function downloadFile( sURL, sPath )
    local response, err = http.get( sURL )
    if err then
        return false, err
    end
    local handle = fs.open( sPath, "w" )
    handle.write( response.readAll() )
    response.close()
    handle.close()
    return true
end

local function addRepo( sRepo, sName, tInstalledList )
    local tRepo = textutils.unserialise( sRepo )
    if type( tRepo ) ~= "table" then
        return false, "Can't Read Repo \""..sName..'"'
    end
    for a, b in pairs( tRepo) do 
        cctpm_cache.packages[a] = b
        if tInstalledList[a] then
            cctpm_cache.packages[a]["installed"] = true
        else
            cctpm_cache.packages[a]["installed"] = false
        end 
    end
    return true
end

function cctpm.readRepos()
    cctpm_cache.packages = {}
    local sDirectory = fs.combine( settings.get( "cctpm.directory", "/var/cctpm" ), "" )
    local tInstalledList = {}
    if fs.exists( sDirectory.."/installed.txt" ) then
        for sLine in io.lines( sDirectory.."/installed.txt" ) do
            local sPackageName = sLine:match("([^,]+);([^,]+)")
            tInstalledList[sPackageName] = true
        end
    end
    local sRepoList = settings.get( "cctpm.repolist", "" )
        for sRepo in sRepoList:gmatch( sRepoList, "[^;]+") do
            local sType, sMeta = sRepo:match("([^,]+)|([^,]+)")
            if sType == "repo" then
                sName, sURL = sMeta:match("([^,]+):([^,]+)")
                local response, err = http.get( sURL )
                if err then
                    return false, err
                end
                local ok, err = addRepo( response.readAll() , sName, tInstalledList )
                response.close()
                if not ok then
                    return false, err
                end
            elseif sType == "repolist" then
                local ok, err = downloadFile( sMeta, "/tmp/repolist.txt" )
                if not ok then
                    return false, err
                end
                for sLine in io.lines( "/tmp/repolist.txt" ) do
                    sName, sURL = sLine:match("([^,]+)|([^,]+)")
                    local response, err = http.get( sURL )
                    if err then
                        return false, err
                    end
                    local ok, err = addRepo( response.readAll() , sName, tInstalledList )
                    response.close()
                    if not ok then
                        return false, err
                    end
                end
            end
        end
    fs.mkdir( sDirectory.."/repos" )
    for k, v in ipairs( fs.list( sDirectory.."/repos" ) ) do
        local sName = v:sub( 1, -5 )
        local handle = fs.open( sDirectory.."/repos/"..v, "r" )
        local sContent = handle.readAll()
        handle.close()
        local ok, err = addRepo( sContent, sName, tInstalledList )
        if not ok then
            return false, err
        end
    end
    return true
end

local function findDependencies( sPackage, sParent )
    if cctpm_cache.err then
        return
    end
    if not cctpm_cache.packages[sPackage] then
        cctpm_cache.err = '"'..sParent..'" depends to the the package "'..sPackage..'" which doesen\'t exists'
        return
    end
    for k,v in ipairs(cctpm_cache.packages[sPackage]["dependencies"]) do
        if not cctpm_cache.dependencies[v] then
            cctpm_cache.dependencies[v] = true
            findDependencies( v, sPackage )
        end
    end
end

function cctpm.getDependencies( sPackage )
    if type( sPackage ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sPackage ) .. ")", 2 )
    end
    if cctpm_cache.packages[sPackage] == nil then
        return nil, "Can't find Package \""..sPackage..'"'
    end
    cctpm_cache.dependencies = {}
    cctpm_cache.err = nil
    findDependencies( sPackage )
    if cctpm_cache.err then
        return nil, cctpm_cache.err
    else
        local tDependencies = {}
        for k, v in pairs( cctpm_cache.dependencies ) do
            table.insert( tDependencies, k )
        end
        cctpm_cache.dependencies = nil
        return tDependencies
    end
end

function cctpm.getInstalledPackages()
    local sDirectory = fs.combine( settings.get( "cctpm.directory", "/var/cctpm" ), "" )
    if not fs.exists( sDirectory.."/installed.txt" ) then
        return {}
    end
    local tInstalled = {}
    for sPackage in io.lines( sDirectory.."/installed.txt" ) do
        local sName = sPackage:match("([^,]+);([^,]+)")
        table.insert( tInstalled, sName )
    end
    return tInstalled
end

function cctpm.install( sPackage, bForce )
    if type( sPackage ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sPackage ) .. ")", 2 )
    end
    if bForce ~= nil and type( bForce ) ~= "boolean" then
        error( "bad argument #2 (expected boolean, got " .. type( bForce ) .. ")", 2 ) 
    end
    if cctpm_cache.packages[sPackage] == nil then
        return false, "Can't find Package \""..sPackage..'"'
    end
    local tInstall, err = cctpm.getDependencies( sPackage )
    if not tInstall then
        return false, err
    end
    table.insert( tInstall, sPackage )
    local sDirectory = fs.combine( settings.get( "cctpm.directory", "/var/cctpm" ), "" )
    for a, sInstallPack in ipairs( tInstall ) do
        if ( not cctpm_cache.packages[sInstallPack]["installed"] ) or ( bForce and sInstallPack == sPackage ) then
            if not cctpm_cache.packages[sInstallPack]["files"] then
                return false, '"'..sInstallPack..'" has no Filelist'
            end
            if not cctpm_cache.packages[sInstallPack]["version"] then
                return false, '"'..sInstallPack..'" has no Version'
            end
            for k,v in pairs(cctpm_cache.packages[sInstallPack]["files"]) do
                local ok, err = downloadFile( k, v )
                if not ok then
                    return false, err
                end
            end
            if not cctpm_cache.packages[sInstallPack]["installed"] then
                local file = fs.open( sDirectory.."/installed.txt", "a" )
                file.writeLine( sInstallPack..";"..cctpm_cache.packages[sInstallPack]["version"] )
                file.close()
            end
            cctpm_cache.packages[sInstallPack]["installed"] = true
        end
    end
    return true
end

function cctpm.remove( sPackage, bForce )
    if type( sPackage ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sPackage ) .. ")", 2 )
    end
    if bForce ~= nil and type( bForce ) ~= "boolean" then
        error( "bad argument #2 (expected boolean, got " .. type( bForce ) .. ")", 2 ) 
    end
    if cctpm_cache.packages[sPackage] == nil then
        return false, "Can't find Package \""..sPackage..'"'
    end
    if ( not cctpm_cache.packages[sPackage]["installed"] ) and not bForce then
        return false, 'Package "'..sPackage..'" is not installed'
    end
    for k,v in pairs(cctpm_cache.packages[sPackage]["files"]) do
        fs.delete( v )
    end
    cctpm_cache.packages[sPackage]["installed"] = false
    local sInstalledFile = fs.combine( settings.get( "cctpm.directory", "/var/cctpm" ), "installed.txt" )
    if fs.exists( sInstalledFile ) then
        local tInstalled = {}
        for sLine in io.lines( sInstalledFile ) do
            if not sLine:find( sPackage ) then
                table.insert( tInstalled, sLine )
            end
        end
        local handle = fs.open( sInstalledFile, "w" )
        for k,v in ipairs( tInstalled ) do
            handle.writeLine( v )
        end
    end
    return true
end

function cctpm.getUpgradeable()
    local sInstalledFile = fs.combine( settings.get( "cctpm.directory", "/var/cctpm" ), "installed.txt" )
    local tUpgrade = {}
    if fs.exists( sInstalledFile ) then
        for sLine in io.lines( sInstalledFile ) do
            local sName, sVersion = sLine:match("([^,]+);([^,]+)")
            if cctpm_cache.packages[sName]["version"] ~= sVersion then
                table.insert( tUpgrade, sName )
            end
        end
    end
    return tUpgrade
end

function cctpm.list()
    local tList = {}
    for k,v in pairs( cctpm_cache.packages ) do
        table.insert( tList, k )
    end
    return tList
end

function cctpm.getPackageInfo( sPackage )
    if type( sPackage ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sPackage ) .. ")", 2 )
    end
    if cctpm_cache.packages[sPackage] then
        return cctpm_cache.packages[sPackage]
    else
        return nil
    end
end

return cctpm
