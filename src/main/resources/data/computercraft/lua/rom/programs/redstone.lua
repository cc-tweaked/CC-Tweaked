
local tArgs = { ... }

local function printUsage()
    print( "Usages:" )
    print( "redstone probe" )
    print( "redstone set <side> <value>" )
    print( "redstone set <side> <color> <value>" )
    print( "redstone pulse <side> <count> <period>" )
end

local sCommand = tArgs[1]
if sCommand == "probe" then
    -- "redstone probe"
    -- Regular input
    print( "Redstone inputs: " )

    local count = 0
    local bundledCount = 0
    for n,sSide in ipairs( redstone.getSides() ) do
        if redstone.getBundledInput( sSide ) > 0 then
            bundledCount = bundledCount + 1
        end
        if redstone.getInput( sSide ) then
            if count > 0 then
                io.write( ", " )
            end
            io.write( sSide )
            count = count + 1
        end
    end
    if count > 0 then
        print( "." )
    else
        print( "None." )
    end

    -- Bundled input
    if bundledCount > 0 then
        print()
        print( "Bundled inputs:" )
        for i,sSide in ipairs( redstone.getSides() ) do
            local nInput = redstone.getBundledInput( sSide )
            if nInput ~= 0 then
                write( sSide..": " )
                local count = 0
                for sColour,nColour in pairs( colors ) do
                    if type( nColour ) == "number" and colors.test( nInput, nColour ) then
                        if count > 0 then
                            write( ", " )
                        end
                        if term.isColour() then
                            term.setTextColour( nColour )
                        end
                        write( sColour )
                        if term.isColour() then
                            term.setTextColour( colours.white )
                        end
                        count = count + 1
                    end
                end
                print( "." )
            end
        end
    end

elseif sCommand == "pulse" then
    -- "redstone pulse"
    local sSide = tArgs[2]
    local nCount = tonumber( tArgs[3] ) or 1
    local nPeriod = tonumber( tArgs[4] ) or 0.5
    for n=1,nCount do
        redstone.setOutput( sSide, true )
        sleep( nPeriod / 2 )
        redstone.setOutput( sSide, false )
        sleep( nPeriod / 2 )
    end

elseif sCommand == "set" then
    -- "redstone set"
    local sSide = tArgs[2]
    if #tArgs > 3 then
        -- Bundled cable output
        local sColour = tArgs[3]
        local nColour = colors[sColour] or colours[sColour]
        if type(nColour) ~= "number" then
            printError( "No such color" )
            return
        end

        local sValue = tArgs[4]
        if sValue == "true" then
            rs.setBundledOutput( sSide, colors.combine( rs.getBundledOutput( sSide ), nColour ) )
        elseif sValue == "false" then
            rs.setBundledOutput( sSide, colors.subtract( rs.getBundledOutput( sSide ), nColour ) )
        else
            print( "Value must be boolean" )
        end
    else
        -- Regular output
        local sValue = tArgs[3]
        local nValue = tonumber(sValue)
        if sValue == "true" then
            rs.setOutput( sSide, true )
        elseif sValue == "false" then
            rs.setOutput( sSide, false )
        elseif nValue and nValue >= 0 and nValue <= 15 then
            rs.setAnalogOutput( sSide, nValue )
        else
            print( "Value must be boolean or 0-15" )
        end
    end

else
    -- Something else
    printUsage()

end
