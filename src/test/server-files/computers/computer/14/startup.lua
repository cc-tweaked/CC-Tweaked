-- CraftOsTest.`Sends basic rednet messages`

rednet.open("top")
while true do
    local id, msg, protocol = rednet.receive()
    rednet.send(id, msg, protocol)
end
