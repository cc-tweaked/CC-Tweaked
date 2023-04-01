-- SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local label = os.getComputerLabel()
if label == nil then return test.fail("Label a computer to use it.") end

local fn, err = loadfile("tests/" .. label .. ".lua", nil, _ENV)
if not fn then return test.fail(err) end

local source = "@" .. label .. ".lua"
debug.sethook(function()
    local i = debug.getinfo(2, "lS")
    if i.source == source and i.currentline then
        test.log("At line " .. i.currentline)
    end
end, "l")

local ok, err = pcall(fn)
if not ok then return test.fail(err) end

print("Run " .. label)
test.ok()
