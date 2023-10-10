-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- A collection of helper methods for working with shell completion.

Most programs may be completed using the [`build`] helper method, rather than
manually switching on the argument index.

Note, the helper functions within this module do not accept an argument index,
and so are not directly usable with the [`shell.setCompletionFunction`]. Instead,
wrap them using [`build`], or your own custom function.

@module cc.shell.completion
@since 1.85.0
@see cc.completion For more general helpers, suitable for use with [`_G.read`].
@see shell.setCompletionFunction

@usage Register a completion handler for example.lua which prompts for a
choice of options, followed by a directory, and then multiple files.

    local completion = require "cc.shell.completion"
    local complete = completion.build(
      { completion.choice, { "get", "put" } },
      completion.dir,
      { completion.file, many = true }
    )
    shell.setCompletionFunction("example.lua", complete)
    read(nil, nil, shell.complete, "example ")
]]

local expect = require "cc.expect".expect
local completion = require "cc.completion"

--- Complete the name of a file relative to the current working directory.
--
-- @tparam table shell The shell we're completing in.
-- @tparam string text Current text to complete.
-- @treturn { string... } A list of suffixes of matching files.
local function file(shell, text)
    return fs.complete(text, shell.dir(), {
        include_files = true,
        include_dirs = false,
        include_hidden = settings.get("shell.autocomplete_hidden"),
    })
end

--- Complete the name of a directory relative to the current working directory.
--
-- @tparam table shell The shell we're completing in.
-- @tparam string text Current text to complete.
-- @treturn { string... } A list of suffixes of matching directories.
local function dir(shell, text)
    return fs.complete(text, shell.dir(), {
        include_files = false,
        include_dirs = true,
        include_hidden = settings.get("shell.autocomplete_hidden"),
    })
end

--- Complete the name of a file or directory relative to the current working
-- directory.
--
-- @tparam table shell The shell we're completing in.
-- @tparam string text Current text to complete.
-- @tparam { string... } previous The shell arguments before this one.
-- @tparam[opt] boolean add_space Whether to add a space after the completed item.
-- @treturn { string... } A list of suffixes of matching files and directories.
local function dirOrFile(shell, text, previous, add_space)
    local results = fs.complete(text, shell.dir(), {
        include_files = true,
        include_dirs = true,
        include_hidden = settings.get("shell.autocomplete_hidden"),
    })
    if add_space then
        for n = 1, #results do
            local result = results[n]
            if result:sub(-1) ~= "/" then
                results[n] = result .. " "
            end
        end
    end
    return results
end

local function wrap(func)
    return function(shell, text, previous, ...)
        return func(text, ...)
    end
end

--- Complete the name of a program.
--
-- @tparam table shell The shell we're completing in.
-- @tparam string text Current text to complete.
-- @treturn { string... } A list of suffixes of matching programs.
-- @see shell.completeProgram
local function program(shell, text)
    return shell.completeProgram(text)
end

--- Complete arguments of a program.
--
-- @tparam table shell The shell we're completing in.
-- @tparam string text Current text to complete.
-- @tparam { string... } previous The shell arguments before this one.
-- @tparam number starting Which argument index this program and args start at.
-- @treturn { string... } A list of suffixes of matching programs or arguments.
-- @since 1.97.0
local function programWithArgs(shell, text, previous, starting)
    if #previous + 1 == starting then
        local tCompletionInfo = shell.getCompletionInfo()
        if text:sub(-1) ~= "/" and tCompletionInfo[shell.resolveProgram(text)] then
            return { " " }
        else
            local results = shell.completeProgram(text)
            for n = 1, #results do
                local sResult = results[n]
                if sResult:sub(-1) ~= "/" and tCompletionInfo[shell.resolveProgram(text .. sResult)] then
                    results[n] = sResult .. " "
                end
            end
            return results
        end
    else
        local program = previous[starting]
        local resolved = shell.resolveProgram(program)
        if not resolved then return end
        local tCompletion = shell.getCompletionInfo()[resolved]
        if not tCompletion then return end
        return tCompletion.fnComplete(shell, #previous - starting + 1, text, { program, table.unpack(previous, starting + 1, #previous) })
    end
end

--[[- A helper function for building shell completion arguments.

This accepts a series of single-argument completion functions, and combines
them into a function suitable for use with [`shell.setCompletionFunction`].

@tparam nil|table|function ... Every argument to [`build`] represents an argument
to the program you wish to complete. Each argument can be one of three types:

 - `nil`: This argument will not be completed.

 - A function: This argument will be completed with the given function. It is
   called with the [`shell`] object, the string to complete and the arguments
   before this one.

 - A table: This acts as a more powerful version of the function case. The table
   must have a function as the first item - this will be called with the shell,
   string and preceding arguments as above, but also followed by any additional
   items in the table. This provides a more convenient interface to pass
   options to your completion functions.

   If this table is the last argument, it may also set the `many` key to true,
   which states this function should be used to complete any remaining arguments.
]]
local function build(...)
    local arguments = table.pack(...)
    for i = 1, arguments.n do
        local arg = arguments[i]
        if arg ~= nil then
            expect(i, arg, "table", "function")
            if type(arg) == "function" then
                arg = { arg }
                arguments[i] = arg
            end

            if type(arg[1]) ~= "function" then
                error(("Bad table entry #1 at argument #%d (function expected, got %s)"):format(i, type(arg[1])), 2)
            end

            if arg.many and i < arguments.n then
                error(("Unexpected 'many' field on argument #%d (should only occur on the last argument)"):format(i), 2)
            end
        end
    end

    return function(shell, index, text, previous)
        local arg = arguments[index]
        if not arg then
            if index <= arguments.n then return end

            arg = arguments[arguments.n]
            if not arg or not arg.many then return end
        end

        return arg[1](shell, text, previous, table.unpack(arg, 2))
    end
end

return {
    file = file,
    dir = dir,
    dirOrFile = dirOrFile,
    program = program,
    programWithArgs = programWithArgs,

    -- Re-export various other functions
    help = wrap(help.completeTopic), --- Wraps [`help.completeTopic`] as a [`build`] compatible function.
    choice = wrap(completion.choice), --- Wraps [`cc.completion.choice`] as a [`build`] compatible function.
    peripheral = wrap(completion.peripheral), --- Wraps [`cc.completion.peripheral`] as a [`build`] compatible function.
    side = wrap(completion.side), --- Wraps [`cc.completion.side`] as a [`build`] compatible function.
    setting = wrap(completion.setting), --- Wraps [`cc.completion.setting`] as a [`build`] compatible function.
    command = wrap(completion.command), --- Wraps [`cc.completion.command`] as a [`build`] compatible function.

    build = build,
}
