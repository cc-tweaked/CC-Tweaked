
local tArgs = { ... }
if #tArgs > 0 then
    print( "This is an interactive Lua prompt." )
    print( "To run a lua program, just type its name." )
    return
end

local bRunning = true
local tCommandHistory = {}
local tEnv = {
    ["exit"] = function()
        bRunning = false
    end,
    ["_echo"] = function( ... )
        return ...
    end,
}
setmetatable( tEnv, { __index = _ENV } )

if term.isColour() then
    term.setTextColour( colours.yellow )
end
print( "Interactive Lua prompt." )
print( "Call exit() to exit." )
term.setTextColour( colours.white )

while bRunning do
    --if term.isColour() then
    --    term.setTextColour( colours.yellow )
    --end
    write( "lua> " )
    --term.setTextColour( colours.white )

    local s = read( nil, tCommandHistory, function( sLine )
        if settings.get( "lua.autocomplete" ) then
            local nStartPos = string.find( sLine, "[a-zA-Z0-9_%.:]+$" )
            if nStartPos then
                sLine = string.sub( sLine, nStartPos )
            end
            if #sLine > 0 then
                return textutils.complete( sLine, tEnv )
            end
        end
        return nil
    end )
    if s:match("%S") and tCommandHistory[#tCommandHistory] ~= s then
        table.insert( tCommandHistory, s )
    end

    local nForcePrint = 0
    local func, e = load( s, "lua", "t", tEnv )
    local func2, e2 = load( "return _echo("..s..");", "lua", "t", tEnv )
    if not func then
        if func2 then
            func = func2
            e = nil
            nForcePrint = 1
        end
    else
        if func2 then
            func = func2
        end
    end

    if func then
        local tResults = table.pack( pcall( func ) )
        if tResults[1] then
            local n = 1
            while n < tResults.n or (n <= nForcePrint) do
                local value = tResults[ n + 1 ]
                if type( value ) == "table" then
                    local metatable = getmetatable( value )
                    if type(metatable) == "table" and type(metatable.__tostring) == "function" then
                        print( tostring( value ) )
                    else
                        local ok, serialised = pcall( textutils.serialise, value )
                        if ok then
                            print( serialised )
                        else
                            print( tostring( value ) )
                        end
                    end
                else
                    print( tostring( value ) )
                end
                n = n + 1
            end
        else
            printError( tResults[2] )
        end
    else
        printError( e )
    end

end
