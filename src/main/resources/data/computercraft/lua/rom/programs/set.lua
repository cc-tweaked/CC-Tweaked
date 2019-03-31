
local tArgs = { ... }
if #tArgs == 0 then
    -- "set"
    local x,y = term.getCursorPos()
    local tSettings = {}
    for n,sName in ipairs( settings.getNames() ) do
        tSettings[n] = textutils.serialize(sName) .. " is " .. textutils.serialize(settings.get(sName))
    end
    textutils.pagedPrint(table.concat(tSettings,"\n"),y-3)

elseif #tArgs == 1 then
    -- "set foo"
    local sName = tArgs[1]
    print( textutils.serialize(sName) .. " is " .. textutils.serialize(settings.get(sName)) )

else
    -- "set foo bar"
    local sName = tArgs[1]
    local sValue = tArgs[2]
    local value
    if sValue == "true" then
        value = true
    elseif sValue == "false" then
        value = false
    elseif sValue == "nil" then
        value = nil
    elseif tonumber(sValue) then
        value = tonumber(sValue)
    else
        value = sValue
    end

    local oldValue = settings.get( sValue )
    if value ~= nil then
        settings.set( sName, value )
        print( textutils.serialize(sName) .. " set to " .. textutils.serialize(value) )
    else
        settings.unset( sName )
        print( textutils.serialize(sName) .. " unset" )
    end
    if value ~= oldValue then
        settings.save( ".settings" )
    end
end
