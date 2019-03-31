
local tArgs = { ... }
local nLimit = 1
if #tArgs > 1 then
    print( "Usage: refuel [number]" )
    return
elseif #tArgs > 0 then
    if tArgs[1] == "all" then
        nLimit = 64 * 16
    else
        nLimit = tonumber( tArgs[1] )
    end
end

if turtle.getFuelLevel() ~= "unlimited" then
    for n=1,16 do
        local nCount = turtle.getItemCount(n)
        if nLimit > 0 and nCount > 0 and turtle.getFuelLevel() < turtle.getFuelLimit() then
            local nBurn = math.min( nLimit, nCount )
            turtle.select( n )
            if turtle.refuel( nBurn ) then
                local nNewCount = turtle.getItemCount(n)
                nLimit = nLimit - (nCount - nNewCount)
            end
        end
    end
    print( "Fuel level is "..turtle.getFuelLevel() )
    if turtle.getFuelLevel() == turtle.getFuelLimit() then
        print( "Fuel limit reached" )
    end
else
    print( "Fuel level is unlimited" )
end
