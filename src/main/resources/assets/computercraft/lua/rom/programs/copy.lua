local translate = require("cc.translate").translate

local tArgs = { ... }
if #tArgs < 2 then
    print(translate("cc.copy.usage"))
    return
end

local sSource = shell.resolve(tArgs[1])
local sDest = shell.resolve(tArgs[2])
local tFiles = fs.find(sSource)
if #tFiles > 0 then
    for _, sFile in ipairs(tFiles) do
        if fs.isDir(sDest) then
            fs.copy(sFile, fs.combine(sDest, fs.getName(sFile)))
        elseif #tFiles == 1 then
            if fs.exists(sDest) then
                 printError(translate("cc.copy.exists"))
            elseif fs.isReadOnly(sDest) then
                printError(translate("cc.copy.read_only"))
            elseif fs.getFreeSpace(sDest) < fs.getSize(sFile) then
                printError(translate("cc.copy.space"))
            else
                 fs.copy(sFile, sDest)
            end
        else
            printError(translate("cc.copy.overwrite_files"))
            return
        end
    end
else
    printError(translate("cc.copy.no_matching"))
end
