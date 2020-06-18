local translate = require("cc.translate").translate

local tPeripherals = peripheral.getNames()
print(translate("cc.peripherals.attached"))
if #tPeripherals > 0 then
    for n = 1, #tPeripherals do
        local sPeripheral = tPeripherals[n]
        print(sPeripheral .. " (" .. peripheral.getType(sPeripheral) .. ")")
    end
else
    print(translate("cc.peripherals.none"))
end
