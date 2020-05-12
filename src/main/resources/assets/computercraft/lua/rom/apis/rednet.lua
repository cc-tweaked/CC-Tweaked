--- The Rednet API allows systems to communicate between each other without
-- using redstone. It serves as a wrapper for the modem API, offering ease of
-- functionality (particularly in regards to repeating signals) with some
-- expense of fine control.
--
-- In order to send and receive data, a modem (either wired, wireless, or ender)
-- is required. The data reaches any possible destinations immediately after
-- sending it, but is range limited.
--
-- Rednet also allows you to use a "protocol" - simple string names indicating
-- what messages are about. Receiving systems may filter messages according to
-- their protocols, thereby automatically ignoring incoming messages which don't
-- specify an identical string. It's also possible to @{rednet.lookup|lookup}
-- which systems in the area use certain protocols, hence making it easier to
-- determine where given messages should be sent in the first place.
--
-- @module rednet

local expect = dofile("rom/modules/main/cc/expect.lua").expect

--- The channel used by the Rednet API to @{broadcast} messages.
CHANNEL_BROADCAST = 65535

--- The channel used by the Rednet API to repeat messages.
CHANNEL_REPEAT = 65533

local tReceivedMessages = {}
local tReceivedMessageTimeouts = {}
local tHostnames = {}

--- Opens a modem with the given @{peripheral} name, allowing it to send and
-- receive messages over rednet.
--
-- This will open the modem on two channels: one which has the same
-- @{os.getComputerID|ID} as the computer, and another on
-- @{CHANNEL_BROADCAST|the broadcast channel}.
--
-- @tparam string modem The name of the modem to open.
-- @throws If there is no such modem with the given name
function open(modem)
    expect(1, modem, "string")
    if peripheral.getType(modem) ~= "modem" then
        error("No such modem: " .. modem, 2)
    end
    peripheral.call(modem, "open", os.getComputerID())
    peripheral.call(modem, "open", CHANNEL_BROADCAST)
end

--- Close a modem with the given @{peripheral} name, meaning it can no longer
-- send and receive rednet messages.
--
-- @tparam[opt] string modem The side the modem exists on. If not given, all
-- open modems will be closed.
-- @throws If there is no such modem with the given name
function close(modem)
    expect(1, modem, "string", "nil")
    if modem then
        -- Close a specific modem
        if peripheral.getType(modem) ~= "modem" then
            error("No such modem: " .. modem, 2)
        end
        peripheral.call(modem, "close", os.getComputerID())
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
function isOpen(modem)
    expect(1, modem, "string", "nil")
    if modem then
        -- Check if a specific modem is open
        if peripheral.getType(modem) == "modem" then
            return peripheral.call(modem, "isOpen", os.getComputerID()) and peripheral.call(modem, "isOpen", CHANNEL_BROADCAST)
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

--- Allows a computer or turtle with an attached modem to send a message
-- intended for a system with a specific ID. At least one such modem must first
-- be @{rednet.open|opened} before sending is possible.
--
-- Assuming the target was in range and also had a correctly opened modem, it
-- may then use @{rednet.receive} to collect the message.
--
-- @tparam number nRecipient The ID of the receiving computer.
-- @param message The message to send. This should not contain coroutines or
-- functions, as they will be converted to @{nil}.
-- @tparam[opt] string sProtocol The "protocol" to send this message under. When
-- using @{rednet.receive} one can filter to only receive messages sent under a
-- particular protocol.
-- @treturn boolean If this message was successfully sent (i.e. if rednet is
-- currently @{rednet.open|open}). Note, this does not guarantee the message was
-- actually _received_.
-- @see rednet.receive
function send(nRecipient, message, sProtocol)
    expect(1, nRecipient, "number")
    expect(3, sProtocol, "string", "nil")
    -- Generate a (probably) unique message ID
    -- We could do other things to guarantee uniqueness, but we really don't need to
    -- Store it to ensure we don't get our own messages back
    local nMessageID = math.random(1, 2147483647)
    tReceivedMessages[nMessageID] = true
    tReceivedMessageTimeouts[os.startTimer(30)] = nMessageID

    -- Create the message
    local nReplyChannel = os.getComputerID()
    local tMessage = {
        nMessageID = nMessageID,
        nRecipient = nRecipient,
        message = message,
        sProtocol = sProtocol,
    }

    local sent = false
    if nRecipient == os.getComputerID() then
        -- Loopback to ourselves
        os.queueEvent("rednet_message", nReplyChannel, message, sProtocol)
        sent = true
    else
        -- Send on all open modems, to the target and to repeaters
        for _, sModem in ipairs(peripheral.getNames()) do
            if isOpen(sModem) then
                peripheral.call(sModem, "transmit", nRecipient, nReplyChannel, tMessage)
                peripheral.call(sModem, "transmit", CHANNEL_REPEAT, nReplyChannel, tMessage)
                sent = true
            end
        end
    end

    return sent
