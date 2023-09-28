-- SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

-- Print out license information if needed
if fs.exists("data/example.dfpwm") then
    local h = io.open("data/example.dfpwm.license")
    local contents = h:read("*a")
    h:close()

    write(contents)
end

-- Make the startup file invisible, then run the file. We could use
-- shell.run, but this ensures the program is in shell history, etc...
fs.delete("startup.lua")
os.queueEvent("paste", "example.lua")
os.queueEvent("key", keys.enter, false)
