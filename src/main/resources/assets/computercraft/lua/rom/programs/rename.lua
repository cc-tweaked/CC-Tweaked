local translate = require("cc.translate").translate

local tArgs = { ... }
if #tArgs < 2 then
    print(translate("cc.rename.usage"))
    return
end

local sSource = shell.resolve(tArgs[1])
local sDest = shell.resolve(tArgs[2])

if not fs.exists(sSource) then
    printError(translate("cc.rename.matching"))
    return
elseif fs.isDriveRoot(sSource) then
    printError(translate("cc.rename.mount"))
    return
elseif fs.isReadOnly(sSource) then
    printError(translate("cc.rename.source_read_only"))
    return
elseif fs.exists(sDest) then
    printError(translate("cc.rename.exists"))
    return
elseif fs.isReadOnly(sDest) then
    printError(translate("cc.rename.destination_read_only"))
    return
end

fs.move(sSource, sDest)
