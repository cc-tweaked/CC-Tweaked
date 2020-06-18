local translate = require("cc.translate").translate

local tArgs = { ... }

if #tArgs < 1 then
    print(translate("cc.mkdir.usage"))
    return
end

for _, v in ipairs(tArgs) do
    local sNewDir = shell.resolve(v)
    if fs.exists(sNewDir) and not fs.isDir(sNewDir) then
        printError(translate("cc.mkdir.destination_exists"):format(v))
    elseif fs.isReadOnly(sNewDir) then
        printError(translate("cc.mkdir.access_denied"):format(v))
    else
        fs.makeDir(sNewDir)
    end
end
