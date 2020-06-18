local translate = require("cc.translate").translate

local tArgs = { ... }
if #tArgs < 1 then
    print(translate("cc.type.usage"))
    return
end

local sPath = shell.resolve(tArgs[1])
if fs.exists(sPath) then
    if fs.isDir(sPath) then
        print(translate("cc.type.directory"))
    else
        print(translate("cc.type.file"))
    end
else
    print(translate("cc.type.path"))
end
