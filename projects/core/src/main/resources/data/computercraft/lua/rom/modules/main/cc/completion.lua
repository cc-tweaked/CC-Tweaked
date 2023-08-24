-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--- A collection of helper methods for working with input completion, such
-- as that require by [`_G.read`].
--
-- @module cc.completion
-- @see cc.shell.completion For additional helpers to use with
-- [`shell.setCompletionFunction`].
-- @since 1.85.0

local expect = require "cc.expect".expect

local function choice_impl(text, choices, add_space)
    local results = {}
    for n = 1, #choices do
        local option = choices[n]
        if #option + (add_space and 1 or 0) > #text and option:sub(1, #text) == text then
            local result = option:sub(#text + 1)
            if add_space then
                table.insert(results, result .. " ")
            else
                table.insert(results, result)
            end
        end
    end
    return results
end

--- Complete from a choice of one or more strings.
--
-- @tparam string text The input string to complete.
-- @tparam { string... } choices The list of choices to complete from.
-- @tparam[opt] boolean add_space Whether to add a space after the completed item.
-- @treturn { string... } A list of suffixes of matching strings.
-- @usage Call [`_G.read`], completing the names of various animals.
--
--     local completion = require "cc.completion"
--     local animals = { "dog", "cat", "lion", "unicorn" }
--     read(nil, nil, function(text) return completion.choice(text, animals) end)
local function choice(text, choices, add_space)
    expect(1, text, "string")
    expect(2, choices, "table")
    expect(3, add_space, "boolean", "nil")
    return choice_impl(text, choices, add_space)
end

--- Complete the name of a currently attached peripheral.
--
-- @tparam string text The input string to complete.
-- @tparam[opt] boolean add_space Whether to add a space after the completed name.
-- @treturn { string... } A list of suffixes of matching peripherals.
-- @usage
--     local completion = require "cc.completion"
--     read(nil, nil, completion.peripheral)
local function peripheral_(text, add_space)
    expect(1, text, "string")
    expect(2, add_space, "boolean", "nil")
    return choice_impl(text, peripheral.getNames(), add_space)
end

local sides = redstone.getSides()

--- Complete the side of a computer.
--
-- @tparam string text The input string to complete.
-- @tparam[opt] boolean add_space Whether to add a space after the completed side.
-- @treturn { string... } A list of suffixes of matching sides.
-- @usage
--     local completion = require "cc.completion"
--     read(nil, nil, completion.side)
local function side(text, add_space)
    expect(1, text, "string")
    expect(2, add_space, "boolean", "nil")
    return choice_impl(text, sides, add_space)
end

--- Complete a [setting][`settings`].
--
-- @tparam string text The input string to complete.
-- @tparam[opt] boolean add_space Whether to add a space after the completed settings.
-- @treturn { string... } A list of suffixes of matching settings.
-- @usage
--     local completion = require "cc.completion"
--     read(nil, nil, completion.setting)
local function setting(text, add_space)
    expect(1, text, "string")
    expect(2, add_space, "boolean", "nil")
    return choice_impl(text, settings.getNames(), add_space)
end

local command_list

--- Complete the name of a Minecraft [command][`commands`].
--
-- @tparam string text The input string to complete.
-- @tparam[opt] boolean add_space Whether to add a space after the completed command.
-- @treturn { string... } A list of suffixes of matching commands.
-- @usage
--     local completion = require "cc.completion"
--     read(nil, nil, completion.command)
local function command(text, add_space)
    expect(1, text, "string")
    expect(2, add_space, "boolean", "nil")
    if command_list == nil then
        command_list = commands and commands.list() or {}
    end

    return choice_impl(text, command_list, add_space)
end

return {
    choice = choice,
    peripheral = peripheral_,
    side = side,
    setting = setting,
    command = command,
}
