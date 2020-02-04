--- The `textutils` API provides helpful utilities for formatting and
-- manipulating strings.
--
-- @module textutils

local expect = dofile("rom/modules/main/cc/expect.lua").expect

--- Slowly writes string text at current cursor position,
-- character-by-character.
--
-- Like @{write}, this does not insert a newline at the end.
--
-- @tparam string sText The the text to write to the screen
-- @tparam[opt] number nRate The number of characters to write each second,
-- Defaults to 20.
-- @usage textutils.slowWrite("Hello, world!")
-- @usage textutils.slowWrite("Hello, world!", 5)
function slowWrite( sText, nRate )
    expect(2, nRate, "number", "nil")
    nRate = nRate or 20
    if nRate < 0 then
        error( "Rate must be positive", 2 )
    end
    local nSleep = 1 / nRate

    sText = tostring( sText )
    local x, y = term.getCursorPos()
    local len = #sText

    for n = 1, len do
        term.setCursorPos( x, y )
        sleep( nSleep )
        local nLines = write( string.sub( sText, 1, n ) )
        local _, newY = term.getCursorPos()
        y = newY - nLines
    end
end

--- Slowly prints string text at current cursor position,
-- character-by-character.
--
-- Like @{print}, this inserts a newline after printing.
--
-- @tparam string sText The the text to write to the screen
-- @tparam[opt] number nRate The number of characters to write each second,
-- Defaults to 20.
-- @usage textutils.slowPrint("Hello, world!")
-- @usage textutils.slowPrint("Hello, world!", 5)
function slowPrint( sText, nRate )
    slowWrite( sText, nRate )
    print()
end

--- Takes input time and formats it in a more readable format such as `6:30 PM`.
--
-- @tparam number nTime The time to format, as provided by @{os.time}.
-- @tparam[opt] boolean bTwentyFourHour Whether to format this as a 24-hour
-- clock (`18:30`) rather than a 12-hour one (`6:30 AM`)
-- @treturn string The formatted time
-- @usage textutils.formatTime(os.time())
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
    local nMinute = math.floor((nTime - nHour) * 60)
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
        for _ = 1, _n do
            nativeScroll( 1 )

            if nFreeLines <= 0 then
                local _, h = _term.getSize()
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

