-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

if not turtle then
    printError("Requires a Turtle")
end

local tMoves = {
    function()
        turtle.up()
        turtle.down()
    end,
    function()
        turtle.up()
        turtle.turnLeft()
        turtle.turnLeft()
        turtle.turnLeft()
        turtle.turnLeft()
        turtle.down()
    end,
    function()
        turtle.up()
        turtle.turnRight()
        turtle.turnRight()
        turtle.turnRight()
        turtle.turnRight()
        turtle.down()
    end,
    function()
        turtle.turnLeft()
        turtle.turnLeft()
        turtle.turnLeft()
        turtle.turnLeft()
    end,
    function()
        turtle.turnRight()
        turtle.turnRight()
        turtle.turnRight()
        turtle.turnRight()
    end,
    function()
        turtle.turnLeft()
        turtle.back()
        turtle.back()
        turtle.turnRight()
        turtle.turnRight()
        turtle.back()
        turtle.back()
        turtle.turnLeft()
    end,
    function()
        turtle.turnRight()
        turtle.back()
        turtle.back()
        turtle.turnLeft()
        turtle.turnLeft()
        turtle.back()
        turtle.back()
        turtle.turnRight()
    end,
    function()
        turtle.back()
        turtle.turnLeft()
        turtle.back()
        turtle.turnLeft()
        turtle.back()
        turtle.turnLeft()
        turtle.back()
        turtle.turnLeft()
    end,
    function()
        turtle.back()
        turtle.turnRight()
        turtle.back()
        turtle.turnRight()
        turtle.back()
        turtle.turnRight()
        turtle.back()
        turtle.turnRight()
    end,
}

textutils.slowWrite("Preparing to get down.")
textutils.slowPrint("..", 0.75)

local sAudio = nil
for _, sName in pairs(peripheral.getNames()) do
    if disk.hasAudio(sName) then
        disk.playAudio(sName)
        print("Jamming to " .. disk.getAudioTitle(sName))
        sAudio = sName
        break
    end
end

print("Press any key to stop the groove")

parallel.waitForAny(
    function() os.pullEvent("key") end,
    function()
        while true do
            tMoves[math.random(1, #tMoves)]()
        end
    end
)

if sAudio then
    disk.stopAudio(sAudio)
end
