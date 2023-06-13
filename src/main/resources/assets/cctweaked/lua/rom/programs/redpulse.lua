-- Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tArgs = { ... }
local sSide = tArgs[1]
if not sSide then
	print("Usage: redpulse <side> <count> <period>")
	return
end

local nCount = tonumber(tArgs[2]) or 1
local nPeriod = tonumber(tArgs[3]) or 0.5

for n=1,nCount do
    redstone.setOutput(sSide, true)
    sleep(nPeriod / 2)
    redstone.setOutput(sSide, false)
    sleep(nPeriod / 2)
end
