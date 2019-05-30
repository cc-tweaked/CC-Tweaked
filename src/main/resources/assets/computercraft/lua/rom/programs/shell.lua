
local multishell = multishell
local parentShell = shell
local parentTerm = term.current()

if multishell then
    multishell.setTitle( multishell.getCurrent(), "shell" )
end

local bExit = false
local sDir = (parentShell and parentShell.dir()) or ""
local sPath = (parentShell and parentShell.path()) or ".:/rom/programs"
local tAliases = (parentShell and parentShell.aliases()) or {}
local tCompletionInfo = (parentShell and parentShell.getCompletionInfo()) or {}
local tProgramStack = {}

local shell = {}
local function createShellEnv( sDir )
    local tEnv = {}
    tEnv[ "shell" ] = shell
    tEnv[ "multishell" ] = multishell

    local package = {}
    package.loaded = {
        _G = _G,
        bit32 = bit32,
        coroutine = coroutine,
        math = math,
        package = package,
        string = string,
        table = table,
    }
    package.path = "?;?.lua;?/init.lua;/rom/modules/main/?;/rom/modules/main/?.lua;/rom/modules/main/?/init.lua"
    if turtle then
        package.path = package.path..";/rom/modules/turtle/?;/rom/modules/turtle/?.lua;/rom/modules/turtle/?/init.lua"
    elseif command then
        package.path = package.path..";/rom/modules/command/?;/rom/modules/command/?.lua;/rom/modules/command/?/init.lua"
    end
    package.config = "/\n;\n?\n!\n-"
    package.preload = {}
    package.loaders = {
        function( name )
            if package.preload[name] then
                return package.preload[name]
            else
                return nil, "no field package.preload['" .. name .. "']"
            end
        end,
        function( name )
            local fname = string.gsub(name, "%.", "/")
            local sError = ""
            for pattern in string.gmatch(package.path, "[^;]+") do
                local sPath = string.gsub(pattern, "%?", fname)
                if sPath:sub(1,1) ~= "/" then
                    sPath = fs.combine(sDir, sPath)
                end
                if fs.exists(sPath) and not fs.isDir(sPath) then
                    local fnFile, sError = loadfile( sPath, tEnv )
                    if fnFile then
                        return fnFile, sPath
                    else
                        return nil, sError
                    end
                else
                    if #sError > 0 then
                        sError = sError .. "\n  "
                    end
                    sError = sError .. "no file '" .. sPath .. "'"
                end
            end
            return nil, sError
        end
    }

    local sentinel = {}
    local function require( name )
        expect(1, name, "string")
        if package.loaded[name] == sentinel then
            error("loop or previous error loading module '" .. name .. "'", 0)
        end
        if package.loaded[name] then
            return package.loaded[name]
        end

        local sError = "module '" .. name .. "' not found:"
        for _, searcher in ipairs(package.loaders) do
            local loader = table.pack(searcher(name))
            if loader[1] then
                package.loaded[name] = sentinel
                local result = loader[1](name, table.unpack(loader, 2, loader.n))
                if result == nil then result = true end

                package.loaded[name] = result
                return result
            else
                sError = sError .. "\n  " .. loader[2]
            end
        end
        error(sError, 2)
    end

    tEnv["package"] = package
    tEnv["require"] = require

    return tEnv
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

