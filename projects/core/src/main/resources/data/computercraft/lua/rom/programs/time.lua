-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local nTime = os.time()
local nDay = os.day()
print("The time is " .. textutils.formatTime(nTime, false) .. " on Day " .. nDay)