end

--- Broadcasts a string message over the predefined @{CHANNEL_BROADCAST}
-- channel. The message will be received by every device listening to rednet.
--
-- @param message The message to send. This should not contain coroutines or
-- functions, as they will be converted to @{nil}.
-- @tparam[opt] string sProtocol The "protocol" to send this message under. When
-- using @{rednet.receive} one can filter to only receive messages sent under a
-- particular protocol.
-- @see rednet.receive
function broadcast(message, sProtocol)
    expect(2, sProtocol, "string", "nil")
    send(CHANNEL_BROADCAST, message, sProtocol)
end

--- Wait for a rednet message to be received, or until `nTimeout` seconds have
-- elapsed.
--
-- @tparam[opt] string sProtocolFilter The protocol the received message must be
-- sent with. If specified, any messages not sent under this protocol will be
-- discarded.
-- @tparam[opt] number nTimeout The number of seconds to wait if no message is
-- received.
-- @treturn[1] number The computer which sent this message
-- @return[1] The received message
-- @treturn[1] string|nil The protocol this message was sent under.
-- @treturn[2] nil If the timeout elapsed and no message was received.
-- @see rednet.broadcast
-- @see rednet.send
function receive(sProtocolFilter, nTimeout)
    -- The parameters used to be ( nTimeout ), detect this case for backwards compatibility
    if type(sProtocolFilter) == "number" and nTimeout == nil then
        sProtocolFilter, nTimeout = nil, sProtocolFilter
    end
    expect(1, sProtocolFilter, "string", "nil")
    expect(2, nTimeout, "number", "nil")

    -- Start the timer
    local timer = nil
    local sFilter = nil
    if nTimeout then
        timer = os.startTimer(nTimeout)
        sFilter = nil
    else
        sFilter = "rednet_message"
    end

    -- Wait for events
    while true do
        local sEvent, p1, p2, p3 = os.pullEvent(sFilter)
        if sEvent == "rednet_message" then
            -- Return the first matching rednet_message
            local nSenderID, message, sProtocol = p1, p2, p3
            if sProtocolFilter == nil or sProtocol == sProtocolFilter then
                return nSenderID, message, sProtocol
            end
        elseif sEvent == "timer" then
            -- Return nil if we timeout
            if p1 == timer then
                return nil
            end
        end
    end
end

--- Register the system as "hosting" the desired protocol under the specified
-- name. If a rednet @{rednet.lookup|lookup} is performed for that protocol (and
-- maybe name) on the same network, the registered system will automatically
-- respond via a background process, hence providing the system performing the
-- lookup with its ID number.
--
-- Multiple computers may not register themselves on the same network as having
-- the same names against the same protocols, and the title `localhost` is
-- specifically reserved. They may, however, share names as long as their hosted
-- protocols are different, or if they only join a given network after
-- "registering" themselves before doing so (eg while offline or part of a
-- different network).
--
-- @tparam string sProtocol The protocol this computer provides.
-- @tparam string sHostname The name this protocol exposes for the given protocol.
-- @throws If trying to register a hostname which is reserved, or currently in use.
-- @see rednet.unhost
-- @see rednet.lookup
function host(sProtocol, sHostname)
    expect(1, sProtocol, "string")
    expect(2, sHostname, "string")
    if sHostname == "localhost" then
        error("Reserved hostname", 2)
    end
    if tHostnames[sProtocol] ~= sHostname then
        if lookup(sProtocol, sHostname) ~= nil then
            error("Hostname in use", 2)
        end
        tHostnames[sProtocol] = sHostname
    end
end

--- Stop @{rednet.host|hosting} a specific protocol, meaning it will no longer
-- respond to @{rednet.lookup} requests.
--
-- @tparam string sProtocol The protocol to unregister your self from.
function unhost(sProtocol)
    expect(1, sProtocol, "string")
    tHostnames[sProtocol] = nil
