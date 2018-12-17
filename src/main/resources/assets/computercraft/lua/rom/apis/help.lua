
local sPath = "/rom/help"

function path()
    return sPath
end

function setPath( _sPath )
    if type( _sPath ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( _sPath ) .. ")", 2 )
    end
    sPath = _sPath
end

function lookup( _sTopic )
    if type( _sTopic ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( _sTopic ) .. ")", 2 )
    end
     -- Look on the path variable
    for sPath in string.gmatch(sPath, "[^:]+") do
        sPath = fs.combine( sPath, _sTopic )
        if fs.exists( sPath ) and not fs.isDir( sPath ) then
            return sPath
        elseif fs.exists( sPath..".txt" ) and not fs.isDir( sPath..".txt" ) then
            return sPath..".txt"
        end
    end

    -- Not found
    return nil
end

function topics()
    -- Add index
    local tItems = {
        [ "index" ] = true
    }

    -- Add topics from the path
    for sPath in string.gmatch(sPath, "[^:]+") do
        if fs.isDir( sPath ) then
            local tList = fs.list( sPath )
            for n,sFile in pairs( tList ) do
                if string.sub( sFile, 1, 1 ) ~= "." then
                    if not fs.isDir( fs.combine( sPath, sFile ) ) then
                        if #sFile > 4 and sFile:sub(-4) == ".txt" then
                            sFile = sFile:sub(1,-5)
                        end
                        tItems[ sFile ] = true
                    end
                end
            end
        end
    end

    -- Sort and return
    local tItemList = {}
    for sItem, b in pairs( tItems ) do
        table.insert( tItemList, sItem )
    end
    table.sort( tItemList )
    return tItemList
end

function completeTopic( sText )
    if type( sText ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sText ) .. ")", 2 )
    end
    local tTopics = topics()
    local tResults = {}
    for n=1,#tTopics do
        local sTopic = tTopics[n]
        if #sTopic > #sText and string.sub( sTopic, 1, #sText ) == sText then
            table.insert( tResults, string.sub( sTopic, #sText + 1 ) )
        end
    end
    return tResults
end
