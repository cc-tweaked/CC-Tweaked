-- SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local tArgs = { ... }

if #tArgs < 1 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <paths>")
    return
end

for _, v in ipairs(tArgs) do
    local sPath = shell.resolve(v)
    if fs.isDir(sPath) then
        printError(v .. ": Is a directory")
    elseif fs.isReadOnly(sPath) then
        printError(v .. ": Access denied")
    else
        local file = fs.open(sPath, "a")
        file.close()
    end
end
