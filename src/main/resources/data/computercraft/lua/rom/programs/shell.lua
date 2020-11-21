--- The shell API provides access to CraftOS's command line interface.
--
-- It allows you to @{run|start programs}, @{setCompletionFunction|add
-- completion for a program}, and much more.
--
-- @{shell} is not a "true" API. Instead, it is a standard program, which its
-- API into the programs that it launches. This allows for multiple shells to
-- run at the same time, but means that the API is not available in the global
-- environment, and so is unavailable to other @{os.loadAPI|APIs}.
--
-- @module[module] shell

local expect = dofile("rom/modules/main/cc/expect.lua").expect
local make_package = dofile("rom/modules/main/cc/require.lua").make

local multishell = multishell
local parentShell = shell
local parentTerm = term.current()

if multishell then
    multishell.setTitle(multishell.getCurrent(), "shell")
end

local bExit = false
local sDir = parentShell and parentShell.dir() or ""
local sPath = parentShell and parentShell.path() or ".:/rom/programs"
local tAliases = parentShell and parentShell.aliases() or {}
local tCompletionInfo = parentShell and parentShell.getCompletionInfo() or {}
local tProgramStack = {}

local shell = {} --- @export
local function createShellEnv(dir)
    local env = { shell = shell, multishell = multishell }
    env.require, env.package = make_package(env, dir)
    return env
end

-- Colours
local promptColour, textColour, bgColour
if term.isColour() then
    promptColour = colours.yellow
    textColour = colours.white
    bgColour = colours.black
else
    promptColour = colours.white
    textColour = colours.white
    bgColour = colours.black
end

