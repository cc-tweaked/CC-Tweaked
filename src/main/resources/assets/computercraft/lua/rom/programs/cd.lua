local translate = require("cc.translate").translate

local tArgs = { ... }
if #tArgs < 1 then
    print(translate("cc.cd.usage"))
    return
end

local sNewDir = shell.resolve(tArgs[1])
if fs.isDir(sNewDir) then
    shell.setDir(sNewDir)
else
    print(translate("cc.cd.no_directory"))
    return
end
