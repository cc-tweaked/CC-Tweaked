-- SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

rednet.open("top")

local id, msg
repeat
    rednet.send(1, "Test msg") -- Keep sending, as other computer may not have started yet.

    id, msg = rednet.receive(nil, 1)
until id == 1

test.eq("Test msg", msg)
