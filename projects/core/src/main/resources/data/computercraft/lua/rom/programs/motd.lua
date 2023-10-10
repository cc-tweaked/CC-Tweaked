-- SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local date = os.date("*t")
if date.month == 1 and date.day == 1 then
    print("Happy new year!")
elseif date.month == 12 and date.day == 24 then
    print("Merry X-mas!")
elseif date.month == 10 and date.day == 31 then
    print("OOoooOOOoooo! Spooky!")
elseif date.month == 4 and date.day == 28 then
    print("Ed Balls")
else
    local tMotd = {}

    for sPath in string.gmatch(settings.get("motd.path"), "[^:]+") do
        if fs.exists(sPath) then
            for sLine in io.lines(sPath) do
                table.insert(tMotd, sLine)
            end
        end
    end

    if #tMotd == 0 then
        print("missingno")
    else
        print(tMotd[math.random(1, #tMotd)])
    end
end
