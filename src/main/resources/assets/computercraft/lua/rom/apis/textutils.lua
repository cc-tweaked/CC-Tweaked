local expect = dofile("rom/modules/main/cc/expect.lua").expect

function slowWrite( sText, nRate )
    expect(2, nRate, "number", "nil")
    nRate = nRate or 20
    if nRate < 0 then
        error( "Rate must be positive", 2 )
    end
    local nSleep = 1 / nRate

    sText = tostring( sText )
    local x,y = term.getCursorPos()
    local len = string.len( sText )

    for n=1,len do
        term.setCursorPos( x, y )
        sleep( nSleep )
        local nLines = write( string.sub( sText, 1, n ) )
        local newX, newY = term.getCursorPos()
        y = newY - nLines
    end
end

function slowPrint( sText, nRate )
    slowWrite( sText, nRate )
    print()
end

function formatTime( nTime, bTwentyFourHour )
    expect(1, nTime, "number")
    expect(2, bTwentyFourHour, "boolean", "nil")
    local sTOD = nil
    if not bTwentyFourHour then
        if nTime >= 12 then
            sTOD = "PM"
        else
            sTOD = "AM"
        end
        if nTime >= 13 then
            nTime = nTime - 12
        end
    end

    local nHour = math.floor(nTime)
    local nMinute = math.floor((nTime - nHour)*60)
    if sTOD then
        return string.format( "%d:%02d %s", nHour, nMinute, sTOD )
    else
        return string.format( "%d:%02d", nHour, nMinute )
    end
end

local function makePagedScroll( _term, _nFreeLines )
    local nativeScroll = _term.scroll
    local nFreeLines = _nFreeLines or 0
    return function( _n )
        for n=1,_n do
            nativeScroll( 1 )

            if nFreeLines <= 0 then
                local w,h = _term.getSize()
                _term.setCursorPos( 1, h )
                _term.write( "Press any key to continue" )
                os.pullEvent( "key" )
                _term.clearLine()
                _term.setCursorPos( 1, h )
            else
                nFreeLines = nFreeLines - 1
            end
        end
    end
end

function pagedPrint( _sText, _nFreeLines )
    expect(2, _nFreeLines, "number", "nil")
    -- Setup a redirector
    local oldTerm = term.current()
    local newTerm = {}
    for k,v in pairs( oldTerm ) do
        newTerm[k] = v
    end
    newTerm.scroll = makePagedScroll( oldTerm, _nFreeLines )
    term.redirect( newTerm )

    -- Print the text
    local result
    local ok, err = pcall( function()
        if _sText ~= nil then
            result = print( _sText )
        else
            result = print()
        end
    end )

    -- Removed the redirector
    term.redirect( oldTerm )

    -- Propogate errors
    if not ok then
        error( err, 0 )
    end
    return result
end

local function tabulateCommon( bPaged, ... )
    local tAll = table.pack(...)
    for i = 1, tAll.n do
        expect(i, tAll[i], "number", "table")
    end

    local w,h = term.getSize()
    local nMaxLen = w / 8
    for n, t in ipairs( tAll ) do
        if type(t) == "table" then
            for nu, sItem in pairs(t) do
                if type( sItem ) ~= "string" then
                    error( "bad argument #"..n.."."..nu.." (expected string, got " .. type( sItem ) .. ")", 3 )
                end
                nMaxLen = math.max( string.len( sItem ) + 1, nMaxLen )
            end
        end
    end
    local nCols = math.floor( w / nMaxLen )
    local nLines = 0
    local function newLine()
        if bPaged and nLines >= (h-3) then
            pagedPrint()
        else
            print()
        end
        nLines = nLines + 1
    end

    local function drawCols( _t )
        local nCol = 1
        for n, s in ipairs( _t ) do
            if nCol > nCols then
                nCol = 1
                newLine()
            end

            local cx, cy = term.getCursorPos()
            cx = 1 + ((nCol - 1) * nMaxLen)
            term.setCursorPos( cx, cy )
            term.write( s )

            nCol = nCol + 1
        end
        print()
    end
    for n, t in ipairs( tAll ) do
        if type(t) == "table" then
            if #t > 0 then
                drawCols( t )
            end
        elseif type(t) == "number" then
            term.setTextColor( t )
        end
    end
