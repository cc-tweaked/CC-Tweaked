-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tArgs = { ... }

if #tArgs < 1 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <paths>")
    return
end

for _, v in ipairs(tArgs) do
    local sNewDir = shell.resolve(v)
    if fs.exists(sNewDir) and not fs.isDir(sNewDir) then
        printError(v .. ": Destination exists")
    elseif fs.isReadOnly(sNewDir) then
        printError(v .. ": Access denied")
    else
        fs.makeDir(sNewDir)
    end
end
