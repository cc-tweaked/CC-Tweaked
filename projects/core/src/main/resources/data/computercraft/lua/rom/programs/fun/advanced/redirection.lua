-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--CCRedirection by : RamiLego4Game and Dan200--
--Based on Redirection by Dan200: http://www.redirectiongame.com--
--Clearing Screen--

--Vars--
local TermW, TermH = term.getSize()

local sLevelTitle
local tScreen
local oScreen
local SizeW, SizeH
local aExits
local fExit
local nSpeed
local Speed
local fSpeed
local fSpeedS
local bPaused
local Tick
local Blocks
local XOrgin, YOrgin
local fLevel

local function reset()
    sLevelTitle = ""
    tScreen = {}
    oScreen = {}
    SizeW, SizeH = TermW, TermH
    aExits = 0
    fExit = "nop"
    nSpeed = 0.6
    Speed = nSpeed
    fSpeed = 0.2
    fSpeedS = false
    bPaused = false
    Tick = os.startTimer(Speed)
    Blocks = 0
    XOrgin, YOrgin = 1, 1

    term.setBackgroundColor(colors.black)
    term.setTextColor(colors.white)
    term.clear()
end

local InterFace = {}
InterFace.cExit = colors.red
InterFace.cSpeedD = colors.white
InterFace.cSpeedA = colors.red
InterFace.cTitle = colors.red

local cG = colors.lightGray
local cW = colors.gray
local cS = colors.black
local cR1 = colors.blue
local cR2 = colors.red
local cR3 = colors.green
local cR4 = colors.yellow

local tArgs = { ... }

