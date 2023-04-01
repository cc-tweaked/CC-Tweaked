-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tArgs = { ... }
if #tArgs < 1 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <path>")
    return
end

local sPath = shell.resolve(tArgs[1])
if fs.exists(sPath) then
    if fs.isDir(sPath) then
        print("directory")
    else
        print("file")
    end
else
    print("No such path")
end
