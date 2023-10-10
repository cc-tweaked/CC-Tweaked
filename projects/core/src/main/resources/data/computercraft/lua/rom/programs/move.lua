-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tArgs = { ... }
if #tArgs < 2 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <source> <destination>")
    return
end

local sSource = shell.resolve(tArgs[1])
local sDest = shell.resolve(tArgs[2])
local tFiles = fs.find(sSource)

local function sanity_checks(source, dest)
    if fs.exists(dest) then
        printError("Destination exists")
        return false
    elseif fs.isReadOnly(dest) then
        printError("Destination is read-only")
        return false
    elseif fs.isDriveRoot(source) then
        printError("Cannot move mount /" .. source)
        return false
    elseif fs.isReadOnly(source) then
        printError("Cannot move read-only file /" .. source)
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
            printError("Cannot overwrite file multiple times")
            return
        end
    end
else
    printError("No matching files")
end
