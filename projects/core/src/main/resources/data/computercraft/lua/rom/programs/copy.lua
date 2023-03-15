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
if #tFiles > 0 then
    for _, sFile in ipairs(tFiles) do
        if fs.isDir(sDest) then
            fs.copy(sFile, fs.combine(sDest, fs.getName(sFile)))
        elseif #tFiles == 1 then
            if fs.exists(sDest) then
                 printError("Destination exists")
            elseif fs.isReadOnly(sDest) then
                printError("Destination is read-only")
            elseif fs.getFreeSpace(sDest) < fs.getSize(sFile) then
                printError("Not enough space")
            else
                 fs.copy(sFile, sDest)
            end
        else
            printError("Cannot overwrite file multiple times")
            return
        end
    end
else
    printError("No matching files")
end