end

function tabulate( ... )
    return tabulateCommon( false, ... )
end

function pagedTabulate( ... )
    return tabulateCommon( true, ... )
end

local g_tLuaKeywords = {
    [ "and" ] = true,
    [ "break" ] = true,
    [ "do" ] = true,
    [ "else" ] = true,
    [ "elseif" ] = true,
    [ "end" ] = true,
    [ "false" ] = true,
    [ "for" ] = true,
    [ "function" ] = true,
    [ "if" ] = true,
    [ "in" ] = true,
    [ "local" ] = true,
    [ "nil" ] = true,
    [ "not" ] = true,
    [ "or" ] = true,
    [ "repeat" ] = true,
    [ "return" ] = true,
    [ "then" ] = true,
    [ "true" ] = true,
    [ "until" ] = true,
    [ "while" ] = true,
}

local function serializeImpl( t, tTracking, sIndent )
    local sType = type(t)
    if sType == "table" then
        if tTracking[t] ~= nil then
            error( "Cannot serialize table with recursive entries", 0 )
        end
        tTracking[t] = true

        if next(t) == nil then
            -- Empty tables are simple
            return "{}"
        else
            -- Other tables take more work
            local sResult = "{\n"
            local sSubIndent = sIndent .. "  "
            local tSeen = {}
            for k,v in ipairs(t) do
                tSeen[k] = true
                sResult = sResult .. sSubIndent .. serializeImpl( v, tTracking, sSubIndent ) .. ",\n"
            end
            for k,v in pairs(t) do
                if not tSeen[k] then
                    local sEntry
                    if type(k) == "string" and not g_tLuaKeywords[k] and string.match( k, "^[%a_][%a%d_]*$" ) then
                        sEntry = k .. " = " .. serializeImpl( v, tTracking, sSubIndent ) .. ",\n"
                    else
                        sEntry = "[ " .. serializeImpl( k, tTracking, sSubIndent ) .. " ] = " .. serializeImpl( v, tTracking, sSubIndent ) .. ",\n"
                    end
                    sResult = sResult .. sSubIndent .. sEntry
                end
            end
            sResult = sResult .. sIndent .. "}"
            return sResult
        end

    elseif sType == "string" then
        return string.format( "%q", t )

    elseif sType == "number" or sType == "boolean" or sType == "nil" then
        return tostring(t)

    else
        error( "Cannot serialize type "..sType, 0 )

    end
end

empty_json_array = setmetatable({}, {
    __newindex = function()
        error("attempt to mutate textutils.empty_json_array", 2)
    end
})

local function serializeJSONImpl( t, tTracking, bNBTStyle )
    local sType = type(t)
    if t == empty_json_array then
        return "[]"

    elseif sType == "table" then
        if tTracking[t] ~= nil then
            error( "Cannot serialize table with recursive entries", 0 )
        end
        tTracking[t] = true

        if next(t) == nil then
            -- Empty tables are simple
            return "{}"
        else
            -- Other tables take more work
            local sObjectResult = "{"
            local sArrayResult = "["
            local nObjectSize = 0
            local nArraySize = 0
            for k,v in pairs(t) do
                if type(k) == "string" then
                    local sEntry
                    if bNBTStyle then
                        sEntry = tostring(k) .. ":" .. serializeJSONImpl( v, tTracking, bNBTStyle )
                    else
                        sEntry = string.format( "%q", k ) .. ":" .. serializeJSONImpl( v, tTracking, bNBTStyle )
                    end
                    if nObjectSize == 0 then
                        sObjectResult = sObjectResult .. sEntry
                    else
                        sObjectResult = sObjectResult .. "," .. sEntry
                    end
                    nObjectSize = nObjectSize + 1
                end
            end
            for n,v in ipairs(t) do
                local sEntry = serializeJSONImpl( v, tTracking, bNBTStyle )
                if nArraySize == 0 then
                    sArrayResult = sArrayResult .. sEntry
                else
                    sArrayResult = sArrayResult .. "," .. sEntry
                end
                nArraySize = nArraySize + 1
            end
            sObjectResult = sObjectResult .. "}"
            sArrayResult = sArrayResult .. "]"
            if nObjectSize > 0 or nArraySize == 0 then
                return sObjectResult
            else
                return sArrayResult
            end
        end

    elseif sType == "string" then
        return string.format( "%q", t )

    elseif sType == "number" or sType == "boolean" then
        return tostring(t)

    else
        error( "Cannot serialize type "..sType, 0 )

    end
