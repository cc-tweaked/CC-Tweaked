local function check_peripherals(expected, msg)
    local peripherals = peripheral.getNames()
    table.sort(peripherals)

    test.eq(table.concat(expected, ", "), table.concat(peripherals, ", "), msg)
end

check_peripherals({
    "monitor_0",
    "printer_0",
    "right",
}, "Starts with peripherals")
