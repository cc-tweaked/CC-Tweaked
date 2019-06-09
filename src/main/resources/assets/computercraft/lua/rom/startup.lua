
-- Setup paths
local sPath = ".:/rom/programs"
local tConditions = {
    ["/rom/programs/advanced"] = term.isColor(),
    ["/rom/programs/turtle"] = turtle ~= nil,
    ["/rom/programs/rednet"] = turtle == nil,
    ["/rom/programs/fun"] = turtle == nil,
    ["/rom/programs/fun/advanced"] = turtle == nil and term.isColor(),
    ["/rom/programs/fun/advanced/levels"] = false,
    ["/rom/programs/pocket"] = pocket ~= nil,
    ["/rom/programs/command"] = command ~= nil,
    ["/rom/programs/http"] = http ~= nil,
}
local tPathStrings = fs.list( "rom/programs" )
local nPathIndex = 1
while nPathIndex < #tPathStrings do
    local sDir = "/rom/programs/" .. tPathStrings[ nPathIndex ]
    if fs.isDir( sDir ) then
        local tStr = fs.list( sDir )
        for k, v in pairs( tStr ) do
            tPathStrings[ #tPathStrings + 1 ] = fs.combine( tPathStrings[ nPathIndex ], v )
        end
    end
    nPathIndex = nPathIndex + 1
end
for _, sProgramPath in pairs( tPathStrings ) do
    local sPathComponent = "/rom/programs/" .. sProgramPath
    if fs.isDir( sPathComponent ) and tConditions[ sPathComponent ] ~= false then
        sPath = sPath .. ":" .. sPathComponent
    end
end
shell.setPath( sPath )
help.setPath( "/rom/help" )

-- Setup aliases
shell.setAlias( "ls", "list" )
shell.setAlias( "dir", "list" )
shell.setAlias( "cp", "copy" )
shell.setAlias( "mv", "move" )
shell.setAlias( "rm", "delete" )
shell.setAlias( "clr", "clear" )
shell.setAlias( "rs", "redstone" )
shell.setAlias( "sh", "shell" )
if term.isColor() then
    shell.setAlias( "background", "bg" )
    shell.setAlias( "foreground", "fg" )
end

-- Setup completion functions
local function completeMultipleChoice( sText, tOptions, bAddSpaces )
    local tResults = {}
    for n=1,#tOptions do
        local sOption = tOptions[n]
        if #sOption + (bAddSpaces and 1 or 0) > #sText and string.sub( sOption, 1, #sText ) == sText then
            local sResult = string.sub( sOption, #sText + 1 )
            if bAddSpaces then
                table.insert( tResults, sResult .. " " )
            else
                table.insert( tResults, sResult )
            end
        end
    end
    return tResults
end
local function completePeripheralName( sText, bAddSpaces )
    return completeMultipleChoice( sText, peripheral.getNames(), bAddSpaces )
end
local tRedstoneSides = redstone.getSides()
local function completeSide( sText, bAddSpaces )
    return completeMultipleChoice( sText, tRedstoneSides, bAddSpaces )
end
local function completeFile( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return fs.complete( sText, shell.dir(), true, false )
    end
end
local function completeFileMany( shell, nIndex, sText, tPreviousText )
    return fs.complete( sText, shell.dir(), true, false )
end
local function completeDir( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return fs.complete( sText, shell.dir(), false, true )
    end
end
local function completeEither( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return fs.complete( sText, shell.dir(), true, true )
    end
end
local function completeEitherMany( shell, nIndex, sText, tPreviousText )
    return fs.complete( sText, shell.dir(), true, true )
end
local function completeEitherEither( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        local tResults = fs.complete( sText, shell.dir(), true, true )
        for n=1,#tResults do
            local sResult = tResults[n]
            if string.sub( sResult, #sResult, #sResult ) ~= "/" then
                tResults[n] = sResult .. " "
            end
        end
        return tResults
    elseif nIndex == 2 then
        return fs.complete( sText, shell.dir(), true, true )
    end
end
local function completeProgram( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return shell.completeProgram( sText )
    end
end
local function completeHelp( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return help.completeTopic( sText )
    end
end
local function completeAlias( shell, nIndex, sText, tPreviousText )
    if nIndex == 2 then
        return shell.completeProgram( sText )
    end
end
local function completePeripheral( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return completePeripheralName( sText )
    end
end
local tGPSOptions = { "host", "host ", "locate" }
local function completeGPS( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return completeMultipleChoice( sText, tGPSOptions )
    end
end
local tLabelOptions = { "get", "get ", "set ", "clear", "clear " }
local function completeLabel( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return completeMultipleChoice( sText, tLabelOptions )
    elseif nIndex == 2 then
        return completePeripheralName( sText )
    end
end
local function completeMonitor( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return completePeripheralName( sText, true )
    elseif nIndex == 2 then
        return shell.completeProgram( sText )
    end
end
local tRedstoneOptions = { "probe", "set ", "pulse " }
local function completeRedstone( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return completeMultipleChoice( sText, tRedstoneOptions )
    elseif nIndex == 2 then
        return completeSide( sText )
    end
end
local tDJOptions = { "play", "play ", "stop " }
local function completeDJ( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return completeMultipleChoice( sText, tDJOptions )
    elseif nIndex == 2 then
        return completePeripheralName( sText )
    end
end
local tPastebinOptions = { "put ", "get ", "run " }
local function completePastebin( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return completeMultipleChoice( sText, tPastebinOptions )
    elseif nIndex == 2 then
        if tPreviousText[2] == "put" then
            return fs.complete( sText, shell.dir(), true, false )
        end
    end
end
local tChatOptions = { "host ", "join " }
local function completeChat( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return completeMultipleChoice( sText, tChatOptions )
    end
end
local function completeSet( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return completeMultipleChoice( sText, settings.getNames(), true )
    end
end
local tCommands
if commands then
    tCommands = commands.list()
end
local function completeExec( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 and commands then
        return completeMultipleChoice( sText, tCommands, true )
    end
end
local tWgetOptions = { "run" }
local function completeWget( shell, nIndex, sText, tPreviousText )
    if nIndex == 1 then
        return completeMultipleChoice( sText, tWgetOptions, true )
    end
end
shell.setCompletionFunction( "rom/programs/alias.lua", completeAlias )
shell.setCompletionFunction( "rom/programs/cd.lua", completeDir )
shell.setCompletionFunction( "rom/programs/copy.lua", completeEitherEither )
shell.setCompletionFunction( "rom/programs/delete.lua", completeEitherMany )
shell.setCompletionFunction( "rom/programs/drive.lua", completeDir )
shell.setCompletionFunction( "rom/programs/edit.lua", completeFile )
shell.setCompletionFunction( "rom/programs/eject.lua", completePeripheral )
shell.setCompletionFunction( "rom/programs/gps.lua", completeGPS )
shell.setCompletionFunction( "rom/programs/help.lua", completeHelp )
shell.setCompletionFunction( "rom/programs/id.lua", completePeripheral )
shell.setCompletionFunction( "rom/programs/label.lua", completeLabel )
shell.setCompletionFunction( "rom/programs/list.lua", completeDir )
shell.setCompletionFunction( "rom/programs/mkdir.lua", completeFileMany )
shell.setCompletionFunction( "rom/programs/monitor.lua", completeMonitor )
shell.setCompletionFunction( "rom/programs/move.lua", completeEitherEither )
shell.setCompletionFunction( "rom/programs/redstone.lua", completeRedstone )
shell.setCompletionFunction( "rom/programs/rename.lua", completeEitherEither )
shell.setCompletionFunction( "rom/programs/shell.lua", completeProgram )
shell.setCompletionFunction( "rom/programs/type.lua", completeEither )
shell.setCompletionFunction( "rom/programs/set.lua", completeSet )
shell.setCompletionFunction( "rom/programs/advanced/bg.lua", completeProgram )
shell.setCompletionFunction( "rom/programs/advanced/fg.lua", completeProgram )
shell.setCompletionFunction( "rom/programs/fun/dj.lua", completeDJ )
shell.setCompletionFunction( "rom/programs/fun/advanced/paint.lua", completeFile )
shell.setCompletionFunction( "rom/programs/http/pastebin.lua", completePastebin )
shell.setCompletionFunction( "rom/programs/rednet/chat.lua", completeChat )
shell.setCompletionFunction( "rom/programs/command/exec.lua", completeExec )
shell.setCompletionFunction( "rom/programs/http/wget.lua", completeWget )

if turtle then
    local tGoOptions = { "left", "right", "forward", "back", "down", "up" }
    local function completeGo( shell, nIndex, sText )
        return completeMultipleChoice( sText, tGoOptions, true)
    end
    local tTurnOptions = { "left", "right" }
    local function completeTurn( shell, nIndex, sText )
            return completeMultipleChoice( sText, tTurnOptions, true )
    end
    local tEquipOptions = { "left", "right" }
    local function completeEquip( shell, nIndex, sText )
        if nIndex == 2 then
            return completeMultipleChoice( sText, tEquipOptions )
        end
    end
    local function completeUnequip( shell, nIndex, sText )
        if nIndex == 1 then
            return completeMultipleChoice( sText, tEquipOptions )
        end
    end
    shell.setCompletionFunction( "rom/programs/turtle/go.lua", completeGo )
    shell.setCompletionFunction( "rom/programs/turtle/turn.lua", completeTurn )
    shell.setCompletionFunction( "rom/programs/turtle/equip.lua", completeEquip )
    shell.setCompletionFunction( "rom/programs/turtle/unequip.lua", completeUnequip )
end


-- Run autorun files
if fs.exists( "/rom/autorun" ) and fs.isDir( "/rom/autorun" ) then
    local tFiles = fs.list( "/rom/autorun" )
    table.sort( tFiles )
    for n, sFile in ipairs( tFiles ) do
        if string.sub( sFile, 1, 1 ) ~= "." then
            local sPath = "/rom/autorun/"..sFile
            if not fs.isDir( sPath ) then
                shell.run( sPath )
            end
        end
    end
end

local function findStartups( sBaseDir )
    local tStartups = nil
    local sBasePath = "/" .. fs.combine( sBaseDir, "startup" )
    local sStartupNode = shell.resolveProgram( sBasePath )
    if sStartupNode then
        tStartups = { sStartupNode }
    end
    -- It's possible that there is a startup directory and a startup.lua file, so this has to be
    -- executed even if a file has already been found.
    if fs.isDir( sBasePath ) then
        if tStartups == nil then
            tStartups = {}
        end
        for _,v in pairs( fs.list( sBasePath ) ) do
            local sPath = "/" .. fs.combine( sBasePath, v )
            if not fs.isDir( sPath ) then
                tStartups[ #tStartups + 1 ] = sPath
            end
        end
    end
    return tStartups
end

-- Show MOTD
if settings.get( "motd.enable" ) then
    shell.run( "motd" )
end

-- Run the user created startup, either from disk drives or the root
local tUserStartups = nil
if settings.get( "shell.allow_startup" ) then
    tUserStartups = findStartups( "/" )
end
if settings.get( "shell.allow_disk_startup" ) then
    for n,sName in pairs( peripheral.getNames() ) do
        if disk.isPresent( sName ) and disk.hasData( sName ) then
            local startups = findStartups( disk.getMountPath( sName ) )
            if startups then
                tUserStartups = startups
                break
            end
        end
    end
end
if tUserStartups then
    for _,v in pairs( tUserStartups ) do
        shell.run( v )
    end
end
