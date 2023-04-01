-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

-- Get arguments
local tArgs = { ... }
if #tArgs == 0 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <drive>")
    return
end

local sDrive = tArgs[1]

-- Check the disk exists
local bPresent = disk.isPresent(sDrive)
if not bPresent then
    print("Nothing in " .. sDrive .. " drive")
    return
end

disk.eject(sDrive)
