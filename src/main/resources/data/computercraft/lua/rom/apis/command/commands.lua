
if not commands then
    error( "Cannot load command API on normal computer", 2 )
end

local native = commands.native or commands

local function collapseArgs( bJSONIsNBT, ... )
    local args = table.pack(...)
    for i = 1, #args do
        local arg = args[i]
        if type(arg) == "boolean" or type(arg) == "number" or type(arg) == "string" then
            args[i] = tostring(arg)
        elseif type(arg) == "table" then
            args[i] = textutils.serialiseJSON( arg, bJSONIsNBT )
        else
            error( "Expected string, number, boolean or table", 3 )
        end
    end

    return table.concat(args, " ")
end

-- Put native functions into the environment
local env = _ENV
env.native = native
for k,v in pairs( native ) do
    env[k] = v
end

-- Create wrapper functions for all the commands
local tAsync = {}
local tNonNBTJSONCommands = {
    [ "tellraw" ] = true,
    [ "title" ] = true
}

local command_mt = {}
function command_mt.__call(self, ...)
    local meta = self[command_mt]
    local sCommand = collapseArgs( meta.json, table.concat(meta.name, " "), ... )
    return meta.func( sCommand )
end

function command_mt.__tostring(self)
    local meta = self[command_mt]
    return ("command %q"):format("/" .. table.concat(meta.name, " "))
end

function command_mt.__index(self, key)
    local meta = self[command_mt]
    if meta.children then return nil end
    meta.children = true

    local name = meta.name
    for _, child in ipairs(native.list(table.unpack(name))) do
        local child_name = { table.unpack(name) }
        child_name[#child_name + 1] = child

        self[child] = setmetatable({ [command_mt] = {
            name = child_name,
            func = meta.func,
            json = meta.json
        } }, command_mt)
    end

    return self[key]
end

for _, sCommandName in ipairs(native.list()) do
    if env[ sCommandName ] == nil then
        local bJSONIsNBT = tNonNBTJSONCommands[ sCommandName ] == nil
        env[ sCommandName ] = setmetatable({ [command_mt] = {
            name = { sCommandName },
            func = native.exec,
            json = bJSONIsNBT
        } }, command_mt)

        tAsync[ sCommandName ] = setmetatable({ [command_mt] = {
            name = { sCommandName },
            func = native.execAsync,
            json = bJSONIsNBT
        } }, command_mt)
    end
end
env.async = tAsync