--Functions--
local function printCentred(yc, stg)
    local xc = math.floor((TermW - #stg) / 2) + 1
    term.setCursorPos(xc, yc)
    term.write(stg)
end

local function centerOrgin()
    XOrgin = math.floor(TermW / 2 - SizeW / 2)
    YOrgin = math.floor(TermH / 2 - SizeH / 2)
end

local function reMap()
    tScreen = nil
    tScreen = {}
    for x = 1, SizeW do
        tScreen[x] = {}
        for y = 1, SizeH do
            tScreen[x][y] = { space = true, wall = false, ground = false, robot = "zz", start = "zz", exit = "zz" }
        end
    end
end

local function tablecopy(t)
  local t2 = {}
  for k, v in pairs(t) do
    t2[k] = v
  end
  return t2
end

local function buMap()
    oScreen = nil
    oScreen = {}
    for x = 1, SizeW do
        oScreen[x] = {}
        for y = 1, SizeH do
            oScreen[x][y] = tablecopy(tScreen[x][y])
        end
    end
end

local function addRobot(x, y, side, color)
    local obj = tScreen[x][y]
    local data = side .. color
    if obj.wall == nil and obj.robot == nil then
        tScreen[x][y].robot = data
    else
        obj.wall = nil
        obj.robot = "zz"
        tScreen[x][y].robot = data
    end
end

local function addStart(x, y, side, color)
    local obj = tScreen[x][y]
    local data = side .. color
    if obj.wall == nil and obj.space == nil then
        tScreen[x][y].start = data
    else
        obj.wall = nil
        obj.space = nil
        tScreen[x][y].start = data
    end
    aExits = aExits + 1
end

local function addGround(x, y)
    local obj = tScreen[x][y]
    if obj.space == nil and obj.exit == nil and obj.wall == nil and obj.robot == nil and obj.start == nil then
        tScreen[x][y].ground = true
    else
        obj.space = nil
        obj.exit = "zz"
        obj.wall = nil
        obj.robot = "zz"
        obj.start = "zz"
        tScreen[x][y].ground = true
    end
end

local function addExit(x, y, cl)
    local obj = tScreen[x][y]
    if obj.space == nil and obj.ground == nil and obj.wall == nil and obj.robot == nil and obj.start == nil then
        tScreen[x][y].exit = cl
    else
        obj.space = nil
        obj.ground = nil
        obj.wall = nil
        obj.robot = "zz"
        obj.start = "zz"
        tScreen[x][y].exit = cl
    end
end

local function addWall(x, y)
    local obj = tScreen[x][y]
    if obj == nil then
        return error("Here X" .. x .. " Y" .. y)
    end
    if obj.space == nil and obj.exit == nil and obj.ground == nil and obj.robot == nil and obj.start == nil then
        tScreen[x][y].wall = true
    else
        obj.space = nil
        obj.exit = nil
        obj.ground = nil
        obj.robot = nil
        obj.start = nil
        tScreen[x][y].wall = true
    end
end

local function loadLevel(nNum)
    sLevelTitle = "Level " .. nNum
    if nNum == nil then return error("nNum == nil") end
    local sDir = fs.getDir(shell.getRunningProgram())
    local sLevelD = sDir .. "/levels/" .. tostring(nNum) .. ".dat"
    if not (fs.exists(sLevelD) or fs.isDir(sLevelD)) then return error("Level Not Exists : " .. sLevelD) end
    fLevel = fs.open(sLevelD, "r")
    local wl = true
    Blocks = tonumber(string.sub(fLevel.readLine(), 1, 1))
    local xSize = #fLevel.readLine() + 2
    local Lines = 3
    while wl do
        local wLine = fLevel.readLine()
        if wLine == nil then
            fLevel.close()
            wl = false
        else
            xSize = math.max(#wLine + 2, xSize)
            Lines = Lines + 1
        end
    end
    SizeW, SizeH = xSize, Lines
    reMap()
    fLevel = fs.open(sLevelD, "r")
    fLevel.readLine()
    for Line = 2, Lines - 1 do
        local sLine = fLevel.readLine()
        local chars = #sLine
        for char = 1, chars do
            local el = string.sub(sLine, char, char)
            if el == "8" then
                addGround(char + 1, Line)
            elseif el == "0" then
                addStart(char + 1, Line, "a", "a")
            elseif el == "1" then
                addStart(char + 1, Line, "b", "a")
            elseif el == "2" then
                addStart(char + 1, Line, "c", "a")
            elseif el == "3" then
                addStart(char + 1, Line, "d", "a")
            elseif el == "4" then
                addStart(char + 1, Line, "a", "b")
            elseif el == "5" then
                addStart(char + 1, Line, "b", "b")
            elseif el == "6" then
                addStart(char + 1, Line, "c", "b")
            elseif el == "9" then
                addStart(char + 1, Line, "d", "b")
            elseif el == "b" then
                addExit(char + 1, Line, "a")
            elseif el == "e" then
                addExit(char + 1, Line, "b")
            elseif el == "7" then
                addWall(char + 1, Line)
            end
        end
    end
    fLevel.close()
end

local function drawStars()
    --CCR Background By : RamiLego--
    local cStar, cStarG, crStar, crStarB = colors.lightGray, colors.gray, ".", "*"
    local DStar, BStar, nStar, gStar = 14, 10, 16, 3
    local TermW, TermH = term.getSize()

    term.clear()
    term.setCursorPos(1, 1)
    for x = 1, TermW do
        for y = 1, TermH do
            local StarT = math.random(1, 30)
            if StarT == DStar then
                term.setCursorPos(x, y)
                term.setTextColor(cStar)
                write(crStar)
            elseif StarT == BStar then
                term.setCursorPos(x, y)
                term.setTextColor(cStar)
                write(crStarB)
            elseif StarT == nStar then
                term.setCursorPos(x, y)
                term.setTextColor(cStarG)
                write(crStar)
            elseif StarT == gStar then
                term.setCursorPos(x, y)
                term.setTextColor(cStarG)
                write(crStarB)
            end
        end
    end
end

local function drawMap()
    for x = 1, SizeW do
        for y = 1, SizeH do

            local obj = tScreen[x][y]
            if obj.ground == true then
                paintutils.drawPixel(XOrgin + x, YOrgin + y + 1, cG)
            end
            if obj.wall == true then
                paintutils.drawPixel(XOrgin + x, YOrgin + y + 1, cW)
            end

         local ex = tostring(tScreen[x][y].exit)
            if not(ex == "zz" or ex == "nil") then
                if ex == "a" then
                    ex = cR1
                elseif ex == "b" then
                    ex = cR2
                elseif ex == "c" then
                    ex = cR3
                elseif ex == "d" then
                    ex = cR4
                else
                    return error("Exit Color Out")
                end
                term.setBackgroundColor(cG)
                term.setTextColor(ex)
                term.setCursorPos(XOrgin + x, YOrgin + y + 1)
                print("X")
            end

         local st = tostring(tScreen[x][y].start)
            if not(st == "zz" or st == "nil") then
                local Cr = string.sub(st, 2, 2)
                if Cr == "a" then
                    Cr = cR1
                elseif Cr == "b" then
                    Cr = cR2
                elseif Cr == "c" then
                    Cr = cR3
                elseif Cr == "d" then
                    Cr = cR4
                else
                    return error("Start Color Out")
                end

                term.setTextColor(Cr)
            term.setBackgroundColor(cG)
                term.setCursorPos(XOrgin + x, YOrgin + y + 1)

                local sSide = string.sub(st, 1, 1)
                if sSide == "a" then
                    print("^")
                elseif sSide == "b" then
                    print(">")
                elseif sSide == "c" then
                    print("v")
                elseif sSide == "d" then
                    print("<")
                else
                    print("@")
                end
            end

            if obj.space == true then
                paintutils.drawPixel(XOrgin + x, YOrgin + y + 1, cS)
            end

            local rb = tostring(tScreen[x][y].robot)
            if not(rb == "zz" or rb == "nil") then
                local Cr = string.sub(rb, 2, 2)
                if Cr == "a" then
                    Cr = cR1
                elseif Cr == "b" then
                    Cr = cR2
                elseif Cr == "c" then
                    Cr = cR3
                elseif Cr == "d" then
                    Cr = cR4
                else
                    Cr = colors.white
                end
                term.setBackgroundColor(Cr)
                term.setTextColor(colors.white)
                term.setCursorPos(XOrgin + x, YOrgin + y + 1)
                local sSide = string.sub(rb, 1, 1)
                if sSide == "a" then
                    print("^")
                elseif sSide == "b" then
                    print(">")
                elseif sSide == "c" then
                    print("v")
                elseif sSide == "d" then
                    print("<")
                else
                    print("@")
                end
            end
        end
    end
end

local function isBrick(x, y)
    local brb = tostring(tScreen[x][y].robot)
    local bobj = oScreen[x][y]
    if (brb == "zz" or brb == "nil") and not bobj.wall == true then
        return false
    else
        return true
    end
end

local function gRender(sContext)
    if sContext == "start" then
        for x = 1, SizeW do
            for y = 1, SizeH do
                local st = tostring(tScreen[x][y].start)
                if not(st == "zz" or st == "nil") then
                    local Cr = string.sub(st, 2, 2)
                    local sSide = string.sub(st, 1, 1)
                    addRobot(x, y, sSide, Cr)
                end
            end
        end
    elseif sContext == "tick" then
        buMap()
        for x = 1, SizeW do
            for y = 1, SizeH do
                local rb = tostring(oScreen[x][y].robot)
                if not(rb == "zz" or rb == "nil") then
                    local Cr = string.sub(rb, 2, 2)
                    local sSide = string.sub(rb, 1, 1)
                    local sobj = oScreen[x][y]
                    if sobj.space == true then
                        tScreen[x][y].robot = "zz"
                        if not sSide == "g" then
                            addRobot(x, y, "g", Cr)
                        end
                    elseif sobj.exit == Cr then
                        if sSide == "a" or sSide == "b" or sSide == "c" or sSide == "d" then
                        tScreen[x][y].robot = "zz"
                        addRobot(x, y, "g", Cr)
                        aExits = aExits - 1
                        end
                    elseif sSide == "a" then
                        local obj = isBrick(x, y - 1)
                        tScreen[x][y].robot = "zz"
                        if not obj == true then
                            addRobot(x, y - 1, sSide, Cr)
                        else
                            local obj2 = isBrick(x - 1, y)
                            local obj3 = isBrick(x + 1, y)
                            if not obj2 == true and not obj3 == true then
                                if Cr == "a" then
                                    addRobot(x, y, "d", Cr)
                                elseif Cr == "b" then
                                    addRobot(x, y, "b", Cr)
                                end
                            elseif obj == true and obj2 == true and obj3 == true then
                                addRobot(x, y, "c", Cr)
                            else
                                if obj3 == true then
                                    addRobot(x, y, "d", Cr)
                                elseif obj2 == true then
                                    addRobot(x, y, "b", Cr)
                                end
                            end
                        end
                    elseif sSide == "b" then
                        local obj = isBrick(x + 1, y)
                        tScreen[x][y].robot = "zz"
                        if not obj == true then
                            addRobot(x + 1, y, sSide, Cr)
                        else
                            local obj2 = isBrick(x, y - 1)
                            local obj3 = isBrick(x, y + 1)
                            if not obj2 == true and not obj3 == true then
                                if Cr == "a" then
                                    addRobot(x, y, "a", Cr)
                                elseif Cr == "b" then
                                    addRobot(x, y, "c", Cr)
                                end
                            elseif obj == true and obj2 == true and obj3 == true then
                                addRobot(x, y, "d", Cr)
                            else
                                if obj3 == true then
                                    addRobot(x, y, "a", Cr)
                                elseif obj2 == true then
                                    addRobot(x, y, "c", Cr)
                                end
                            end
                        end
                    elseif sSide == "c" then
                        local obj = isBrick(x, y + 1)
                        tScreen[x][y].robot = "zz"
                        if not obj == true then
                            addRobot(x, y + 1, sSide, Cr)
                        else
                            local obj2 = isBrick(x - 1, y)
                            local obj3 = isBrick(x + 1, y)
                            if not obj2 == true and not obj3 == true then
                                if Cr == "a" then
                                    addRobot(x, y, "b", Cr)
                                elseif Cr == "b" then
                                    addRobot(x, y, "d", Cr)
                                end
                            elseif obj == true and obj2 == true and obj3 == true then
                                addRobot(x, y, "a", Cr)
                            else
                                if obj3 == true then
                                    addRobot(x, y, "d", Cr)
                                elseif obj2 == true then
                                    addRobot(x, y, "b", Cr)
                                end
                            end
                        end
                    elseif sSide == "d" then
                        local obj = isBrick(x - 1, y)
                        tScreen[x][y].robot = "zz"
                        if not obj == true then
                            addRobot(x - 1, y, sSide, Cr)
                        else
                            local obj2 = isBrick(x, y - 1)
                            local obj3 = isBrick(x, y + 1)
                            if not obj2 == true and not obj3 == true then
                                if Cr == "a" then
                                    addRobot(x, y, "c", Cr)
                                elseif Cr == "b" then
                                    addRobot(x, y, "a", Cr)
                                end
                            elseif obj == true and obj2 == true and obj3 == true then
                                addRobot(x, y, "b", Cr)
                            else
                                if obj3 == true then
                                    addRobot(x, y, "a", Cr)
                                elseif obj2 == true then
                                    addRobot(x, y, "c", Cr)
                                end
                            end
                        end
                    else
                        addRobot(x, y, sSide, "g")
                    end
                end
            end
        end
    end
end

function InterFace.drawBar()
    term.setBackgroundColor(colors.black)
    term.setTextColor(InterFace.cTitle)
    printCentred(1, "  " .. sLevelTitle .. "  ")

    term.setCursorPos(1, 1)
    term.setBackgroundColor(cW)
    write(" ")
    term.setBackgroundColor(colors.black)
    write(" x " .. tostring(Blocks) .. " ")

    term.setCursorPos(TermW - 8, TermH)
    term.setBackgroundColor(colors.black)
    term.setTextColour(InterFace.cSpeedD)
    write(" <<")
    if bPaused then
        term.setTextColour(InterFace.cSpeedA)
    else
        term.setTextColour(InterFace.cSpeedD)
    end
    write(" ||")
    if fSpeedS then
        term.setTextColour(InterFace.cSpeedA)
    else
        term.setTextColour(InterFace.cSpeedD)
    end
    write(" >>")

    term.setCursorPos(TermW - 1, 1)
    term.setBackgroundColor(colors.black)
    term.setTextColour(InterFace.cExit)
    write(" X")
    term.setBackgroundColor(colors.black)
end

function InterFace.render()
    local id, p1, p2, p3 = os.pullEvent()
    if id == "mouse_click" then
        if p3 == 1 and p2 == TermW then
            return "end"
        elseif p3 == TermH and p2 >= TermW - 7 and p2 <= TermW - 6 then
            return "retry"
        elseif p3 == TermH and p2 >= TermW - 4 and p2 <= TermW - 3 then
            bPaused = not bPaused
            fSpeedS = false
            Speed = bPaused and 0 or nSpeed
            if Speed > 0 then
                Tick = os.startTimer(Speed)
            else
                Tick = nil
            end
            InterFace.drawBar()
        elseif p3 == TermH and p2 >= TermW - 1 then
            bPaused = false
            fSpeedS = not fSpeedS
            Speed = fSpeedS and fSpeed or nSpeed
            Tick = os.startTimer(Speed)
            InterFace.drawBar()
        elseif p3 - 1 < YOrgin + SizeH + 1 and p3 - 1 > YOrgin and
               p2 < XOrgin + SizeW + 1 and p2 > XOrgin then
            local eobj = tScreen[p2 - XOrgin][p3 - YOrgin - 1]
            local erobj = tostring(tScreen[p2 - XOrgin][p3 - YOrgin - 1].robot)
            if (erobj == "zz" or erobj == "nil") and not eobj.wall == true and not eobj.space == true and Blocks > 0 then
                addWall(p2 - XOrgin, p3 - YOrgin - 1)
                Blocks = Blocks - 1
                InterFace.drawBar()
                drawMap()
            end
        end
    elseif id == "timer" and p1 == Tick then
        gRender("tick")
        drawMap()
        if Speed > 0 then
            Tick = os.startTimer(Speed)
        else
            Tick = nil
        end
    end
end

local function startG(LevelN)
    drawStars()
    loadLevel(LevelN)
    centerOrgin()
    drawMap()
    InterFace.drawBar()
    gRender("start")
    drawMap()

    local NExit = true
    if aExits == 0 then
        NExit = false
    end

    while true do
        local isExit = InterFace.render()
        if isExit == "end" then
            return nil
        elseif isExit == "retry" then
            return LevelN
        elseif fExit == "yes" then
            if fs.exists(fs.getDir(shell.getRunningProgram()) .. "/levels/" .. tostring(LevelN + 1) .. ".dat") then
                return LevelN + 1
            else
                return nil
            end
        end
        if aExits == 0 and NExit == true then
            fExit = "yes"
        end
    end
end

local ok, err = true, nil

--Menu--
local sStartLevel = tArgs[1]
if ok and not sStartLevel then
    ok, err = pcall(function()
        term.setTextColor(colors.white)
        term.setBackgroundColor(colors.black)
        term.clear()
        drawStars()
        term.setTextColor(colors.red)
        printCentred(TermH / 2 - 1, "  REDIRECTION  ")
        printCentred(TermH / 2 - 0, "  ComputerCraft Edition  ")
        term.setTextColor(colors.yellow)
        printCentred(TermH / 2 + 2, "  Click to Begin  ")
        os.pullEvent("mouse_click")
    end)
end

--Game--
if ok then
    ok, err = pcall(function()
        local nLevel
        if sStartLevel then
            nLevel = tonumber(sStartLevel)
        else
            nLevel = 1
        end
        while nLevel do
            reset()
            nLevel = startG(nLevel)
        end
    end)
end

--Upsell screen--
if ok then
    ok, err = pcall(function()
        term.setTextColor(colors.white)
        term.setBackgroundColor(colors.black)
        term.clear()
        drawStars()
        term.setTextColor(colors.red)
        if TermW >= 40 then
            printCentred(TermH / 2 - 1, "  Thank you for playing Redirection  ")
            printCentred(TermH / 2 - 0, "  ComputerCraft Edition  ")
            printCentred(TermH / 2 + 2, "  Check out the full game:  ")
            term.setTextColor(colors.yellow)
            printCentred(TermH / 2 + 3, "  http://www.redirectiongame.com  ")
        else
            printCentred(TermH / 2 - 2, "  Thank you for  ")
            printCentred(TermH / 2 - 1, "  playing Redirection  ")
            printCentred(TermH / 2 - 0, "  ComputerCraft Edition  ")
            printCentred(TermH / 2 + 2, "  Check out the full game:  ")
            term.setTextColor(colors.yellow)
            printCentred(TermH / 2 + 3, "  www.redirectiongame.com  ")
        end
        parallel.waitForAll(
            function() sleep(2) end,
            function() os.pullEvent("mouse_click") end
        )
    end)
end

--Clear and exit--
term.setCursorPos(1, 1)
term.setTextColor(colors.white)
term.setBackgroundColor(colors.black)
term.clear()
if not ok then
    if err == "Terminated" then
        print("Check out the full version of Redirection:")
        print("http://www.redirectiongame.com")
    else
        printError(err)
    end
end
