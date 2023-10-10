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

if not fs.exists(sSource) then
    printError("No matching files")
    return
elseif fs.isDriveRoot(sSource) then
    printError("Can't rename mounts")
    return
elseif fs.isReadOnly(sSource) then
    printError("Source is read-only")
    return
elseif fs.exists(sDest) then
    printError("Destination exists")
    return
elseif fs.isReadOnly(sDest) then
    printError("Destination is read-only")
    return
end

fs.move(sSource, sDest)
