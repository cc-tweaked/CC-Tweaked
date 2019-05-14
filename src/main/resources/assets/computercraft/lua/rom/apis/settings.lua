
local tSettings = {}

function set( sName, value )
    expect(1, sName, "string")
    expect(2, value, "number", "string", "boolean", "table")

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
    expect(1, sName, "string")
    local result = tSettings[ sName ]
    if result ~= nil then
        return copy(result)
    else
        return default
    end
end

function unset( sName )
    expect(1, sName, "string")
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
    expect(1, sPath, "string")
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
    expect(1, sPath, "string")
    local file = fs.open( sPath, "w" )
    if not file then
        return false
    end

    file.write( textutils.serialize( tSettings ) )
    file.close()

    return true
end
