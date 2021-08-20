local function check_peripherals(expected, msg)
    local peripherals = peripheral.getNames()
    table.sort(peripherals)

    test.eq(table.concat(expected, ", "), table.concat(peripherals, ", "), msg)
end

check_peripherals({"back"}, "Has no peripherals on startup")
test.ok("initial")

os.pullEvent("peripheral")
sleep(0)

check_peripherals({
    "back",
    "monitor_1",
    "printer_1",
}, "Gains new peripherals")
