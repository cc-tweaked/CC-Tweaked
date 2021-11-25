local modem = peripheral.find("modem")
modem.open(12)


local _, name, chan, reply, payload, distance = os.pullEvent("modem_message")
test.eq("left", name, "Modem name")
test.eq(12, chan, "Channel")
test.eq(34, reply, "Reply channel")
test.eq("Hello!", payload, "Payload")
test.eq(4, distance, "Distance") -- Why 4?!
