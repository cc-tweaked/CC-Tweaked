-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local bAll = false
local tArgs = { ... }
if #tArgs > 0 and tArgs[1] == "all" then
    bAll = true
end

local tPrograms = shell.programs(bAll)
textutils.pagedTabulate(tPrograms)
