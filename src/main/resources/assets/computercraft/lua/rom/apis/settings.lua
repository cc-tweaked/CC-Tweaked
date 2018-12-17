
local tSettings = {}

function set( sName, value )
    if type( sName ) ~= "string" then error( "bad argument #1 (expected string, got " .. type( sName ) .. ")", 2 ) end

    local sValueTy = type(value)
    if sValueTy ~= "number" and sValueTy ~= "string" and sValueTy ~= "boolean" and sValueTy ~= "table" then
        error( "bad argument #2 (expected value, got " .. sValueTy .. ")", 2 )
    end
    if sValueTy == "table" then
        -- Ensure value is serializeable
        value = textutils.unserialize( textutils.serialize(value) )
    end
    tSettings[ sName ] = value
end

local copy
function copy( value )
    if type(value) == "table" then
        local result = {}
        for k,v in pairs(value) do
            result[k] = copy(v)
        end
        return result
    else
        return value
    end
end

function get( sName, default )
    if type(sName) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sName ) .. ")", 2 )
    end
    local result = tSettings[ sName ]
    if result ~= nil then
        return copy(result)
    else
        return default
    end
end

function unset( sName )
    if type(sName) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sName ) .. ")", 2 )
    end
    tSettings[ sName ] = nil
end

function clear()
    tSettings = {}
end

function getNames()
    local result = {}
    for k,v in pairs( tSettings ) do
        result[ #result + 1 ] = k
    end
    table.sort(result)
    return result
end

function load( sPath )
    if type(sPath) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sPath ) .. ")", 2 )
    end
    local file = fs.open( sPath, "r" )
    if not file then
        return false
    end

    local sText = file.readAll()
    file.close()

    local tFile = textutils.unserialize( sText )
    if type(tFile) ~= "table" then
        return false
    end

    for k,v in pairs(tFile) do
        if type(k) == "string" and
           (type(v) == "string" or type(v) == "number" or type(v) == "boolean" or type(v) == "table") then
            set( k, v )
        end
    end

    return true
end

function save( sPath )
    if type(sPath) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sPath ) .. ")", 2 )
    end
    local file = fs.open( sPath, "w" )
    if not file then
        return false
    end

    file.write( textutils.serialize( tSettings ) )
    file.close()

    return true
end
