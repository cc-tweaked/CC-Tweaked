
local tArgs = { ... }
if #tArgs < 2 then
    print("Usage: mv <source> <destination>")
    return
end

local sSource = shell.resolve(tArgs[1])
local sDest = shell.resolve(tArgs[2])
local tFiles = fs.find(sSource)

local function sanityChecks(sSource)
    if fs.isDriveRoot(sSource) then
        printError("Cannot move mount " .. sSource)
        return false
    elseif fs.isReadOnly(sSource) then
        printError("Cannot move read-only file " .. sSource)
        return false
    end
    return true
end

if fs.exists(sDest) then
    printError("Destination exists")
    return
elseif fs.isReadOnly(sDest) then
    printError("Destination is read-only")
    return
end

if #tFiles > 0 then
    for _, sFile in ipairs(tFiles) do
        if fs.isDir(sDest) then
            if sanityChecks(sFile) then
                fs.move(sFile, fs.combine(sDest, fs.getName(sFile)))
            end
        elseif #tFiles == 1 then
            if sanityChecks(sFile) then
                fs.move(sFile, sDest)
            end
        else
            printError("Cannot overwrite file multiple times")
            return
        end
    end
else
    printError("No matching files")
end
