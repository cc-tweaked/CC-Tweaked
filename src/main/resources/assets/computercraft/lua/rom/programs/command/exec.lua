local translate = require("cc.translate").translate

local tArgs = { ... }
if not commands then
    printError(translate("cc.exec.requires_command_computer"))
    return
end
if #tArgs == 0 then
    printError(translate("cc.exec.usage"))
    return
end

local function printSuccess(text)
    if term.isColor() then
        term.setTextColor(colors.green)
    end
    print(text)
    term.setTextColor(colors.white)
end

local sCommand = string.lower(tArgs[1])
for n = 2, #tArgs do
    sCommand = sCommand .. " " .. tArgs[n]
end

local bResult, tOutput = commands.exec(sCommand)
if bResult then
    printSuccess(translate("cc.exec.success"))
    if #tOutput > 0 then
        for n = 1, #tOutput do
            print(tOutput[n])
        end
    end
else
    printError(translate("cc.exec.failed"))
    if #tOutput > 0 then
        for n = 1, #tOutput do
            print(tOutput[n])
        end
    end
end