end

--- Search the local rednet network for systems @{rednet.host|hosting} the
-- desired protocol and returns any computer IDs that respond as "registered"
-- against it.
--
-- If a hostname is specified, only one ID will be returned (assuming an exact
-- match is found).
--
-- @tparam string sProtocol The protocol to search for.
-- @tparam[opt] string sHostname The hostname to search for.
--
-- @treturn[1] { number }|nil A list of computer IDs hosting the given
-- protocol, or @{nil} if none exist.
-- @treturn[2] number|nil The computer ID with the provided hostname and protocol,
-- or @{nil} if none exists.
function lookup(sProtocol, sHostname)
    expect(1, sProtocol, "string")
    expect(2, sHostname, "string", "nil")

    -- Build list of host IDs
    local tResults = nil
    if sHostname == nil then
        tResults = {}
    end

    -- Check localhost first
    if tHostnames[sProtocol] then
        if sHostname == nil then
            table.insert(tResults, os.getComputerID())
        elseif sHostname == "localhost" or sHostname == tHostnames[sProtocol] then
            return os.getComputerID()
        end
    end

    if not isOpen() then
        if tResults then
            return table.unpack(tResults)
        end
        return nil
    end

    -- Broadcast a lookup packet
    broadcast({
        sType = "lookup",
        sProtocol = sProtocol,
        sHostname = sHostname,
    }, "dns")

    -- Start a timer
    local timer = os.startTimer(2)

    -- Wait for events
    while true do
        local event, p1, p2, p3 = os.pullEvent()
        if event == "rednet_message" then
            -- Got a rednet message, check if it's the response to our request
            local nSenderID, tMessage, sMessageProtocol = p1, p2, p3
            if sMessageProtocol == "dns" and type(tMessage) == "table" and tMessage.sType == "lookup response" then
                if tMessage.sProtocol == sProtocol then
                    if sHostname == nil then
                        table.insert(tResults, nSenderID)
                    elseif tMessage.sHostname == sHostname then
                        return nSenderID
                    end
                end
            end
        else
            -- Got a timer event, check it's the end of our timeout
            if p1 == timer then
                break
            end
        end
    end
    if tResults then
        return table.unpack(tResults)
    end
    return nil
end

local bRunning = false

--- Listen for modem messages and converts them into rednet messages, which may
-- then be @{receive|received}.
--
-- This is automatically started in the background on computer startup, and
-- should not be called manually.
function run()
    if bRunning then
        error("rednet is already running", 2)
    end
    bRunning = true

    while bRunning do
        local sEvent, p1, p2, p3, p4 = os.pullEventRaw()
        if sEvent == "modem_message" then
            -- Got a modem message, process it and add it to the rednet event queue
            local sModem, nChannel, nReplyChannel, tMessage = p1, p2, p3, p4
            if isOpen(sModem) and (nChannel == os.getComputerID() or nChannel == CHANNEL_BROADCAST) then
                if type(tMessage) == "table" and tMessage.nMessageID then
                    if not tReceivedMessages[tMessage.nMessageID] then
                        tReceivedMessages[tMessage.nMessageID] = true
                        tReceivedMessageTimeouts[os.startTimer(30)] = tMessage.nMessageID
                        os.queueEvent("rednet_message", nReplyChannel, tMessage.message, tMessage.sProtocol)
                    end
                end
            end

        elseif sEvent == "rednet_message" then
            -- Got a rednet message (queued from above), respond to dns lookup
            local nSenderID, tMessage, sProtocol = p1, p2, p3
            if sProtocol == "dns" and type(tMessage) == "table" and tMessage.sType == "lookup" then
                local sHostname = tHostnames[tMessage.sProtocol]
                if sHostname ~= nil and (tMessage.sHostname == nil or tMessage.sHostname == sHostname) then
                    rednet.send(nSenderID, {
                        sType = "lookup response",
                        sHostname = sHostname,
                        sProtocol = tMessage.sProtocol,
                    }, "dns")
                end
            end

        elseif sEvent == "timer" then
            -- Got a timer event, use it to clear the event queue
            local nTimer = p1
            local nMessage = tReceivedMessageTimeouts[nTimer]
            if nMessage then
                tReceivedMessageTimeouts[nTimer] = nil
                tReceivedMessages[nMessage] = nil
            end
        end
    end
end
