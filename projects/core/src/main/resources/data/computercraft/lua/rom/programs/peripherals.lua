-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tPeripherals = peripheral.getNames()
print("Attached Peripherals:")
if #tPeripherals > 0 then
    for n = 1, #tPeripherals do
        local sPeripheral = tPeripherals[n]
        print(sPeripheral .. " (" .. table.concat({ peripheral.getType(sPeripheral) }, ", ") .. ")")
    end
else
    print("None")
end
