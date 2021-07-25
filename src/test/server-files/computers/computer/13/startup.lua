-- CraftOsTest.`Sends basic rednet messages`

rednet.open("top")

local id, msg
repeat
    rednet.send(14, "Test msg") -- Keep sending, as other computer may not have started yet.

    id, msg = rednet.receive(nil, 1)
    print(id, msg)
until id == 14

test.eq("Test msg", msg)
test.ok()
