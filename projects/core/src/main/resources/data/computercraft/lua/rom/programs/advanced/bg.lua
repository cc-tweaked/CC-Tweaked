-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

if not shell.openTab then
    printError("Requires multishell")
    return
end

local tArgs = { ... }
if #tArgs > 0 then
    shell.openTab(table.unpack(tArgs))
else
    shell.openTab("shell")
end
