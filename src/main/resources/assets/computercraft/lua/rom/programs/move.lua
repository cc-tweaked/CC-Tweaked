
local tArgs = { ... }
if #tArgs < 2 then
    print("Usage: mv <source> <destination>")
    return
end

local sSource = shell.resolve(tArgs[1])
local sDest = shell.resolve(tArgs[2])
local tFiles = fs.find(sSource)

local function sanityChecks(sSource, sDestination)
    if fs.isMountPoint(sSource) then
        printError("Can't move mounts")
        return false
    elseif fs.isReadOnly(sSource) then
        printError("Source is read-only")
        return false
    elseif fs.exists(sDest) then
        printError("Destination exists")
        return false
    elseif fs.isReadOnly(sDest) then
        printError("Destination is read-only")
        return false
    end
    return true
end

if #tFiles > 0 then
    for _, sFile in ipairs(tFiles) do
        if fs.isDir(sDest) then
            if not sanityChecks(sFile, sDest) then return end
            fs.move(sFile, fs.combine(sDest, fs.getName(sFile)))
        elseif #tFiles == 1 then
            if not sanityChecks(sFile, sDest) then return end
            fs.move(sFile, sDest)
        else
            printError("Cannot overwrite file multiple times")
            return
        end
    end
else
    printError("No matching files")
end
