-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- Communicate with other computers by using [modems][`modem`]. [`rednet`]
provides a layer of abstraction on top of the main [`modem`] peripheral, making
it slightly easier to use.

## Basic usage
In order to send a message between two computers, each computer must have a
modem on one of its sides (or in the case of pocket computers and turtles, the
modem must be equipped as an upgrade). The two computers should then call
[`rednet.open`], which sets up the modems ready to send and receive messages.

Once rednet is opened, you can send messages using [`rednet.send`] and receive
them using [`rednet.receive`]. It's also possible to send a message to _every_
rednet-using computer using [`rednet.broadcast`].

> [Network security][!WARNING]
>
> While rednet provides a friendly way to send messages to specific computers, it
> doesn't provide any guarantees about security. Other computers could be
> listening in to your messages, or even pretending to send messages from other computers!
>
> If you're playing on a multi-player server (or at least one where you don't
> trust other players), it's worth encrypting or signing your rednet messages.

## Protocols and hostnames
Several rednet messages accept "protocol"s - simple string names describing what
a message is about. When sending messages using [`rednet.send`] and
[`rednet.broadcast`], you can optionally specify a protocol for the message. This
same protocol can then be given to [`rednet.receive`], to ignore all messages not
using this protocol.

It's also possible to look-up computers based on protocols, providing a basic
system for service discovery and [DNS]. A computer can advertise that it
supports a particular protocol with [`rednet.host`], also providing a friendly
"hostname". Other computers may then find all computers which support this
protocol using [`rednet.lookup`].

[DNS]: https://en.wikipedia.org/wiki/Domain_Name_System "Domain Name System"

@module rednet
@since 1.2
@see rednet_message Queued when a rednet message is received.
@see modem Rednet is built on top of the modem peripheral. Modems provide a more
bare-bones but flexible interface.
]]

local expect = dofile("rom/modules/main/cc/expect.lua").expect

--- The channel used by the Rednet API to [`broadcast`] messages.
CHANNEL_BROADCAST = 65535

--- The channel used by the Rednet API to repeat messages.
CHANNEL_REPEAT = 65533

--- The number of channels rednet reserves for computer IDs. Computers with IDs
-- greater or equal to this limit wrap around to 0.
MAX_ID_CHANNELS = 65500

local received_messages = {}
local hostnames = {}
local prune_received_timer

local function id_as_channel(id)
    return (id or os.getComputerID()) % MAX_ID_CHANNELS
end

--[[- Opens a modem with the given [`peripheral`] name, allowing it to send and
receive messages over rednet.

This will open the modem on two channels: one which has the same
[ID][`os.getComputerID`] as the computer, and another on
[the broadcast channel][`CHANNEL_BROADCAST`].

@tparam string modem The name of the modem to open.
@throws If there is no such modem with the given name
@usage Open rednet on the back of the computer, allowing you to send and receive
rednet messages using it.

    rednet.open("back")

@usage Open rednet on all attached modems. This abuses the "filter" argument to
[`peripheral.find`].

    peripheral.find("modem", rednet.open)
@see rednet.close
@see rednet.isOpen
]]
function open(modem)
    expect(1, modem, "string")
    if peripheral.getType(modem) ~= "modem" then
        error("No such modem: " .. modem, 2)
    end
    peripheral.call(modem, "open", id_as_channel())
    peripheral.call(modem, "open", CHANNEL_BROADCAST)
end

--- Close a modem with the given [`peripheral`] name, meaning it can no longer
-- send and receive rednet messages.
--
-- @tparam[opt] string modem The side the modem exists on. If not given, all
-- open modems will be closed.
-- @throws If there is no such modem with the given name
-- @see rednet.open
function close(modem)
    expect(1, modem, "string", "nil")
    if modem then
        -- Close a specific modem
        if peripheral.getType(modem) ~= "modem" then
            error("No such modem: " .. modem, 2)
        end
        peripheral.call(modem, "close", id_as_channel())
        peripheral.call(modem, "close", CHANNEL_BROADCAST)
    else
        -- Close all modems
        for _, modem in ipairs(peripheral.getNames()) do
            if isOpen(modem) then
                close(modem)
            end
        end
    end
