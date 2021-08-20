local modem = peripheral.find("modem")
while true do
    modem.transmit(12, 34, "Hello!")
    sleep(1)
end
