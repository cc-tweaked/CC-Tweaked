-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

-- Display the start screen
local w, h = term.getSize()

local headingColour, textColour, wormColour, fruitColour
if term.isColour() then
    headingColour = colours.yellow
    textColour = colours.white
    wormColour = colours.green
    fruitColour = colours.red
else
    headingColour = colours.white
    textColour = colours.white
    wormColour = colours.white
    fruitColour = colours.white
end

local function printCentred(y, s)
    local x = math.floor((w - #s) / 2)
    term.setCursorPos(x, y)
    --term.clearLine()
    term.write(s)
end

local xVel, yVel = 1, 0
local xPos, yPos = math.floor(w / 2), math.floor(h / 2)
local pxVel, pyVel = nil, nil
local nExtraLength = 6
local bRunning = true

local tailX, tailY = xPos, yPos
local nScore = 0
local nDifficulty = 2
local nSpeed, nInterval

-- Setup the screen
local screen = {}
for x = 1, w do
    screen[x] = {}
    for y = 1, h do
        screen[x][y] = {}
    end
end
screen[xPos][yPos] = { snake = true }

local nFruit = 1
local tFruits = {
    "A", "B", "C", "D", "E", "F", "G", "H",
    "I", "J", "K", "L", "M", "N", "O", "P",
    "Q", "R", "S", "T", "U", "V", "W", "X",
    "Y", "Z",
    "a", "b", "c", "d", "e", "f", "g", "h",
    "i", "j", "k", "l", "m", "n", "o", "p",
    "q", "r", "s", "t", "u", "v", "w", "x",
    "y", "z",
    "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
    "@", "$", "%", "#", "&", "!", "?", "+", "*", "~",
}

local function addFruit()
    while true do
        local x = math.random(1, w)
        local y = math.random(2, h)
        local fruit = screen[x][y]
        if fruit.snake == nil and fruit.wall == nil and fruit.fruit == nil then
            screen[x][y] = { fruit = true }
            term.setCursorPos(x, y)
            term.setBackgroundColour(fruitColour)
            term.write(" ")
            term.setBackgroundColour(colours.black)
            break
        end
    end

    nFruit = nFruit + 1
    if nFruit > #tFruits then
        nFruit = 1
    end
end

local function drawMenu()
    term.setTextColour(headingColour)
    term.setCursorPos(1, 1)
    term.write("SCORE ")

    term.setTextColour(textColour)
    term.setCursorPos(7, 1)
    term.write(tostring(nScore))

    term.setTextColour(headingColour)
    term.setCursorPos(w - 11, 1)
    term.write("DIFFICULTY ")

    term.setTextColour(textColour)
    term.setCursorPos(w, 1)
    term.write(tostring(nDifficulty or "?"))

    term.setTextColour(colours.white)
end

local function update( )
    if pxVel and pyVel then
        xVel, yVel = pxVel, pyVel
        pxVel, pyVel = nil, nil
    end

    -- Remove the tail
    if nExtraLength == 0 then
        local tail = screen[tailX][tailY]
        screen[tailX][tailY] = {}
        term.setCursorPos(tailX, tailY)
        term.write(" ")
        tailX = tail.nextX
        tailY = tail.nextY
    else
        nExtraLength = nExtraLength - 1
    end

    -- Update the head
    local head = screen[xPos][yPos]
    local newXPos = xPos + xVel
    local newYPos = yPos + yVel
    if newXPos < 1 then
        newXPos = w
    elseif newXPos > w then
        newXPos = 1
    end
    if newYPos < 2 then
        newYPos = h
    elseif newYPos > h then
        newYPos = 2
    end

    local newHead = screen[newXPos][newYPos]
    if newHead.snake == true or newHead.wall == true then
        bRunning = false

    else
        if newHead.fruit == true then
            nScore = nScore + 10
            nExtraLength = nExtraLength + 1
            addFruit()
        end
        xPos = newXPos
        yPos = newYPos
        head.nextX = newXPos
        head.nextY = newYPos
        screen[newXPos][newYPos] = { snake = true }

    end

    term.setCursorPos(xPos, yPos)
    term.setBackgroundColour(wormColour)
    term.write(" ")
    term.setBackgroundColour(colours.black)

    drawMenu()
end

-- Display the frontend
term.clear()
local function drawFrontend()
    --term.setTextColour( titleColour )
    --printCentred( math.floor(h/2) - 4, " W O R M " )

    term.setTextColour(headingColour)
    printCentred(math.floor(h / 2) - 3, "")
    printCentred(math.floor(h / 2) - 2, " SELECT DIFFICULTY ")
    printCentred(math.floor(h / 2) - 1, "")

    printCentred(math.floor(h / 2) + 0, "            ")
    printCentred(math.floor(h / 2) + 1, "            ")
    printCentred(math.floor(h / 2) + 2, "            ")
    printCentred(math.floor(h / 2) - 1 + nDifficulty, " [        ] ")

    term.setTextColour(textColour)
    printCentred(math.floor(h / 2) + 0, "EASY")
    printCentred(math.floor(h / 2) + 1, "MEDIUM")
    printCentred(math.floor(h / 2) + 2, "HARD")
    printCentred(math.floor(h / 2) + 3, "")

    term.setTextColour(colours.white)
end

drawMenu()
drawFrontend()
while true do
    local _, key = os.pullEvent("key")
    if key == keys.up or key == keys.w then
        -- Up
        if nDifficulty > 1 then
            nDifficulty = nDifficulty - 1
            drawMenu()
            drawFrontend()
        end
    elseif key == keys.down or key == keys.s then
        -- Down
        if nDifficulty < 3 then
            nDifficulty = nDifficulty + 1
            drawMenu()
            drawFrontend()
        end
    elseif key == keys.enter or key == keys.numPadEnter then
        -- Enter/Numpad Enter
        break
    end
end

local tSpeeds = { 5, 10, 25 }
nSpeed = tSpeeds[nDifficulty]
nInterval = 1 / nSpeed

-- Grow the snake to its intended size
term.clear()
drawMenu()
screen[tailX][tailY].snake = true
while nExtraLength > 0 do
    update()
end
addFruit()
addFruit()

-- Play the game
local timer = os.startTimer(0)
while bRunning do
    local event, p1 = os.pullEvent()
    if event == "timer" and p1 == timer then
        timer = os.startTimer(nInterval)
        update(false)

    elseif event == "key" then
        local key = p1
        if key == keys.up or key == keys.w then
            -- Up
            if yVel == 0 then
                pxVel, pyVel = 0, -1
            end
        elseif key == keys.down or key == keys.s then
            -- Down
            if yVel == 0 then
                pxVel, pyVel = 0, 1
            end
        elseif key == keys.left or key == keys.a then
            -- Left
            if xVel == 0 then
                pxVel, pyVel = -1, 0
            end

        elseif key == keys.right or key == keys.d then
            -- Right
            if xVel == 0 then
                pxVel, pyVel = 1, 0
            end

        end
    end
end

-- Display the gameover screen
term.setTextColour(headingColour)
printCentred(math.floor(h / 2) - 2, "                   ")
printCentred(math.floor(h / 2) - 1, " G A M E   O V E R ")

term.setTextColour(textColour)
printCentred(math.floor(h / 2) + 0, "                 ")
printCentred(math.floor(h / 2) + 1, " FINAL SCORE " .. nScore .. " ")
printCentred(math.floor(h / 2) + 2, "                 ")
term.setTextColour(colours.white)

local timer = os.startTimer(2.5)
repeat
    local e, p = os.pullEvent()
    if e == "timer" and p == timer then
        term.setTextColour(textColour)
        printCentred(math.floor(h / 2) + 2, " PRESS ANY KEY ")
        printCentred(math.floor(h / 2) + 3, "               ")
        term.setTextColour(colours.white)
    end
until e == "char"

term.clear()
term.setCursorPos(1, 1)