end

--- Determine if rednet is currently open.
--
-- @tparam[opt] string modem Which modem to check. If not given, all connected
-- modems will be checked.
-- @treturn boolean If the given modem is open.
-- @since 1.31
-- @see rednet.open
function isOpen(modem)
    expect(1, modem, "string", "nil")
    if modem then
        -- Check if a specific modem is open
        if peripheral.getType(modem) == "modem" then
            return peripheral.call(modem, "isOpen", id_as_channel()) and peripheral.call(modem, "isOpen", CHANNEL_BROADCAST)
        end
    else
        -- Check if any modem is open
        for _, modem in ipairs(peripheral.getNames()) do
            if isOpen(modem) then
                return true
            end
        end
    end
    return false
end

--[[- Allows a computer or turtle with an attached modem to send a message
intended for a computer with a specific ID. At least one such modem must first
be [opened][`rednet.open`] before sending is possible.

Assuming the target was in range and also had a correctly opened modem, the
target computer may then use [`rednet.receive`] to collect the message.

@tparam number recipient The ID of the receiving computer.
@param message The message to send. Like with [`modem.transmit`], this can
contain any primitive type (numbers, booleans and strings) as well as
tables. Other types (like functions), as well as metatables, will not be
transmitted.
@tparam[opt] string protocol The "protocol" to send this message under. When
using [`rednet.receive`] one can filter to only receive messages sent under a
particular protocol.
@treturn boolean If this message was successfully sent (i.e. if rednet is
currently [open][`rednet.open`]). Note, this does not guarantee the message was
actually _received_.
@changed 1.6 Added protocol parameter.
@changed 1.82.0 Now returns whether the message was successfully sent.
@see rednet.receive
@usage Send a message to computer #2.

    rednet.send(2, "Hello from rednet!")
]]
function send(recipient, message, protocol)
    expect(1, recipient, "number")
    expect(3, protocol, "string", "nil")
    -- Generate a (probably) unique message ID
    -- We could do other things to guarantee uniqueness, but we really don't need to
    -- Store it to ensure we don't get our own messages back
    local message_id = math.random(1, 2147483647)
    received_messages[message_id] = os.clock() + 9.5
    if not prune_received_timer then prune_received_timer = os.startTimer(10) end

    -- Create the message
    local reply_channel = id_as_channel()
    local message_wrapper = {
        nMessageID = message_id,
        nRecipient = recipient,
        nSender = os.getComputerID(),
        message = message,
        sProtocol = protocol,
    }

    local sent = false
    if recipient == os.getComputerID() then
        -- Loopback to ourselves
        os.queueEvent("rednet_message", os.getComputerID(), message, protocol)
        sent = true
    else
        -- Send on all open modems, to the target and to repeaters
        if recipient ~= CHANNEL_BROADCAST then
            recipient = id_as_channel(recipient)
        end

        for _, modem in ipairs(peripheral.getNames()) do
            if isOpen(modem) then
                peripheral.call(modem, "transmit", recipient, reply_channel, message_wrapper)
                peripheral.call(modem, "transmit", CHANNEL_REPEAT, reply_channel, message_wrapper)
                sent = true
            end
        end
    end

    return sent
end

--[[- Broadcasts a string message over the predefined [`CHANNEL_BROADCAST`]
channel. The message will be received by every device listening to rednet.

@param message The message to send. This should not contain coroutines or
functions, as they will be converted to [`nil`].
@tparam[opt] string protocol The "protocol" to send this message under. When
using [`rednet.receive`] one can filter to only receive messages sent under a
particular protocol.
@see rednet.receive
@changed 1.6 Added protocol parameter.
@usage Broadcast the words "Hello, world!" to every computer using rednet.

    rednet.broadcast("Hello, world!")
]]
function broadcast(message, protocol)
    expect(2, protocol, "string", "nil")
    send(CHANNEL_BROADCAST, message, protocol)
end

