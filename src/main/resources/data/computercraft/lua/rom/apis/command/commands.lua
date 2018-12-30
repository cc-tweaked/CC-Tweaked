
if not commands then
	error( "Cannot load command API on normal computer", 2 )
end
native = commands.native or commands

local function collapseArgs( errorDepth, bJSONIsNBT, arg1, ... )
    if arg1 ~= nil then
        if type(arg1) == "boolean" or type(arg1) == "number" or type(arg1) == "string" then
            return tostring(arg1) .. " " .. collapseArgs( errorDepth + 1, bJSONIsNBT, ... )
        elseif type(arg1) == "table" then
            return textutils.serialiseJSON( arg1, bJSONIsNBT ) .. " " .. collapseArgs( errorDepth + 1, bJSONIsNBT, ... )
        else
            error( "Expected string, number, boolean or table", errorDepth )
        end
    end
    return ""
end

-- Put native functions into the environment
local env = _ENV
for k,v in pairs( native ) do
    env[k] = v
end

-- Create wrapper functions for all the commands
local tAsync = {}
local tNonNBTJSONCommands = {
    [ "tellraw" ] = true,
    [ "title" ] = true
}
local tCommands = native.list()
for n,sCommandName in ipairs(tCommands) do
    if env[ sCommandName ] == nil then
        local bJSONIsNBT = (tNonNBTJSONCommands[ sCommandName ] == nil)
        env[ sCommandName ] = function( ... )
            local sCommand = sCommandName .. " " .. collapseArgs( 3, bJSONIsNBT, ... )
            return native.exec( sCommand )
        end
        tAsync[ sCommandName ] = function( ... )
            local sCommand = sCommandName .. " " .. collapseArgs( 3, bJSONIsNBT, ... )
            return native.execAsync( sCommand )
        end
    end
end
env.async = tAsync
