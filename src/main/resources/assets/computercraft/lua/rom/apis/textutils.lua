function prompt( _tOptions )
    local tVerify = {replaceChar = "string", history = "table", complete = "function", prefix = "string", limit = "number", newline = "boolean", completeBGColor = "number", completeTextColor = "number", filter = "function", customkeys = "table"}
    if not _tOptions then
        _tOptions = {}
    end
    for k, v in pairs(tVerify) do
        if _tOptions[k] ~= nil and type( _tOptions[k] ) ~= v then
            error( "bad argument " .. k .. " (expected " .. v .. " got ".. type( _tOptions[k] ) .. ")", 2)
        end
    end
    
    term.setCursorBlink( true )

    local sLine
    if type( _tOptions.prefix ) == "string" then
        sLine = _tOptions.prefix
    else
        sLine = ""
    end
    local nhistoryPos
    local nPos = #sLine
    if _tOptions.replaceChar then
        _tOptions.replaceChar = string.sub( _tOptions.replaceChar, 1, 1 )
    end
    if not _tOptions.customkeys then
        _tOptions.customkeys = {}
    end
    local tCustomKeyNames = {enter = keys.enter, up = keys.up, down = keys.down, left = keys.left, right = keys.right, backspace = keys.backspace, home = keys.home, delete = keys.delete, tab = keys.tab, ["end"] = keys["end"]}
    for k, v in pairs(tCustomKeyNames) do
        if not _tOptions.customkeys[k] then
            _tOptions.customkeys[k] = v
        end
    end

    local tCompletions
    local nCompletion
    local function recomplete()
        if _tOptions.complete and nPos == string.len(sLine) then
            tCompletions = _tOptions.complete( sLine )
            if tCompletions and #tCompletions > 0 then
                nCompletion = 1
            else
                nCompletion = nil
            end
        else
            tCompletions = nil
            nCompletion = nil
        end
    end

    local function uncomplete()
        tCompletions = nil
        nCompletion = nil
    end
    
    local w
    if not _tOptions.limit then
        w = term.getSize()
    else
        w = _tOptions.limit
    end
    
    local sx = term.getCursorPos()

    local function redraw( _bClear )
        local nScroll = 0
        if sx + nPos >= w then
            nScroll = (sx + nPos) - w
        end

        local cx,cy = term.getCursorPos()
        term.setCursorPos( sx, cy )
        local sReplace = (_bClear and " ") or _tOptions.replaceChar
        if sReplace then
            term.write( string.sub( string.rep( sReplace, math.max( string.len(sLine) + nScroll, 0 ) ),  nScroll + 1, nScroll + w ) )
        else
            term.write( string.sub( sLine, nScroll + 1, nScroll + w ) )
        end

        if nCompletion then
            local sCompletion = tCompletions[ nCompletion ]
            local oldText, oldBg
            if not _bClear then
                oldText = term.getTextColor()
                oldBg = term.getBackgroundColor()
                if not _tOptions.completeTextColor then
                    term.setTextColor( colors.white )
                else
                    term.setTextColor( _tOptions.completeTextColor )
                end
                if not _tOptions.completeBGColor then
                    term.setBackgroundColor( colors.gray )
                else
                    term.setBackgroundColor( _tOptions.completeBGColor )
                end
            end
            if sReplace then
                term.write( string.rep( sReplace, string.len( sCompletion ) ) )
            else
                term.write( sCompletion )
            end
            if not _bClear then
                term.setTextColor( oldText )
                term.setBackgroundColor( oldBg )
            end
        end

        term.setCursorPos( sx + nPos - nScroll, cy )
    end
    
    local function clear()
        redraw( true )
    end

    recomplete()
    redraw()

    local function acceptCompletion()
        if nCompletion then
            -- Clear
            clear()

            -- Find the common prefix of all the other suggestions which start with the same letter as the current one
            local sCompletion = tCompletions[ nCompletion ]
            sLine = sLine .. sCompletion
            nPos = string.len( sLine )

            -- Redraw
            recomplete()
            redraw()
        end
    end
    while true do
        local sEvent, param = os.pullEvent()

        if sEvent == "char" then
            -- Typed key
            clear()
            sLine = string.sub( sLine, 1, nPos ) .. param .. string.sub( sLine, nPos + 1 )
            nPos = nPos + 1
            recomplete()
            redraw()

        elseif sEvent == "paste" then
            -- Pasted text
            clear()
            sLine = string.sub( sLine, 1, nPos ) .. param .. string.sub( sLine, nPos + 1 )
            nPos = nPos + string.len( param )
            recomplete()
            redraw()

        elseif sEvent == "key" then
            if _tOptions.customkeys.enter == param then
                -- Enter
                if nCompletion then
                    clear()
                    uncomplete()
                    redraw()
                end
                break
                
            elseif _tOptions.customkeys.left == param then
                -- Left
                if nPos > 0 then
                    clear()
                    nPos = nPos - 1
                    recomplete()
                    redraw()
                end
                
            elseif _tOptions.customkeys.right == param then
                -- Right                
                if nPos < string.len(sLine) then
                    -- Move right
                    clear()
                    nPos = nPos + 1
                    recomplete()
                    redraw()
                else
                    -- Accept autocomplete
                    acceptCompletion()
                end

            elseif  _tOptions.customkeys.up == param or _tOptions.customkeys.down == param then
                -- Up or down
                if nCompletion then
                    -- Cycle completions
                    clear()
                    if _tOptions.customkeys.up == param then
                        nCompletion = nCompletion - 1
                        if nCompletion < 1 then
                            nCompletion = #tCompletions
                        end
                    elseif _tOptions.customkeys.down == param then
                        nCompletion = nCompletion + 1
                        if nCompletion > #tCompletions then
                            nCompletion = 1
                        end
                    end
                    redraw()

                elseif _tOptions.history then
                    -- Cycle history
                    clear()
                    if _tOptions.customkeys.up == param then
                        -- Up
                        if nhistoryPos == nil then
                            if #_tOptions.history > 0 then
                                nhistoryPos = #_tOptions.history
                            end
                        elseif nhistoryPos > 1 then
                            nhistoryPos = nhistoryPos - 1
                        end
                    elseif _tOptions.customkeys.down == param then
                        -- Down
                        if nhistoryPos == #_tOptions.history then
                            nhistoryPos = nil
                        elseif nhistoryPos ~= nil then
                            nhistoryPos = nhistoryPos + 1
                        end                        
                    end
                    if nhistoryPos then
                        sLine = _tOptions.history[nhistoryPos]
                        nPos = string.len( sLine ) 
                    else
                        sLine = ""
                        nPos = 0
                    end
                    uncomplete()
                    redraw()

                end

            elseif _tOptions.customkeys.backspace == param then
                -- Backspace
                if nPos > 0 then
                    clear()
                    sLine = string.sub( sLine, 1, nPos - 1 ) .. string.sub( sLine, nPos + 1 )
                    nPos = nPos - 1
                    recomplete()
                    redraw()
                end

            elseif _tOptions.customkeys.home == param then
                -- Home
                if nPos > 0 then
                    clear()
                    nPos = 0
                    recomplete()
                    redraw()
                end

            elseif _tOptions.customkeys.delete == param then
                -- Delete
                if nPos < string.len(sLine) then
                    clear()
                    sLine = string.sub( sLine, 1, nPos ) .. string.sub( sLine, nPos + 2 )
                    recomplete()
                    redraw()
                end

            elseif _tOptions.customkeys["end"] == param then
                -- End
                if nPos < string.len(sLine ) then
                    clear()
                    nPos = string.len(sLine)
                    recomplete()
                    redraw()
                end

            elseif _tOptions.customkeys.tab == param then
                -- Tab (accept autocomplete)
                acceptCompletion()

            end
        
        elseif sEvent == "term_resize" then
            -- Terminal resized
            if not _tOptions.limit then
                w = term.getSize()
            else
                w = _tOptions.limit
            end
            redraw()

        end

        if _tOptions.filter then
            -- Filter out all unwanted characters/strings using a function defined by the user
            local sPreFilterLine = sLine
            sLine = _tOptions.filter( sLine )
            if string.len( sPreFilterLine ) ~= string.len( sLine ) then
                local sPreClearLine = sLine
                sLine = sPreFilterLine
                clear()
                sLine = sPreClearLine
            end
            if not sLine then
                sLine = sPreFilterLine
            else
                if nPos >= ( string.len( sPreFilterLine ) - string.len( sLine ) ) then
                    nPos = nPos - ( string.len( sPreFilterLine ) - string.len( sLine ) )
                else
                    nPos = 0
                end
            end
            redraw()
        end
    end

    local cx, cy = term.getCursorPos()
    term.setCursorBlink( false )
    if _tOptions.newline == nil or _tOptions.newline == true then
        term.setCursorPos( w + 1, cy )
        print()
    end
    
    return sLine