--- Run a program with the supplied arguments.
--
-- Unlike @{shell.run}, each argument is passed to the program verbatim. While
-- `shell.run("echo", "b c")` runs `echo` with `b` and `c`,
-- `shell.execute("echo", "b c")` runs `echo` with a single argument `b c`.
--
-- @tparam string command The program to execute.
-- @tparam string ... Arguments to this program.
-- @treturn boolean Whether the program exited successfully.
-- @usage Run `paint my-image` from within your program:
--
--     shell.execute("paint", "my-image")
function shell.execute(command, ...)
    expect(1, command, "string")
    for i = 1, select('#', ...) do
        expect(i + 1, select(i, ...), "string")
    end

    local sPath = shell.resolveProgram(command)
    if sPath ~= nil then
        tProgramStack[#tProgramStack + 1] = sPath
        if multishell then
            local sTitle = fs.getName(sPath)
            if sTitle:sub(-4) == ".lua" then
                sTitle = sTitle:sub(1, -5)
            end
            multishell.setTitle(multishell.getCurrent(), sTitle)
        end

        local sDir = fs.getDir(sPath)
        local env = createShellEnv(sDir)
        env.arg = { [0] = command, ... }
        local result = os.run(env, sPath, ...)

        tProgramStack[#tProgramStack] = nil
        if multishell then
            if #tProgramStack > 0 then
                local sTitle = fs.getName(tProgramStack[#tProgramStack])
                if sTitle:sub(-4) == ".lua" then
                    sTitle = sTitle:sub(1, -5)
                end
                multishell.setTitle(multishell.getCurrent(), sTitle)
            else
                multishell.setTitle(multishell.getCurrent(), "shell")
            end
        end
        return result
       else
        printError("No such program")
        return false
    end
end

local function tokenise(...)
    local sLine = table.concat({ ... }, " ")
    local tWords = {}
    local bQuoted = false
    for match in string.gmatch(sLine .. "\"", "(.-)\"") do
        if bQuoted then
            table.insert(tWords, match)
        else
            for m in string.gmatch(match, "[^ \t]+") do
                table.insert(tWords, m)
            end
        end
        bQuoted = not bQuoted
    end
    return tWords
end

-- Install shell API

--- Run a program with the supplied arguments.
--
-- All arguments are concatenated together and then parsed as a command line. As
-- a result, `shell.run("program a b")` is the same as `shell.run("program",
-- "a", "b")`.
--
-- @tparam string ... The program to run and its arguments.
-- @treturn boolean Whether the program exited successfully.
-- @usage Run `paint my-image` from within your program:
--
--     shell.run("paint", "my-image")
-- @see shell.execute Run a program directly without parsing the arguments.
function shell.run(...)
    local tWords = tokenise(...)
    local sCommand = tWords[1]
    if sCommand then
        return shell.execute(sCommand, table.unpack(tWords, 2))
    end
    return false
end

--- Exit the current shell.
--
-- This does _not_ terminate your program, it simply makes the shell terminate
-- after your program has finished. If this is the toplevel shell, then the
-- computer will be shutdown.
function shell.exit()
    bExit = true
end

--- Return the current working directory. This is what is displayed before the
-- `> ` of the shell prompt, and is used by @{shell.resolve} to handle relative
-- paths.
--
-- @treturn string The current working directory.
-- @see setDir To change the working directory.
function shell.dir()
    return sDir
end

--- Set the current working directory.
--
-- @tparam string dir The new working directory.
-- @throws If the path does not exist or is not a directory.
-- @usage Set the working directory to "rom"
--
--     shell.setDir("rom")
function shell.setDir(dir)
    expect(1, dir, "string")
    if not fs.isDir(dir) then
        error("Not a directory", 2)
    end
    sDir = fs.combine(dir, "")
end

--- Set the path where programs are located.
--
-- The path is composed of a list of directory names in a string, each separated
-- by a colon (`:`). On normal turtles will look in the current directory (`.`),
-- `/rom/programs` and `/rom/programs/turtle` folder, making the path
-- `.:/rom/programs:/rom/programs/turtle`.
--
-- @treturn string The current shell's path.
-- @see setPath To change the current path.
function shell.path()
    return sPath
end

--- Set the @{path|current program path}.
--
-- Be careful to prefix directories with a `/`. Otherwise they will be searched
-- for from the @{shell.dir|current directory}, rather than the computer's root.
--
-- @tparam string path The new program path.
function shell.setPath(path)
    expect(1, path, "string")
    sPath = path
end

--- Resolve a relative path to an absolute path.
--
-- The @{fs} and @{io} APIs work using absolute paths, and so we must convert
-- any paths relative to the @{dir|current directory} to absolute ones. This
-- does nothing when the path starts with `/`.
--
-- @tparam string path The path to resolve.
-- @usage Resolve `startup.lua` when in the `rom` folder.
--
--     shell.setDir("rom")
--     print(shell.resolve("startup.lua"))
--     -- => rom/startup.lua
function shell.resolve(path)
    expect(1, path, "string")
    local sStartChar = string.sub(path, 1, 1)
    if sStartChar == "/" or sStartChar == "\\" then
        return fs.combine("", path)
    else
        return fs.combine(sDir, path)
    end
end

local function pathWithExtension(_sPath, _sExt)
    local nLen = #sPath
    local sEndChar = string.sub(_sPath, nLen, nLen)
    -- Remove any trailing slashes so we can add an extension to the path safely
    if sEndChar == "/" or sEndChar == "\\" then
        _sPath = string.sub(_sPath, 1, nLen - 1)
    end
    return _sPath .. "." .. _sExt
end

--- Resolve a program, using the @{path|program path} and list of @{aliases|aliases}.
--
-- @tparam string command The name of the program
-- @treturn string|nil The absolute path to the program, or @{nil} if it could
-- not be found.
-- @usage Locate the `hello` program.
--
--      shell.resolveProgram("hello")
--      -- => rom/programs/fun/hello.lua
function shell.resolveProgram(command)
    expect(1, command, "string")
    -- Substitute aliases firsts
    if tAliases[command] ~= nil then
        command = tAliases[command]
    end

    -- If the path is a global path, use it directly
    if command:find("/") or command:find("\\") then
        local sPath = shell.resolve(command)
        if fs.exists(sPath) and not fs.isDir(sPath) then
            return sPath
        else
            local sPathLua = pathWithExtension(sPath, "lua")
            if fs.exists(sPathLua) and not fs.isDir(sPathLua) then
                return sPathLua
            end
        end
        return nil
    end

     -- Otherwise, look on the path variable
    for sPath in string.gmatch(sPath, "[^:]+") do
        sPath = fs.combine(shell.resolve(sPath), command)
        if fs.exists(sPath) and not fs.isDir(sPath) then
            return sPath
        else
            local sPathLua = pathWithExtension(sPath, "lua")
            if fs.exists(sPathLua) and not fs.isDir(sPathLua) then
                return sPathLua
            end
        end
    end

    -- Not found
    return nil
end

--- Return a list of all programs on the @{shell.path|path}.
--
-- @tparam[opt] boolean include_hidden Include hidden files. Namely, any which
-- start with `.`.
-- @treturn { string } A list of available programs.
-- @usage textutils.tabulate(shell.programs())
function shell.programs(include_hidden)
    expect(1, include_hidden, "boolean", "nil")

    local tItems = {}

    -- Add programs from the path
    for sPath in string.gmatch(sPath, "[^:]+") do
        sPath = shell.resolve(sPath)
        if fs.isDir(sPath) then
            local tList = fs.list(sPath)
            for n = 1, #tList do
                local sFile = tList[n]
                if not fs.isDir(fs.combine(sPath, sFile)) and
                   (include_hidden or string.sub(sFile, 1, 1) ~= ".") then
                    if #sFile > 4 and sFile:sub(-4) == ".lua" then
                        sFile = sFile:sub(1, -5)
                    end
                    tItems[sFile] = true
                end
            end
        end
    end

    -- Sort and return
    local tItemList = {}
    for sItem in pairs(tItems) do
        table.insert(tItemList, sItem)
    end
    table.sort(tItemList)
    return tItemList
end

local function completeProgram(sLine)
    if #sLine > 0 and (sLine:find("/") or sLine:find("\\")) then
        -- Add programs from the root
        return fs.complete(sLine, sDir, true, false)

    else
        local tResults = {}
        local tSeen = {}

        -- Add aliases
        for sAlias in pairs(tAliases) do
            if #sAlias > #sLine and string.sub(sAlias, 1, #sLine) == sLine then
                local sResult = string.sub(sAlias, #sLine + 1)
                if not tSeen[sResult] then
                    table.insert(tResults, sResult)
                    tSeen[sResult] = true
                end
            end
        end

        -- Add all subdirectories. We don't include files as they will be added in the block below
        local tDirs = fs.complete(sLine, sDir, false, false)
        for i = 1, #tDirs do
            local sResult = tDirs[i]
            if not tSeen[sResult] then
                table.insert (tResults, sResult)
                tSeen [sResult] = true
            end
        end

        -- Add programs from the path
        local tPrograms = shell.programs()
        for n = 1, #tPrograms do
            local sProgram = tPrograms[n]
            if #sProgram > #sLine and string.sub(sProgram, 1, #sLine) == sLine then
                local sResult = string.sub(sProgram, #sLine + 1)
                if not tSeen[sResult] then
                    table.insert(tResults, sResult)
                    tSeen[sResult] = true
                end
            end
        end

        -- Sort and return
        table.sort(tResults)
        return tResults
    end
end

local function completeProgramArgument(sProgram, nArgument, sPart, tPreviousParts)
    local tInfo = tCompletionInfo[sProgram]
    if tInfo then
        return tInfo.fnComplete(shell, nArgument, sPart, tPreviousParts)
    end
    return nil
end

--- Complete a shell command line.
--
-- This accepts an incomplete command, and completes the program name or
-- arguments. For instance, `l` will be completed to `ls`, and `ls ro` will be
-- completed to `ls rom/`.
--
-- Completion handlers for your program may be registered with
-- @{shell.setCompletionFunction}.
--
-- @tparam string sLine The input to complete.
-- @treturn { string }|nil The list of possible completions.
-- @see _G.read For more information about completion.
-- @see shell.completeProgram
-- @see shell.setCompletionFunction
-- @see shell.getCompletionInfo
function shell.complete(sLine)
    expect(1, sLine, "string")
    if #sLine > 0 then
        local tWords = tokenise(sLine)
        local nIndex = #tWords
        if string.sub(sLine, #sLine, #sLine) == " " then
            nIndex = nIndex + 1
        end
        if nIndex == 1 then
            local sBit = tWords[1] or ""
            local sPath = shell.resolveProgram(sBit)
            if tCompletionInfo[sPath] then
                return { " " }
            else
                local tResults = completeProgram(sBit)
                for n = 1, #tResults do
                    local sResult = tResults[n]
                    local sPath = shell.resolveProgram(sBit .. sResult)
                    if tCompletionInfo[sPath] then
                        tResults[n] = sResult .. " "
                    end
                end
                return tResults
            end

        elseif nIndex > 1 then
            local sPath = shell.resolveProgram(tWords[1])
            local sPart = tWords[nIndex] or ""
            local tPreviousParts = tWords
            tPreviousParts[nIndex] = nil
            return completeProgramArgument(sPath , nIndex - 1, sPart, tPreviousParts)

        end
    end
    return nil
end

--- Complete the name of a program.
--
-- @tparam string program The name of a program to complete.
-- @treturn { string } A list of possible completions.
-- @see cc.shell.completion.program
function shell.completeProgram(program)
    expect(1, program, "string")
    return completeProgram(program)
end

--- Set the completion function for a program. When the program is entered on
-- the command line, this program will be called to provide auto-complete
-- information.
--
-- The completion function accepts four arguments:
--
--  1. The current shell. As completion functions are inherited, this is not
--     guaranteed to be the shell you registered this function in.
--  2. The index of the argument currently being completed.
--  3. The current argument. This may be the empty string.
--  4. A list of the previous arguments.
--
-- For instance, when completing `pastebin put rom/st` our pastebin completion
-- function will receive the shell API, an index of 2, `rom/st` as the current
-- argument, and a "previous" table of `{ "put" }`. This function may then wish
-- to return a table containing `artup.lua`, indicating the entire command
-- should be completed to `pastebin put rom/startup.lua`.
--
-- You completion entries may also be followed by a space, if you wish to
-- indicate another argument is expected.
--
-- @tparam string program The path to the program. This should be an absolute path
-- _without_ the leading `/`.
-- @tparam function(shell: table, index: number, argument: string, previous: { string }):({ string }|nil) complete
-- The completion function.
-- @see cc.shell.completion Various utilities to help with writing completion functions.
-- @see shell.complete
-- @see _G.read For more information about completion.
function shell.setCompletionFunction(program, complete)
    expect(1, program, "string")
    expect(2, complete, "function")
    tCompletionInfo[program] = {
        fnComplete = complete,
    }
end

--- Get a table containing all completion functions.
--
-- This should only be needed when building custom shells. Use
-- @{setCompletionFunction} to add a completion function.
--
-- @treturn { [string] = { fnComplete = function } } A table mapping the
-- absolute path of programs, to their completion functions.
function shell.getCompletionInfo()
    return tCompletionInfo
end

--- Returns the path to the currently running program.
--
-- @treturn string The absolute path to the running program.
function shell.getRunningProgram()
    if #tProgramStack > 0 then
        return tProgramStack[#tProgramStack]
    end
    return nil
end

--- Add an alias for a program.
--
-- @tparam string command The name of the alias to add.
-- @tparam string program The name or path to the program.
-- @usage Alias `vim` to the `edit` program
--
--     shell.setAlias("vim", "edit")
function shell.setAlias(command, program)
    expect(1, command, "string")
    expect(2, program, "string")
    tAliases[command] = program
end

--- Remove an alias.
--
-- @tparam string command The alias name to remove.
function shell.clearAlias(command)
    expect(1, command, "string")
    tAliases[command] = nil
end

--- Get the current aliases for this shell.
--
-- Aliases are used to allow multiple commands to refer to a single program. For
-- instance, the `list` program is aliased `dir` or `ls`. Running `ls`, `dir` or
-- `list` in the shell will all run the `list` program.
--
-- @treturn { [string] = string } A table, where the keys are the names of
-- aliases, and the values are the path to the program.
-- @see shell.setAlias
-- @see shell.resolveProgram This uses aliases when resolving a program name to
-- an absolute path.
function shell.aliases()
    -- Copy aliases
    local tCopy = {}
    for sAlias, sCommand in pairs(tAliases) do
        tCopy[sAlias] = sCommand
    end
    return tCopy
end

if multishell then
    --- Open a new @{multishell} tab running a command.
    --
    -- This behaves similarly to @{shell.run}, but instead returns the process
    -- index.
    --
    -- This function is only available if the @{multishell} API is.
    --
    -- @tparam string ... The command line to run.
    -- @see shell.run
    -- @see multishell.launch
    -- @usage Launch the Lua interpreter and switch to it.
    --
    --     local id = shell.openTab("lua")
    --     shell.switchTab(id)
    function shell.openTab(...)
        local tWords = tokenise(...)
        local sCommand = tWords[1]
        if sCommand then
            local sPath = shell.resolveProgram(sCommand)
            if sPath == "rom/programs/shell.lua" then
                return multishell.launch(createShellEnv("rom/programs"), sPath, table.unpack(tWords, 2))
            elseif sPath ~= nil then
                return multishell.launch(createShellEnv("rom/programs"), "rom/programs/shell.lua", sCommand, table.unpack(tWords, 2))
            else
                printError("No such program")
            end
        end
    end

    --- Switch to the @{multishell} tab with the given index.
    --
    -- @tparam number id The tab to switch to.
    -- @see multishell.setFocus
    function shell.switchTab(id)
        expect(1, id, "number")
        multishell.setFocus(id)
    end
end

local tArgs = { ... }
if #tArgs > 0 then
    -- "shell x y z"
    -- Run the program specified on the commandline
    shell.run(...)

else
    -- "shell"
    -- Print the header
    term.setBackgroundColor(bgColour)
    term.setTextColour(promptColour)
    print(os.version())
    term.setTextColour(textColour)

    -- Run the startup program
    if parentShell == nil then
        shell.run("/rom/startup.lua")
    end

    -- Read commands and execute them
    local tCommandHistory = {}
    while not bExit do
        term.redirect(parentTerm)
        term.setBackgroundColor(bgColour)
        term.setTextColour(promptColour)
        write(shell.dir() .. "> ")
        term.setTextColour(textColour)


        local sLine
        if settings.get("shell.autocomplete") then
            sLine = read(nil, tCommandHistory, shell.complete)
        else
            sLine = read(nil, tCommandHistory)
        end
        if sLine:match("%S") and tCommandHistory[#tCommandHistory] ~= sLine then
            table.insert(tCommandHistory, sLine)
        end
        shell.run(sLine)
    end
end
