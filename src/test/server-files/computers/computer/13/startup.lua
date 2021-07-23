-- CraftOsTest.`Sends basic rednet messages`

rednet.open("top")

rednet.send(14, "Test msg")

local id, msg
repeat
    id, msg = rednet.receive()
    print(id, msg)
until id == 14

test.eq("Test msg", msg)
test.ok()
