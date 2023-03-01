-- SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

os.getComputerID = function() return 1 end
os.computerID = os.getComputerID

rednet.open("top")
while true do
    local id, msg, protocol = rednet.receive()
    rednet.send(id, msg, protocol)
end
