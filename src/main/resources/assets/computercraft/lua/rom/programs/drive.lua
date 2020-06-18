local translate = require("cc.translate").translate

local tArgs = { ... }

-- Get where a directory is mounted
local sPath = shell.dir()
if tArgs[1] ~= nil then
    sPath = shell.resolve(tArgs[1])
end

if fs.exists(sPath) then
    write(fs.getDrive(sPath) .. " (")
    local nSpace = fs.getFreeSpace(sPath)
    if nSpace >= 1000 * 1000 then
        print(translate("cc.drive.remaining.mb"):format(math.floor(nSpace / (100 * 1000)) / 10) .. ")")
    elseif nSpace >= 1000 then
        print(translate("cc.drive.remaining.kb"):format(math.floor(nSpace / 100) / 10) .. ")")
    else
        print(translate("cc.drive.remaining.b"):format(nSpace) .. ")")
    end
else
    print(translate("cc.drive.no_path"))
end
