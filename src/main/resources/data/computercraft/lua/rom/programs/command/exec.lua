
local tArgs = { ... }
if not commands then
    printError( "Requires a Command Computer." )
    return
end
if #tArgs == 0 then
    printError( "Usage: exec <command>" )
    return
end

local function printSuccess( text )
    if term.isColor() then
        term.setTextColor( colors.green )
    end
    print( text )
    term.setTextColor( colors.white )
end

local sCommand = string.lower( tArgs[1] )
for n=2,#tArgs do
    sCommand = sCommand .. " " .. tArgs[n]
end

local bResult, tOutput = commands.exec( sCommand )
if bResult then
    printSuccess( "Success" )
    if #tOutput > 0 then
        for n=1,#tOutput do
            print( tOutput[n] )
        end
    end
else
    printError( "Failed" )
    if #tOutput > 0 then
        for n=1,#tOutput do
            print( tOutput[n] )
        end
    end
end
