-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- Execute [Minecraft commands][mc] and gather data from the results from
a command computer.

> [!NOTE]
> This API is only available on Command computers. It is not accessible to normal
> players.

While one may use [`commands.exec`] directly to execute a command, the
commands API also provides helper methods to execute every command. For
instance, `commands.say("Hi!")` is equivalent to `commands.exec("say Hi!")`.

[`commands.async`] provides a similar interface to execute asynchronous
commands. `commands.async.say("Hi!")` is equivalent to
`commands.execAsync("say Hi!")`.

[mc]: https://minecraft.wiki/w/Commands

@module commands
@usage Set the block above this computer to stone:

    commands.setblock("~", "~1", "~", "minecraft:stone")
]]
if not commands then
    error("Cannot load command API on normal computer", 2)
end

--- The builtin commands API, without any generated command helper functions
--
-- This may be useful if a built-in function (such as [`commands.list`]) has been
-- overwritten by a command.
local native = commands.native or commands

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
env.native = native
for k, v in pairs(native) do
    env[k] = v
end

-- Create wrapper functions for all the commands
local tAsync = {}
local tNonNBTJSONCommands = {
    ["tellraw"] = true,
    ["title"] = true,
}

local command_mt = {}
function command_mt.__call(self, ...)
    local meta = self[command_mt]
    local sCommand = collapseArgs(meta.json, table.concat(meta.name, " "), ...)
    return meta.func(sCommand)
end

function command_mt.__tostring(self)
    local meta = self[command_mt]
    return ("command %q"):format("/" .. table.concat(meta.name, " "))
end

local function mk_command(name, json, func)
    return setmetatable({
        [command_mt] = {
            name = name,
            func = func,
            json = json,
        },
    }, command_mt)
end

function command_mt.__index(self, key)
    local meta = self[command_mt]
    if meta.children then return nil end
    meta.children = true

    local name = meta.name
    for _, child in ipairs(native.list(table.unpack(name))) do
        local child_name = { table.unpack(name) }
        child_name[#child_name + 1] = child
        self[child] = mk_command(child_name, meta.json, meta.func)
    end

    return self[key]
end

for _, sCommandName in ipairs(native.list()) do
    if env[sCommandName] == nil then
        local bJSONIsNBT = tNonNBTJSONCommands[sCommandName] == nil
        env[sCommandName] = mk_command({ sCommandName }, bJSONIsNBT, native.exec)
        tAsync[sCommandName] = mk_command({ sCommandName }, bJSONIsNBT, native.execAsync)
    end
end

--- A table containing asynchronous wrappers for all commands.
--
-- As with [`commands.execAsync`], this returns the "task id" of the enqueued
-- command.
-- @see execAsync
-- @usage Asynchronously sets the block above the computer to stone.
--
--     commands.async.setblock("~", "~1", "~", "minecraft:stone")
env.async = tAsync
