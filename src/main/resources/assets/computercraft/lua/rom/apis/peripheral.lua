local native = peripheral

function getNames()
    local tResults = {}
    for n,sSide in ipairs( rs.getSides() ) do
        if native.isPresent( sSide ) then
            table.insert( tResults, sSide )
            if native.getType( sSide ) == "modem" and not native.call( sSide, "isWireless" ) then
                local tRemote = native.call( sSide, "getNamesRemote" )
                for n,sName in ipairs( tRemote ) do
                    table.insert( tResults, sName )
                end
            end
        end
    end
    return tResults
end

function isPresent( _sSide )
    if type( _sSide ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( _sSide ) .. ")", 2 )
    end
    if native.isPresent( _sSide ) then
        return true
    end
    for n,sSide in ipairs( rs.getSides() ) do
        if native.getType( sSide ) == "modem" and not native.call( sSide, "isWireless" ) then
            if native.call( sSide, "isPresentRemote", _sSide )  then
                return true
            end
        end
    end
    return false
end

function getType( _sSide )
    if type( _sSide ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( _sSide ) .. ")", 2 )
    end
    if native.isPresent( _sSide ) then
        return native.getType( _sSide )
    end
    for n,sSide in ipairs( rs.getSides() ) do
        if native.getType( sSide ) == "modem" and not native.call( sSide, "isWireless" ) then
            if native.call( sSide, "isPresentRemote", _sSide )  then
                return native.call( sSide, "getTypeRemote", _sSide )
            end
        end
    end
    return nil
end

function getMethods( _sSide )
    if type( _sSide ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( _sSide ) .. ")", 2 )
    end
    if native.isPresent( _sSide ) then
        return native.getMethods( _sSide )
    end
    for n,sSide in ipairs( rs.getSides() ) do
        if native.getType( sSide ) == "modem" and not native.call( sSide, "isWireless" ) then
            if native.call( sSide, "isPresentRemote", _sSide )  then
                return native.call( sSide, "getMethodsRemote", _sSide )
            end
        end
    end
    return nil
end

function call( _sSide, _sMethod, ... )
    if type( _sSide ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( _sSide ) .. ")", 2 )
    end
    if type( _sSide ) ~= "string" then
        error( "bad argument #2 (expected string, got " .. type( _sMethod ) .. ")", 2 )
    end
    if native.isPresent( _sSide ) then
        return native.call( _sSide, _sMethod, ... )
    end
    for n,sSide in ipairs( rs.getSides() ) do
        if native.getType( sSide ) == "modem" and not native.call( sSide, "isWireless" ) then
            if native.call( sSide, "isPresentRemote", _sSide )  then
                return native.call( sSide, "callRemote", _sSide, _sMethod, ... )
            end
        end
    end
    return nil
end

function wrap( _sSide )
    if type( _sSide ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( _sSide ) .. ")", 2 )
    end
    if peripheral.isPresent( _sSide ) then
        local tMethods = peripheral.getMethods( _sSide )
        local tResult = {}
        for n,sMethod in ipairs( tMethods ) do
            tResult[sMethod] = function( ... )
                return peripheral.call( _sSide, sMethod, ... )
            end
        end
        return tResult
    end
    return nil
end

function find( sType, fnFilter )
    if type( sType ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sType ) .. ")", 2 )
    end
    if fnFilter ~= nil and type( fnFilter ) ~= "function" then
        error( "bad argument #2 (expected function, got " .. type( fnFilter ) .. ")", 2 )
    end
    local tResults = {}
    for n,sName in ipairs( peripheral.getNames() ) do
        if peripheral.getType( sName ) == sType then
            local wrapped = peripheral.wrap( sName )
            if fnFilter == nil or fnFilter( sName, wrapped ) then
                table.insert( tResults, wrapped )
            end
        end
    end
    return table.unpack( tResults )
end