end

function serialize( t )
    local tTracking = {}
    return serializeImpl( t, tTracking, "" )
end

function unserialize( s )
    expect(1, s, "string")
    local func = load( "return "..s, "unserialize", "t", {} )
    if func then
        local ok, result = pcall( func )
        if ok then
            return result
        end
    end
    return nil
end

function serializeJSON( t, bNBTStyle )
    expect(1, t, "table", "string", "number", "boolean")
    expect(2, bNBTStyle, "boolean", "nil")
    local tTracking = {}
    return serializeJSONImpl( t, tTracking, bNBTStyle or false )
end

function urlEncode( str )
    expect(1, str, "string")
    if str then
        str = string.gsub(str, "\n", "\r\n")
        str = string.gsub(str, "([^A-Za-z0-9 %-%_%.])", function(c)
            local n = string.byte(c)
            if n < 128 then
                -- ASCII
                return string.format("%%%02X", n)
            else
                -- Non-ASCII (encode as UTF-8)
                return
                    string.format("%%%02X", 192 + bit32.band( bit32.arshift(n,6), 31 ) ) ..
                    string.format("%%%02X", 128 + bit32.band( n, 63 ) )
            end
        end )
        str = string.gsub(str, " ", "+")
    end
    return str
end

local tEmpty = {}
function complete( sSearchText, tSearchTable )
    expect(1, sSearchText, "string")
    expect(2, tSearchTable, "table", "nil")

    if g_tLuaKeywords[sSearchText] then return tEmpty end
    local nStart = 1
    local nDot = string.find( sSearchText, ".", nStart, true )
    local tTable = tSearchTable or _ENV
    while nDot do
        local sPart = string.sub( sSearchText, nStart, nDot - 1 )
        local value = tTable[ sPart ]
        if type( value ) == "table" then
            tTable = value
            nStart = nDot + 1
            nDot = string.find( sSearchText, ".", nStart, true )
        else
            return tEmpty
        end
    end
    local nColon = string.find( sSearchText, ":", nStart, true )
    if nColon then
        local sPart = string.sub( sSearchText, nStart, nColon - 1 )
        local value = tTable[ sPart ]
        if type( value ) == "table" then
            tTable = value
            nStart = nColon + 1
        else
            return tEmpty
        end
    end

    local sPart = string.sub( sSearchText, nStart )
    local nPartLength = string.len( sPart )

    local tResults = {}
    local tSeen = {}
    while tTable do
        for k,v in pairs( tTable ) do
            if not tSeen[k] and type(k) == "string" then
                if string.find( k, sPart, 1, true ) == 1 then
                    if not g_tLuaKeywords[k] and string.match( k, "^[%a_][%a%d_]*$" ) then
                        local sResult = string.sub( k, nPartLength + 1 )
                        if nColon then
                            if type(v) == "function" then
                                table.insert( tResults, sResult .. "(" )
                            elseif type(v) == "table" then
                                local tMetatable = getmetatable( v )
                                if tMetatable and ( type( tMetatable.__call ) == "function" or  type( tMetatable.__call ) == "table" ) then
                                    table.insert( tResults, sResult .. "(" )
                                end
                            end
                        else
                            if type(v) == "function" then
                                sResult = sResult .. "("
                            elseif type(v) == "table" and next(v) ~= nil then
                                sResult = sResult .. "."
                            end
                            table.insert( tResults, sResult )
                        end
                    end
                end
            end
            tSeen[k] = true
        end
        local tMetatable = getmetatable( tTable )
        if tMetatable and type( tMetatable.__index ) == "table" then
            tTable = tMetatable.__index
        else
            tTable = nil
        end
    end

    table.sort( tResults )
    return tResults
end

-- GB versions
serialise = serialize
unserialise = unserialize
serialiseJSON = serializeJSON