--[[- Wait for a rednet message to be received, or until `nTimeout` seconds have
elapsed.

@tparam[opt] string protocol_filter The protocol the received message must be
sent with. If specified, any messages not sent under this protocol will be
discarded.
@tparam[opt] number timeout The number of seconds to wait if no message is
received.
@treturn[1] number The computer which sent this message
@return[1] The received message
@treturn[1] string|nil The protocol this message was sent under.
@treturn[2] nil If the timeout elapsed and no message was received.
@see rednet.broadcast
@see rednet.send
@changed 1.6 Added protocol filter parameter.
@usage Receive a rednet message.

    local id, message = rednet.receive()
    print(("Computer %d sent message %s"):format(id, message))

@usage Receive a message, stopping after 5 seconds if no message was received.

    local id, message = rednet.receive(nil, 5)
    if not id then
        printError("No message received")
    else
        print(("Computer %d sent message %s"):format(id, message))
    end

@usage Receive a message from computer #2.

    local id, message
    repeat
        id, message = rednet.receive()
    until id == 2

    print(message)
]]
function receive(protocol_filter, timeout)
    -- The parameters used to be ( nTimeout ), detect this case for backwards compatibility
    if type(protocol_filter) == "number" and timeout == nil then
        protocol_filter, timeout = nil, protocol_filter
    end
    expect(1, protocol_filter, "string", "nil")
    expect(2, timeout, "number", "nil")

    -- Start the timer
    local timer = nil
    local event_filter = nil
    if timeout then
        timer = os.startTimer(timeout)
        event_filter = nil
    else
        event_filter = "rednet_message"
    end

    -- Wait for events
    while true do
        local event, p1, p2, p3 = os.pullEvent(event_filter)
        if event == "rednet_message" then
            -- Return the first matching rednet_message
            local sender_id, message, protocol = p1, p2, p3
            if protocol_filter == nil or protocol == protocol_filter then
                if timer then os.cancelTimer(timer) end
                return sender_id, message, protocol
            end
        elseif event == "timer" then
            -- Return nil if we timeout
            if p1 == timer then
                return nil
            end
        end
    end
end

--[[- Register the system as "hosting" the desired protocol under the specified
name. If a rednet [lookup][`rednet.lookup`] is performed for that protocol (and
maybe name) on the same network, the registered system will automatically
respond via a background process, hence providing the system performing the
lookup with its ID number.

Multiple computers may not register themselves on the same network as having the
same names against the same protocols, and the title `localhost` is specifically
reserved. They may, however, share names as long as their hosted protocols are
different, or if they only join a given network after "registering" themselves
before doing so (eg while offline or part of a different network).

@tparam string protocol The protocol this computer provides.
@tparam string hostname The name this computer exposes for the given protocol.
@throws If trying to register a hostname which is reserved, or currently in use.
@see rednet.unhost
@see rednet.lookup
@since 1.6
]]
function host(protocol, hostname)
    expect(1, protocol, "string")
    expect(2, hostname, "string")
    if hostname == "localhost" then
        error("Reserved hostname", 2)
    end
    if hostnames[protocol] ~= hostname then
        if lookup(protocol, hostname) ~= nil then
            error("Hostname in use", 2)
        end
        hostnames[protocol] = hostname
    end
end

--- Stop [hosting][`rednet.host`] a specific protocol, meaning it will no longer
-- respond to [`rednet.lookup`] requests.
--
-- @tparam string protocol The protocol to unregister your self from.
-- @since 1.6
function unhost(protocol)
    expect(1, protocol, "string")
    hostnames[protocol] = nil
end