--- Prints a given string to the display.
--
-- If the action can be completed without scrolling, it acts much the same as
-- @{print}; otherwise, it will throw up a "Press any key to continue" prompt at
-- the bottom of the display. Each press will cause it to scroll down and write
-- a single line more before prompting again, if need be.
--
-- @tparam string _sText The text to print to the screen.
-- @tparam[opt] number _nFreeLines The number of lines which will be
-- automatically scrolled before the first prompt appears (meaning _nFreeLines +
-- 1 lines will be printed). This can be set to the terminal's height - 2 to
-- always try to fill the screen. Defaults to 0, meaning only one line is
-- displayed before prompting.
-- @treturn number The number of lines printed.
-- @usage
-- local width, height = term.getSize()
-- textutils.pagedPrint(("This is a rather verbose dose of repetition.\n"):rep(30), height - 2)
function pagedPrint( _sText, _nFreeLines )
    expect(2, _nFreeLines, "number", "nil")
    -- Setup a redirector
    local oldTerm = term.current()
    local newTerm = {}
    for k, v in pairs( oldTerm ) do
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

    local w, h = term.getSize()
    local nMaxLen = w / 8
    for n, t in ipairs( tAll ) do
        if type(t) == "table" then
            for nu, sItem in pairs(t) do
                if type( sItem ) ~= "string" then
                    error( "bad argument #" .. n .. "." .. nu .. " (expected string, got " .. type( sItem ) .. ")", 3 )
                end
                nMaxLen = math.max( #sItem + 1, nMaxLen )
            end
        end
    end
    local nCols = math.floor( w / nMaxLen )
    local nLines = 0
    local function newLine()
        if bPaged and nLines >= h - 3 then
            pagedPrint()
        else
            print()
        end
        nLines = nLines + 1
    end

    local function drawCols( _t )
        local nCol = 1
        for _, s in ipairs( _t ) do
            if nCol > nCols then
                nCol = 1
                newLine()
            end

            local cx, cy = term.getCursorPos()
            cx = 1 + (nCol - 1) * nMaxLen
            term.setCursorPos( cx, cy )
            term.write( s )

            nCol = nCol + 1
        end
        print()
    end
    for _, t in ipairs( tAll ) do
        if type(t) == "table" then
            if #t > 0 then
                drawCols( t )
            end
        elseif type(t) == "number" then
            term.setTextColor( t )
        end
    end
end

--- Prints tables in a structured form.
--
-- This accepts multiple arguments, either a table or a number. When
-- encountering a table, this will be treated as a table row, with each column
-- width being auto-adjusted.
--
-- When encountering a number, this sets the text color of the subsequent rows to it.
--
-- @tparam {string...}|number ... The rows and text colors to display.
-- @usage textutils.tabulate(colors.orange, { "1", "2", "3" }, colors.lightBlue, { "A", "B", "C" })
function tabulate( ... )
    return tabulateCommon( false, ... )
end

--- Prints tables in a structured form, stopping and prompting for input should
-- the result not fit on the terminal.
--
-- This functions identically to @{textutils.tabulate}, but will prompt for user
-- input should the whole output not fit on the display.
--
-- @tparam {string...}|number ... The rows and text colors to display.
-- @usage textutils.tabulate(colors.orange, { "1", "2", "3" }, colors.lightBlue, { "A", "B", "C" })
-- @see textutils.tabulate
-- @see textutils.pagedPrint
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
            for k, v in ipairs(t) do
                tSeen[k] = true
                sResult = sResult .. sSubIndent .. serializeImpl( v, tTracking, sSubIndent ) .. ",\n"
            end
            for k, v in pairs(t) do
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
        error( "Cannot serialize type " .. sType, 0 )

    end
end

--- A table representing an empty JSON array, in order to distinguish it from an
-- empty JSON object.
--
-- The contents of this table should not be modified.
--
-- @usage textutils.serialiseJSON(textutils.empty_json_array)
-- @see textutils.serialiseJSON
empty_json_array = setmetatable({}, {
    __newindex = function()
        error("attempt to mutate textutils.empty_json_array", 2)
    end,
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
            for k, v in pairs(t) do
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
            for _, v in ipairs(t) do
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
        error( "Cannot serialize type " .. sType, 0 )

    end
end

--- Convert a Lua object into a textual representation, suitable for
-- saving in a file or pretty-printing.
--
-- @param t The object to serialise
-- @treturn string The serialised representation
-- @throws If the object contains a value which cannot be
-- serialised. This includes functions and tables which appear multiple
-- times.
function serialize( t )
    local tTracking = {}
    return serializeImpl( t, tTracking, "" )
end

serialise = serialize -- GB version

--- Converts a serialised string back into a reassembled Lua object.
--
-- This is mainly used together with @{textutils.serialize}.
--
-- @tparam string s The serialised string to deserialise.
-- @return[1] The deserialised object
-- @treturn[2] nil If the object could not be deserialised.
function unserialize( s )
    expect(1, s, "string")
    local func = load( "return " .. s, "unserialize", "t", {} )
    if func then
        local ok, result = pcall( func )
        if ok then
            return result
        end
    end
    return nil
end

unserialise = unserialize -- GB version

--- Returns a JSON representation of the given data.
--
-- This function attempts to guess whether a table is a JSON array or
-- object. However, empty tables are assumed to be empty objects - use
-- @{textutils.empty_json_array} to mark an empty array.
--
-- This is largely intended for interacting with various functions from the
-- @{commands} API, though may also be used in making @{http} requests.
--
-- @param t The value to serialise. Like @{textutils.serialise}, this should not
-- contain recursive tables or functions.
-- @tparam[opt] boolean bNBTStyle Whether to produce NBT-style JSON (non-quoted keys)
-- instead of standard JSON.
-- @treturn string The JSON representation of the input.
-- @throws If the object contains a value which cannot be
-- serialised. This includes functions and tables which appear multiple
-- times.
-- @usage textutils.serializeJSON({ values = { 1, "2", true } })
function serializeJSON( t, bNBTStyle )
    expect(1, t, "table", "string", "number", "boolean")
    expect(2, bNBTStyle, "boolean", "nil")
    local tTracking = {}
    return serializeJSONImpl( t, tTracking, bNBTStyle or false )
end

serialiseJSON = serializeJSON -- GB version

--- Replaces certain characters in a string to make it safe for use in URLs or POST data.
--
-- @tparam string str The string to encode
-- @treturn string The encoded string.
-- @usage print("https://example.com/?view=" .. textutils.urlEncode(read()))
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
                    string.format("%%%02X", 192 + bit32.band( bit32.arshift(n, 6), 31 ) ) ..
                    string.format("%%%02X", 128 + bit32.band( n, 63 ) )
            end
        end )
        str = string.gsub(str, " ", "+")
    end
    return str
end

local tEmpty = {}

--- Provides a list of possible completions for a partial Lua expression.
--
-- If the completed element is a table, suggestions will have `.` appended to
-- them. Similarly, functions have `(` appended to them.
--
-- @tparam string sSearchText The partial expression to complete, such as a
-- variable name or table index.
--
-- @tparam[opt] table tSearchTable The table to find variables in, defaulting to
-- the global environment (@{_G}). The function also searches the "parent"
-- environment via the `__index` metatable field.
--
-- @treturn { string... } The (possibly empty) list of completions.
-- @see shell.setCompletionFunction
-- @see read
-- @usage textutils.complete( "pa", getfenv() )
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
    local nPartLength = #sPart

    local tResults = {}
    local tSeen = {}
    while tTable do
        for k, v in pairs( tTable ) do
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
