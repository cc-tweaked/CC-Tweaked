local expect = dofile("rom/modules/main/cc/expect.lua").expect

local native = peripheral

function getNames()
    local tResults = {}
    for _, sSide in ipairs( rs.getSides() ) do
        if native.isPresent( sSide ) then
            table.insert( tResults, sSide )
            if native.getType( sSide ) == "modem" and not native.call( sSide, "isWireless" ) then
                local tRemote = native.call( sSide, "getNamesRemote" )
                for _, sName in ipairs( tRemote ) do
                    table.insert( tResults, sName )
                end
            end
        end
    end
    return tResults
end

function isPresent( _sSide )
    expect(1, _sSide, "string")
    if native.isPresent( _sSide ) then
        return true
    end
    for _, sSide in ipairs( rs.getSides() ) do
        if native.getType( sSide ) == "modem" and not native.call( sSide, "isWireless" ) then
            if native.call( sSide, "isPresentRemote", _sSide )  then
                return true
            end
        end
    end
    return false
end

function getType( _sSide )
    expect(1, _sSide, "string")
    if native.isPresent( _sSide ) then
        return native.getType( _sSide )
    end
    for _, sSide in ipairs( rs.getSides() ) do
        if native.getType( sSide ) == "modem" and not native.call( sSide, "isWireless" ) then
            if native.call( sSide, "isPresentRemote", _sSide )  then
                return native.call( sSide, "getTypeRemote", _sSide )
            end
        end
    end
    return nil
end

function getMethods( _sSide )
    expect(1, _sSide, "string")
    if native.isPresent( _sSide ) then
        return native.getMethods( _sSide )
    end
    for _, sSide in ipairs( rs.getSides() ) do
        if native.getType( sSide ) == "modem" and not native.call( sSide, "isWireless" ) then
            if native.call( sSide, "isPresentRemote", _sSide )  then
                return native.call( sSide, "getMethodsRemote", _sSide )
            end
        end
    end
    return nil
end

function call( _sSide, _sMethod, ... )
    expect(1, _sSide, "string")
    expect(2, _sMethod, "string")
    if native.isPresent( _sSide ) then
        return native.call( _sSide, _sMethod, ... )
    end
    for _, sSide in ipairs( rs.getSides() ) do
        if native.getType( sSide ) == "modem" and not native.call( sSide, "isWireless" ) then
            if native.call( sSide, "isPresentRemote", _sSide )  then
                return native.call( sSide, "callRemote", _sSide, _sMethod, ... )
            end
        end
    end
    return nil
end

function wrap( _sSide )
    expect(1, _sSide, "string")
    if peripheral.isPresent( _sSide ) then
        local tMethods = peripheral.getMethods( _sSide )
        local tResult = {}
        for _, sMethod in ipairs( tMethods ) do
            tResult[sMethod] = function( ... )
                return peripheral.call( _sSide, sMethod, ... )
            end
        end
        return tResult
    end
    return nil
end

function find( sType, fnFilter )
    expect(1, sType, "string")
    expect(2, fnFilter, "function", "nil")
    local tResults = {}
    for _, sName in ipairs( peripheral.getNames() ) do
        if peripheral.getType( sName ) == sType then
            local wrapped = peripheral.wrap( sName )
            if fnFilter == nil or fnFilter( sName, wrapped ) then
                table.insert( tResults, wrapped )
            end
        end
    end
    return table.unpack( tResults )
end
