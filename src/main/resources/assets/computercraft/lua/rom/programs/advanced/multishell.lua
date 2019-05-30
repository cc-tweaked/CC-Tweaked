local expect = _G["~expect"]

-- Setup process switching
local parentTerm = term.current()
local w,h = parentTerm.getSize()

local tProcesses = {}
local nCurrentProcess = nil
local nRunningProcess = nil
local bShowMenu = false
local bWindowsResized = false
local nScrollPos = 1
local bScrollRight = false

local function selectProcess( n )
    if nCurrentProcess ~= n then
        if nCurrentProcess then
            local tOldProcess = tProcesses[ nCurrentProcess ]
            tOldProcess.window.setVisible( false )
        end
        nCurrentProcess = n
        if nCurrentProcess then
            local tNewProcess = tProcesses[ nCurrentProcess ]
            tNewProcess.window.setVisible( true )
            tNewProcess.bInteracted = true
        end
    end
end

local function setProcessTitle( n, sTitle )
    tProcesses[ n ].sTitle = sTitle
end

local function resumeProcess( nProcess, sEvent, ... )
    local tProcess = tProcesses[ nProcess ]
    local sFilter = tProcess.sFilter
    if sFilter == nil or sFilter == sEvent or sEvent == "terminate" then
        local nPreviousProcess = nRunningProcess
        nRunningProcess = nProcess
        term.redirect( tProcess.terminal )
        local ok, result = coroutine.resume( tProcess.co, sEvent, ... )
        tProcess.terminal = term.current()
        if ok then
            tProcess.sFilter = result
        else
            printError( result )
        end
        nRunningProcess = nPreviousProcess
    end
end

local function launchProcess( bFocus, tProgramEnv, sProgramPath, ... )
    local tProgramArgs = table.pack( ... )
    local nProcess = #tProcesses + 1
    local tProcess = {}
    tProcess.sTitle = fs.getName( sProgramPath )
    if bShowMenu then
        tProcess.window = window.create( parentTerm, 1, 2, w, h-1, false )
    else
        tProcess.window = window.create( parentTerm, 1, 1, w, h, false )
    end
    tProcess.co = coroutine.create( function()
        os.run( tProgramEnv, sProgramPath, table.unpack( tProgramArgs, 1, tProgramArgs.n ) )
        if not tProcess.bInteracted then
            term.setCursorBlink( false )
            print( "Press any key to continue" )
            os.pullEvent( "char" )
        end
    end )
    tProcess.sFilter = nil
    tProcess.terminal = tProcess.window
    tProcess.bInteracted = false
    tProcesses[ nProcess ] = tProcess
    if bFocus then
        selectProcess( nProcess )
    end
    resumeProcess( nProcess )
    return nProcess
end

local function cullProcess( nProcess )
    local tProcess = tProcesses[ nProcess ]
    if coroutine.status( tProcess.co ) == "dead" then
        if nCurrentProcess == nProcess then
            selectProcess( nil )
        end
        table.remove( tProcesses, nProcess )
        if nCurrentProcess == nil then
            if nProcess > 1 then
                selectProcess( nProcess - 1 )
            elseif #tProcesses > 0 then
                selectProcess( 1 )
            end
        end
        if nScrollPos ~= 1 then
            nScrollPos = nScrollPos - 1
        end
        return true
    end
    return false
end

local function cullProcesses()
    local culled = false
    for n=#tProcesses,1,-1 do
        culled = culled or cullProcess( n )
    end
    return culled
end

-- Setup the main menu
local menuMainTextColor, menuMainBgColor, menuOtherTextColor, menuOtherBgColor
if parentTerm.isColor() then
    menuMainTextColor, menuMainBgColor = colors.yellow, colors.black
    menuOtherTextColor, menuOtherBgColor = colors.black, colors.gray
else
    menuMainTextColor, menuMainBgColor = colors.white, colors.black
    menuOtherTextColor, menuOtherBgColor = colors.black, colors.gray
end

local function redrawMenu()
    if bShowMenu then
        -- Draw menu
        parentTerm.setCursorPos( 1, 1 )
        parentTerm.setBackgroundColor( menuOtherBgColor )
        parentTerm.clearLine()
        local nCharCount = 0
        local nSize = parentTerm.getSize()
        if nScrollPos ~= 1 then
            parentTerm.setTextColor( menuOtherTextColor )
            parentTerm.setBackgroundColor( menuOtherBgColor )
            parentTerm.write( "<" )
            nCharCount = 1
        end
        for n=nScrollPos,#tProcesses do
            if n == nCurrentProcess then
                parentTerm.setTextColor( menuMainTextColor )
                parentTerm.setBackgroundColor( menuMainBgColor )
            else
                parentTerm.setTextColor( menuOtherTextColor )
                parentTerm.setBackgroundColor( menuOtherBgColor )
            end
            parentTerm.write( " " .. tProcesses[n].sTitle .. " " )
            nCharCount = nCharCount + #tProcesses[n].sTitle + 2
        end
        if nCharCount > nSize then
            parentTerm.setTextColor( menuOtherTextColor )
            parentTerm.setBackgroundColor( menuOtherBgColor )
            parentTerm.setCursorPos( nSize, 1 )
            parentTerm.write( ">" )
            bScrollRight = true
        else
            bScrollRight = false
        end

        -- Put the cursor back where it should be
        local tProcess = tProcesses[ nCurrentProcess ]
        if tProcess then
            tProcess.window.restoreCursor()
        end
    end
end

