
local tArgs = { ... }
if #tArgs > 2 then
    print( "Usage: alias <alias> <program>" )
    return
end

local sAlias = tArgs[1]
local sProgram = tArgs[2]

if sAlias and sProgram then
    -- Set alias
    shell.setAlias( sAlias, sProgram )
elseif sAlias then
    -- Clear alias
    shell.clearAlias( sAlias )
else
    -- List aliases
    local tAliases = shell.aliases()
    local tList = {}
    for sAlias, sCommand in pairs( tAliases ) do
        table.insert( tList, sAlias..":"..sCommand )
    end
    table.sort( tList )
    textutils.pagedTabulate( tList )
end
