--- The commands API allows your system to directly execute [Minecraft
-- commands][mc] and gather data from the results.
--
-- While one may use @{commands.exec} directly to execute a command, the
-- commands API also provides helper methods to execute every command. For
-- instance, `commands.say("Hi!")` is equivalent to `commands.exec("say Hi!")`.
--
-- @{commands.async} provides a similar interface to execute asynchronous
-- commands. `commands.async.say("Hi!")` is equivalent to
-- `commands.execAsync("Hi!")`.
--
-- [mc]: https://minecraft.gamepedia.com/Commands
--
-- @module commands

if not commands then
    error("Cannot load command API on normal computer", 2)
end

--- The builtin commands API, without any generated command helper functions
--
-- This may be useful if a built-in function (such as @{commands.list}) has been
-- overwritten by a command.
native = commands.native or commands

local function collapseArgs(bJSONIsNBT, ...)
    local args = table.pack(...)
    for i = 1, #args do
        local arg = args[i]
        if type(arg) == "boolean" or type(arg) == "number" or type(arg) == "string" then
            args[i] = tostring(arg)
        elseif type(arg) == "table" then
            args[i] = textutils.serialiseJSON(arg, bJSONIsNBT)
        else
            error("Expected string, number, boolean or table", 3)
        end
    end

    return table.concat(args, " ")
end

-- Put native functions into the environment
local env = _ENV
for k, v in pairs(native) do
    env[k] = v
end

-- Create wrapper functions for all the commands
local tAsync = {}
local tNonNBTJSONCommands = {
    ["tellraw"] = true,
    ["title"] = true,
}
local tCommands = native.list()
for _, sCommandName in ipairs(tCommands) do
    if env[sCommandName] == nil then
        local bJSONIsNBT = tNonNBTJSONCommands[sCommandName] == nil
        env[sCommandName] = function(...)
            local sCommand = collapseArgs(bJSONIsNBT, sCommandName, ...)
            return native.exec(sCommand)
        end
        tAsync[sCommandName] = function(...)
            local sCommand = collapseArgs(bJSONIsNBT, sCommandName, ...)
            return native.execAsync(sCommand)
        end
    end
end
env.async = tAsync
