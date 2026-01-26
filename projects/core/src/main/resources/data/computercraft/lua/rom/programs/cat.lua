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
    if fs.exists(sPath) then
        if fs.isDir(sPath) then
            printError(v .. ": Is a directory")
        else
            local file = fs.open(sPath, "r")
            print(file.readAll())
            file.close()
        end
    else
        printError(v .. ": No such file")
    end
end
