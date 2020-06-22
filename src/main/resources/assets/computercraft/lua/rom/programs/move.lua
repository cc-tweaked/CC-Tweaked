local translate = require("cc.translate").translate

local tArgs = { ... }
if #tArgs < 2 then
    print(translate("cc.move.usage"))
    return
end

local sSource = shell.resolve(tArgs[1])
local sDest = shell.resolve(tArgs[2])
local tFiles = fs.find(sSource)

local function sanity_checks(source, dest)
    if fs.exists(dest) then
        printError(translate("cc.move.destination_exists"))
        return false
    elseif fs.isReadOnly(dest) then
        printError(translate("cc.move.destination_read_only"))
        return false
    elseif fs.isDriveRoot(source) then
        printError(translate("cc.move.source_mount"):format(source))
        return false
    elseif fs.isReadOnly(source) then
        printError(translate("cc.move.source_read_only"):format(source))
        return false
    end
    return true
end

if #tFiles > 0 then
    for _, sFile in ipairs(tFiles) do
        if fs.isDir(sDest) then
            local dest = fs.combine(sDest, fs.getName(sFile))
            if sanity_checks(sFile, dest) then
                fs.move(sFile, dest)
            end
        elseif #tFiles == 1 then
            if sanity_checks(sFile, sDest) then
                fs.move(sFile, sDest)
            end
        else
            printError(translate("cc.move.overwrite"))
            return
        end
    end
else
    printError(translate("cc.move.no_matching"))
end
