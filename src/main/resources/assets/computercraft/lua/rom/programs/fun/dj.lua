local translate = require("cc.translate").translate

local tArgs = { ... }

local function printUsage()
    print(translate("cc.dj.usage_title"))
    print("dj play")
    print("dj play " .. translate("cc.dj.usage_drive"))
    print("dj stop")
end

if #tArgs > 2 then
    printUsage()
    return
end

local sCommand = tArgs[1]
if sCommand == "stop" then
    -- Stop audio
    disk.stopAudio()

elseif sCommand == "play" or sCommand == nil then
    -- Play audio
    local sName = tArgs[2]
    if sName == nil then
        -- No disc specified, pick one at random
        local tNames = {}
        for _, sName in ipairs(peripheral.getNames()) do
            if disk.isPresent(sName) and disk.hasAudio(sName) then
                table.insert(tNames, sName)
            end
        end
        if #tNames == 0 then
            print(translate("cc.dj.no_disk_any"))
            return
        end
        sName = tNames[math.random(1, #tNames)]
    end

    -- Play the disc
    if disk.isPresent(sName) and disk.hasAudio(sName) then
        print(translate("cc.dj.playing"):format(disk.getAudioTitle(sName)))
        disk.playAudio(sName)
    else
        print(translate("cc.dj.no_disk_specific"):format(sName))
        return
    end

else
    printUsage()

end