end

function slowWrite( sText, nRate )
    if nRate ~= nil and type( nRate ) ~= "number" then
        error( "bad argument #2 (expected number, got " .. type( nRate ) .. ")", 2 )
    end
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
    if type( nTime ) ~= "number" then
        error( "bad argument #1 (expected number, got " .. type( nTime ) .. ")", 2 )
    end
    if bTwentyFourHour ~= nil and type( bTwentyFourHour ) ~= "boolean" then
        error( "bad argument #2 (expected boolean, got " .. type( bTwentyFourHour ) .. ")", 2 ) 
    end
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
    if _nFreeLines ~= nil and type( _nFreeLines ) ~= "number" then
        error( "bad argument #2 (expected number, got " .. type( _nFreeLines ) .. ")", 2 ) 
    end
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
    local tAll = { ... }
    for k,v in ipairs( tAll ) do
        if type( v ) ~= "number" and type( v ) ~= "table" then
            error( "bad argument #"..k.." (expected number or table, got " .. type( v ) .. ")", 3 ) 
        end
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
    tabulateCommon( false, ... )
end

function pagedTabulate( ... )
    tabulateCommon( true, ... )
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

empty_json_array = {}

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
    if type( s ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( s ) .. ")", 2 )
    end
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
    if type( t ) ~= "table" and type( t ) ~= "string" and type( t ) ~= "number" and type( t ) ~= "boolean" then
        error( "bad argument #1 (expected table/string/number/boolean, got " .. type( t ) .. ")", 2 )
    end
    if bNBTStyle ~= nil and type( bNBTStyle ) ~= "boolean" then
        error( "bad argument #2 (expected boolean, got " .. type( bNBTStyle ) .. ")", 2 )
    end
    local tTracking = {}
    return serializeJSONImpl( t, tTracking, bNBTStyle or false )
end

function urlEncode( str )
    if type( str ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( str ) .. ")", 2 )
    end
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
    if type( sSearchText ) ~= "string" then
        error( "bad argument #1 (expected string, got " .. type( sSearchText ) .. ")", 2 )
    end
    if tSearchTable ~= nil and type( tSearchTable ) ~= "table" then
        error( "bad argument #2 (expected table, got " .. type( tSearchTable ) .. ")", 2 )
    end

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
