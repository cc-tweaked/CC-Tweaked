-- SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: LicenseRef-CCPL

if not pocket then
    printError("Requires a Pocket Computer")
    return
end

local ok, err = pocket.unequipBack()
if not ok then
    printError(err)
else
    print("Item unequipped")
end