--[[- Search the local rednet network for systems [hosting][`rednet.host`] the
desired protocol and returns any computer IDs that respond as "registered"
against it.

If a hostname is specified, only one ID will be returned (assuming an exact
match is found).

@tparam string protocol The protocol to search for.
@tparam[opt] string hostname The hostname to search for.

@treturn[1] number... A list of computer IDs hosting the given protocol.
@treturn[2] number|nil The computer ID with the provided hostname and protocol,
or [`nil`] if none exists.
@since 1.6
@usage Find all computers which are hosting the `"chat"` protocol.

    local computers = {rednet.lookup("chat")}
    print(#computers .. " computers available to chat")
    for _, computer in pairs(computers) do
      print("Computer #" .. computer)
    end

@usage Find a computer hosting the `"chat"` protocol with a hostname of `"my_host"`.

    local id = rednet.lookup("chat", "my_host")
    if id then
      print("Found my_host at computer #" .. id)
    else
      printError("Cannot find my_host")
    end

]]
function lookup(protocol, hostname)
    expect(1, protocol, "string")
    expect(2, hostname, "string", "nil")

    -- Build list of host IDs
    local results = nil
    if hostname == nil then
        results = {}
    end

    -- Check localhost first
    if hostnames[protocol] then
        if hostname == nil then
            table.insert(results, os.getComputerID())
        elseif hostname == "localhost" or hostname == hostnames[protocol] then
            return os.getComputerID()
        end
    end

    if not isOpen() then
        if results then
            return table.unpack(results)
        end
        return nil
    end

    -- Broadcast a lookup packet
    broadcast({
        sType = "lookup",
        sProtocol = protocol,
        sHostname = hostname,
    }, "dns")

    -- Start a timer
    local timer = os.startTimer(2)

    -- Wait for events
    while true do
        local event, p1, p2, p3 = os.pullEvent()
        if event == "rednet_message" then
            -- Got a rednet message, check if it's the response to our request
            local sender_id, message, message_protocol = p1, p2, p3
            if message_protocol == "dns" and type(message) == "table" and message.sType == "lookup response" then
                if message.sProtocol == protocol then
                    if hostname == nil then
                        table.insert(results, sender_id)
                    elseif message.sHostname == hostname then
                        os.cancelTimer(timer)
                        return sender_id
                    end
                end
            end
        elseif event == "timer" and p1 == timer then
            -- Got a timer event, check it's the end of our timeout
            break
        end
    end

    os.cancelTimer(timer)

    if results then
        return table.unpack(results)
    end
    return nil
end

local started = false

--- Listen for modem messages and converts them into rednet messages, which may
-- then be [received][`receive`].
--
-- This is automatically started in the background on computer startup, and
-- should not be called manually.
function run()
    if started then
        error("rednet is already running", 2)
    end
    started = true

    while true do
        local event, p1, p2, p3, p4 = os.pullEventRaw()
        if event == "modem_message" then
            -- Got a modem message, process it and add it to the rednet event queue
            local modem, channel, reply_channel, message = p1, p2, p3, p4
            if channel == id_as_channel() or channel == CHANNEL_BROADCAST then
                if type(message) == "table" and type(message.nMessageID) == "number"
                    and message.nMessageID == message.nMessageID and not received_messages[message.nMessageID]
                    and (type(message.nSender) == "nil" or (type(message.nSender) == "number" and message.nSender == message.nSender))
                    and ((message.nRecipient and message.nRecipient == os.getComputerID()) or channel == CHANNEL_BROADCAST)
                    and isOpen(modem)
                then
                    received_messages[message.nMessageID] = os.clock() + 9.5
                    if not prune_received_timer then prune_received_timer = os.startTimer(10) end
                    os.queueEvent("rednet_message", message.nSender or reply_channel, message.message, message.sProtocol)
                end
            end

        elseif event == "rednet_message" then
            -- Got a rednet message (queued from above), respond to dns lookup
            local sender, message, protocol = p1, p2, p3
            if protocol == "dns" and type(message) == "table" and message.sType == "lookup" then
                local hostname = hostnames[message.sProtocol]
                if hostname ~= nil and (message.sHostname == nil or message.sHostname == hostname) then
                    send(sender, {
                        sType = "lookup response",
                        sHostname = hostname,
                        sProtocol = message.sProtocol,
                    }, "dns")
                end
            end

        elseif event == "timer" and p1 == prune_received_timer then
            -- Got a timer event, use it to prune the set of received messages
            prune_received_timer = nil
            local now, has_more = os.clock(), nil
            for message_id, deadline in pairs(received_messages) do
                if deadline <= now then received_messages[message_id] = nil
                else has_more = true end
            end
            prune_received_timer = has_more and os.startTimer(10)
        end
    end
end
