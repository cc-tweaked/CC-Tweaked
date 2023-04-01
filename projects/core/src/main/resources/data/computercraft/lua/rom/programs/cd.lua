-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tArgs = { ... }
if #tArgs < 1 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <path>")
    return
end

local sNewDir = shell.resolve(tArgs[1])
if fs.isDir(sNewDir) then
    shell.setDir(sNewDir)
else
    print("Not a directory")
    return
end
