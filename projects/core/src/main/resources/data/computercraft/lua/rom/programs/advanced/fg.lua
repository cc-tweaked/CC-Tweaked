-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

if not shell.openTab then
    printError("Requires multishell")
    return
end

local tArgs = { ... }
if #tArgs > 0 then
    local nTask = shell.openTab(table.unpack(tArgs))
    if nTask then
        shell.switchTab(nTask)
    end
else
    local nTask = shell.openTab("shell")
    if nTask then
        shell.switchTab(nTask)
    end
end