local function resizeWindows()
    local windowY, windowHeight
    if bShowMenu then
        windowY = 2
        windowHeight = h-1
    else
        windowY = 1
        windowHeight = h
    end
    for n=1,#tProcesses do
        local tProcess = tProcesses[n]
        local window = tProcess.window
        local x,y = tProcess.window.getCursorPos()
        if y > windowHeight then
            tProcess.window.scroll( y - windowHeight )
            tProcess.window.setCursorPos( x, windowHeight )
        end
        tProcess.window.reposition( 1, windowY, w, windowHeight )
    end
    bWindowsResized = true
end

local function setMenuVisible( bVis )
    if bShowMenu ~= bVis then
        bShowMenu = bVis
        resizeWindows()
        redrawMenu()
    end
end

local multishell = {}

function multishell.getFocus()
    return nCurrentProcess
end

function multishell.setFocus( n )
    expect(1, n, "number")
    if n >= 1 and n <= #tProcesses then
        selectProcess( n )
        redrawMenu()
        return true
    end
    return false
end

function multishell.getTitle( n )
    expect(1, n, "number")
    if n >= 1 and n <= #tProcesses then
        return tProcesses[n].sTitle
    end
    return nil
end

function multishell.setTitle( n, sTitle )
    expect(1, n, "number")
    expect(2, sTitle, "string")
    if n >= 1 and n <= #tProcesses then
        setProcessTitle( n, sTitle )
        redrawMenu()
    end
end

function multishell.getCurrent()
    return nRunningProcess
end

function multishell.launch( tProgramEnv, sProgramPath, ... )
    expect(1, tProgramEnv, "table")
    expect(2, sProgramPath, "string")
    local previousTerm = term.current()
    setMenuVisible( (#tProcesses + 1) >= 2 )
    local nResult = launchProcess( false, tProgramEnv, sProgramPath, ... )
    redrawMenu()
    term.redirect( previousTerm )
    return nResult
end

function multishell.getCount()
    return #tProcesses
end

-- Begin
parentTerm.clear()
setMenuVisible( false )
launchProcess( true, {
    ["shell"] = shell,
    ["multishell"] = multishell,
}, "/rom/programs/shell.lua" )

-- Run processes
while #tProcesses > 0 do
    -- Get the event
    local tEventData = table.pack( os.pullEventRaw() )
    local sEvent = tEventData[1]
    if sEvent == "term_resize" then
        -- Resize event
        w,h = parentTerm.getSize()
        resizeWindows()
        redrawMenu()

    elseif sEvent == "char" or sEvent == "key" or sEvent == "key_up" or sEvent == "paste" or sEvent == "terminate" then
        -- Keyboard event
        -- Passthrough to current process
        resumeProcess( nCurrentProcess, table.unpack( tEventData, 1, tEventData.n ) )
        if cullProcess( nCurrentProcess ) then
            setMenuVisible( #tProcesses >= 2 )
            redrawMenu()
        end

    elseif sEvent == "mouse_click" then
        -- Click event
        local button, x, y = tEventData[2], tEventData[3], tEventData[4]
        if bShowMenu and y == 1 then
            -- Switch process
            if x == 1 and nScrollPos ~= 1 then
                nScrollPos = nScrollPos - 1
                redrawMenu()
            elseif bScrollRight and x == term.getSize() then
                nScrollPos = nScrollPos + 1
                redrawMenu()
            else
                local tabStart = 1
                if nScrollPos ~= 1 then
                    tabStart = 2
                end
                for n=nScrollPos,#tProcesses do
                    local tabEnd = tabStart + string.len( tProcesses[n].sTitle ) + 1
                    if x >= tabStart and x <= tabEnd then
                        selectProcess( n )
                        redrawMenu()
                        break
                    end
                    tabStart = tabEnd + 1
                end
            end
        else
            -- Passthrough to current process
            resumeProcess( nCurrentProcess, sEvent, button, x, (bShowMenu and y-1) or y )
            if cullProcess( nCurrentProcess ) then
                setMenuVisible( #tProcesses >= 2 )
                redrawMenu()
            end
        end

    elseif sEvent == "mouse_drag" or sEvent == "mouse_up" or sEvent == "mouse_scroll" then
        -- Other mouse event
        local p1, x, y = tEventData[2], tEventData[3], tEventData[4]
        if bShowMenu and sEvent == "mouse_scroll" and y == 1 then
            if p1 == -1 and nScrollPos ~= 1 then
                nScrollPos = nScrollPos - 1
                redrawMenu()
            elseif bScrollRight and p1 == 1 then
                nScrollPos = nScrollPos + 1
                redrawMenu()
            end
        elseif not (bShowMenu and y == 1) then
            -- Passthrough to current process
            resumeProcess( nCurrentProcess, sEvent, p1, x, (bShowMenu and y-1) or y )
            if cullProcess( nCurrentProcess ) then
                setMenuVisible( #tProcesses >= 2 )
                redrawMenu()
            end
        end

    else
        -- Other event
        -- Passthrough to all processes
        local nLimit = #tProcesses -- Storing this ensures any new things spawned don't get the event
        for n=1,nLimit do
            resumeProcess( n, table.unpack( tEventData, 1, tEventData.n ) )
        end
        if cullProcesses() then
            setMenuVisible( #tProcesses >= 2 )
            redrawMenu()
        end
    end

    if bWindowsResized then
        -- Pass term_resize to all processes
        local nLimit = #tProcesses -- Storing this ensures any new things spawned don't get the event
        for n=1,nLimit do
            resumeProcess( n, "term_resize" )
        end
        bWindowsResized = false
        if cullProcesses() then
            setMenuVisible( #tProcesses >= 2 )
            redrawMenu()
        end
    end
end

-- Shutdown
term.redirect( parentTerm )
