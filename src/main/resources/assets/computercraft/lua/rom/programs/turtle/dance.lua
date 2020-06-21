local translate = require("cc.translate").translate

if not turtle then
    printError(translate("cc.dance.requires_turtle"))
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

textutils.slowWrite(translate("cc.dance.get_down"))
textutils.slowPrint("..", 0.75)

local sAudio = nil
for _, sName in pairs(peripheral.getNames()) do
    if disk.hasAudio(sName) then
        disk.playAudio(sName)
        print(translate("cc.dance.jamming"):format(disk.getAudioTitle(sName)))
        sAudio = sName
        break
    end
end

print(translate("cc.dance.press_key"))

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
