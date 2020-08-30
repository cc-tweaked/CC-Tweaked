local tPeripherals = peripheral.getNames()
print("Attached Peripherals:")
if #tPeripherals > 0 then
    for n = 1, #tPeripherals do
        local sPeripheral = tPeripherals[n]
        print(sPeripheral .. " (" .. peripheral.getType(sPeripheral) .. ")")
    end
else
    print("None")
end
