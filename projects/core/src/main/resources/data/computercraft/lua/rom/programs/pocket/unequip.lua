-- SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

if not pocket then
    printError("Requires a Pocket Computer")
    return
end

if select('#', ...) > 1 then
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usage: " .. programName .. " <side>")
    return
end

local function unequip(fn)
    local ok, err = fn()
    if not ok then
        printError(err)
    else
        print("Item unequipped")
    end
end

local side = ... or "back"
if side == "back" then
    unequip(pocket.unequipBack)
elseif side == "bottom" then
    unequip(pocket.unequipBottom)
else
    printError("Unknown side. Expected 'back' or 'bottom'.")
    return
end