local function run( _sCommand, ... )
    local sPath = shell.resolveProgram( _sCommand )
    if sPath ~= nil then
        tProgramStack[#tProgramStack + 1] = sPath
        if multishell then
            local sTitle = fs.getName( sPath )
            if sTitle:sub(-4) == ".lua" then
                sTitle = sTitle:sub(1,-5)
            end
            multishell.setTitle( multishell.getCurrent(), sTitle )
        end

        local sDir = fs.getDir( sPath )
        local env = createShellEnv( sDir )
        env[ "arg" ] = { [0] = _sCommand, ... }
        local result = os.run( env, sPath, ... )

        tProgramStack[#tProgramStack] = nil
        if multishell then
            if #tProgramStack > 0 then
                local sTitle = fs.getName( tProgramStack[#tProgramStack] )
                if sTitle:sub(-4) == ".lua" then
                    sTitle = sTitle:sub(1,-5)
                end
                multishell.setTitle( multishell.getCurrent(), sTitle )
            else
                multishell.setTitle( multishell.getCurrent(), "shell" )
            end
        end
        return result
       else
        printError( "No such program" )
        return false
    end
end

local function tokenise( ... )
    local sLine = table.concat( { ... }, " " )
    local tWords = {}
    local bQuoted = false
    for match in string.gmatch( sLine .. "\"", "(.-)\"" ) do
        if bQuoted then
            table.insert( tWords, match )
        else
            for m in string.gmatch( match, "[^ \t]+" ) do
                table.insert( tWords, m )
            end
        end
        bQuoted = not bQuoted
    end
    return tWords
end

-- Install shell API
function shell.run( ... )
    local tWords = tokenise( ... )
    local sCommand = tWords[1]
    if sCommand then
        return run( sCommand, table.unpack( tWords, 2 ) )
    end
    return false
end

function shell.exit()
    bExit = true
end

function shell.dir()
    return sDir
end

function shell.setDir( _sDir )
    expect(1, _sDir, "string")
    if not fs.isDir( _sDir ) then
        error( "Not a directory", 2 )
    end
    sDir = _sDir
end

function shell.path()
    return sPath
end

function shell.setPath( _sPath )
    expect(1, _sPath, "string")
    sPath = _sPath
end

function shell.resolve( _sPath )
    expect(1, _sPath, "string")
    local sStartChar = string.sub( _sPath, 1, 1 )
    if sStartChar == "/" or sStartChar == "\\" then
        return fs.combine( "", _sPath )
    else
        return fs.combine( sDir, _sPath )
    end
end

local function pathWithExtension( _sPath, _sExt )
    local nLen = #sPath
    local sEndChar = string.sub( _sPath, nLen, nLen )
    -- Remove any trailing slashes so we can add an extension to the path safely
    if sEndChar == "/" or sEndChar == "\\" then
        _sPath = string.sub( _sPath, 1, nLen - 1 )
    end
    return _sPath .. "." .. _sExt
end

function shell.resolveProgram( _sCommand )
    expect(1, _sCommand, "string")
    -- Substitute aliases firsts
    if tAliases[ _sCommand ] ~= nil then
        _sCommand = tAliases[ _sCommand ]
    end

    -- If the path is a global path, use it directly
    if _sCommand:find("/") or _sCommand:find("\\") then
        local sPath = shell.resolve( _sCommand )
        if fs.exists( sPath ) and not fs.isDir( sPath ) then
            return sPath
        else
            local sPathLua = pathWithExtension( sPath, "lua" )
            if fs.exists( sPathLua ) and not fs.isDir( sPathLua ) then
                return sPathLua
            end
        end
        return nil
    end

     -- Otherwise, look on the path variable
    for sPath in string.gmatch(sPath, "[^:]+") do
        sPath = fs.combine( shell.resolve( sPath ), _sCommand )
        if fs.exists( sPath ) and not fs.isDir( sPath ) then
            return sPath
        else
            local sPathLua = pathWithExtension( sPath, "lua" )
            if fs.exists( sPathLua ) and not fs.isDir( sPathLua ) then
                return sPathLua
            end
        end
    end

    -- Not found
    return nil
end

function shell.programs( _bIncludeHidden )
    local tItems = {}

    -- Add programs from the path
    for sPath in string.gmatch(sPath, "[^:]+") do
        sPath = shell.resolve( sPath )
        if fs.isDir( sPath ) then
            local tList = fs.list( sPath )
            for n=1,#tList do
                local sFile = tList[n]
                if not fs.isDir( fs.combine( sPath, sFile ) ) and
                   (_bIncludeHidden or string.sub( sFile, 1, 1 ) ~= ".") then
                    if #sFile > 4 and sFile:sub(-4) == ".lua" then
                        sFile = sFile:sub(1,-5)
                    end
                    tItems[ sFile ] = true
                end
            end
        end
    end

    -- Sort and return
    local tItemList = {}
    for sItem, b in pairs( tItems ) do
        table.insert( tItemList, sItem )
    end
    table.sort( tItemList )
    return tItemList
end

local function completeProgram( sLine )
    if #sLine > 0 and (sLine:find("/") or sLine:find("\\")) then
        -- Add programs from the root
        return fs.complete( sLine, sDir, true, false )

    else
        local tResults = {}
        local tSeen = {}

        -- Add aliases
        for sAlias, sCommand in pairs( tAliases ) do
            if #sAlias > #sLine and string.sub( sAlias, 1, #sLine ) == sLine then
                local sResult = string.sub( sAlias, #sLine + 1 )
                if not tSeen[ sResult ] then
                    table.insert( tResults, sResult )
                    tSeen[ sResult ] = true
                end
            end
        end

        -- Add all subdirectories. We don't include files as they will be added in the block below
        local tDirs = fs.complete( sLine, sDir, false, false )
        for i = 1, #tDirs do
            local sResult = tDirs[i]
            if not tSeen[ sResult ] then
                table.insert ( tResults, sResult )
                tSeen [ sResult ] = true
            end
        end

        -- Add programs from the path
        local tPrograms = shell.programs()
        for n=1,#tPrograms do
            local sProgram = tPrograms[n]
            if #sProgram > #sLine and string.sub( sProgram, 1, #sLine ) == sLine then
                local sResult = string.sub( sProgram, #sLine + 1 )
                if not tSeen[ sResult ] then
                    table.insert( tResults, sResult )
                    tSeen[ sResult ] = true
                end
            end
        end

        -- Sort and return
        table.sort( tResults )
        return tResults
    end
end

local function completeProgramArgument( sProgram, nArgument, sPart, tPreviousParts )
    local tInfo = tCompletionInfo[ sProgram ]
    if tInfo then
        return tInfo.fnComplete( shell, nArgument, sPart, tPreviousParts )
    end
    return nil
end

function shell.complete( sLine )
    expect(1, sLine, "string")
    if #sLine > 0 then
        local tWords = tokenise( sLine )
        local nIndex = #tWords
        if string.sub( sLine, #sLine, #sLine ) == " " then
            nIndex = nIndex + 1
        end
        if nIndex == 1 then
            local sBit = tWords[1] or ""
            local sPath = shell.resolveProgram( sBit )
            if tCompletionInfo[ sPath ] then
                return { " " }
            else
                local tResults = completeProgram( sBit )
                for n=1,#tResults do
                    local sResult = tResults[n]
                    local sPath = shell.resolveProgram( sBit .. sResult )
                    if tCompletionInfo[ sPath ] then
                        tResults[n] = sResult .. " "
                    end
                end
                return tResults
            end

        elseif nIndex > 1 then
            local sPath = shell.resolveProgram( tWords[1] )
            local sPart = tWords[nIndex] or ""
            local tPreviousParts = tWords
            tPreviousParts[nIndex] = nil
            return completeProgramArgument( sPath , nIndex - 1, sPart, tPreviousParts )

        end
    end
    return nil
end

function shell.completeProgram( sProgram )
    expect(1, sProgram, "string")
    return completeProgram( sProgram )
end

function shell.setCompletionFunction( sProgram, fnComplete )
    expect(1, sProgram, "string")
    expect(2, fnComplete, "function")
    tCompletionInfo[ sProgram ] = {
        fnComplete = fnComplete
    }
end

function shell.getCompletionInfo()
    return tCompletionInfo
end

function shell.getRunningProgram()
    if #tProgramStack > 0 then
        return tProgramStack[#tProgramStack]
    end
    return nil
end

function shell.setAlias( _sCommand, _sProgram )
    expect(1, _sCommand, "string")
    expect(2, _sProgram, "string")
    tAliases[ _sCommand ] = _sProgram
end

function shell.clearAlias( _sCommand )
    expect(1, _sCommand, "string")
    tAliases[ _sCommand ] = nil
end

function shell.aliases()
    -- Copy aliases
    local tCopy = {}
    for sAlias, sCommand in pairs( tAliases ) do
        tCopy[sAlias] = sCommand
    end
    return tCopy
end

if multishell then
    function shell.openTab( ... )
        local tWords = tokenise( ... )
        local sCommand = tWords[1]
        if sCommand then
            local sPath = shell.resolveProgram( sCommand )
            if sPath == "rom/programs/shell.lua" then
                return multishell.launch( createShellEnv( "rom/programs" ), sPath, table.unpack( tWords, 2 ) )
            elseif sPath ~= nil then
                return multishell.launch( createShellEnv( "rom/programs" ), "rom/programs/shell.lua", sCommand, table.unpack( tWords, 2 ) )
            else
                printError( "No such program" )
            end
        end
    end

    function shell.switchTab( nID )
        expect(1, nID, "number")
        multishell.setFocus( nID )
    end
end

local tArgs = { ... }
if #tArgs > 0 then
    -- "shell x y z"
    -- Run the program specified on the commandline
    shell.run( ... )

else
    -- "shell"
    -- Print the header
    term.setBackgroundColor( bgColour )
    term.setTextColour( promptColour )
    print( os.version() )
    term.setTextColour( textColour )

    -- Run the startup program
    if parentShell == nil then
        shell.run( "/rom/startup.lua" )
    end

    -- Read commands and execute them
    local tCommandHistory = {}
    while not bExit do
        term.redirect( parentTerm )
        term.setBackgroundColor( bgColour )
        term.setTextColour( promptColour )
        write( shell.dir() .. "> " )
        term.setTextColour( textColour )


        local sLine
        if settings.get( "shell.autocomplete" ) then
            sLine = read( nil, tCommandHistory, shell.complete )
        else
            sLine = read( nil, tCommandHistory )
        end
        if sLine:match("%S") and tCommandHistory[#tCommandHistory] ~= sLine then
            table.insert( tCommandHistory, sLine )
        end
        shell.run( sLine )
    end
end
