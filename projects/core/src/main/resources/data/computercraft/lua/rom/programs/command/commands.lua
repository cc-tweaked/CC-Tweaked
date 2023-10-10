-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

if not commands then
    printError("Requires a Command Computer.")
    return
end

local tCommands = commands.list()
table.sort(tCommands)

if term.isColor() then
    term.setTextColor(colors.green)
end
print("Available commands:")
term.setTextColor(colors.white)

textutils.pagedTabulate(tCommands)
