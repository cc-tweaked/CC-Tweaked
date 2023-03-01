-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

-- Find modems
local tModems = {}
for _, sModem in ipairs(peripheral.getNames()) do
    if peripheral.getType(sModem) == "modem" then
        table.insert(tModems, sModem)
    end
end
if #tModems == 0 then
    print("No modems found.")
    return
elseif #tModems == 1 then
    print("1 modem found.")
else
    print(#tModems .. " modems found.")
end

local function open(nChannel)
    for n = 1, #tModems do
        local sModem = tModems[n]
        peripheral.call(sModem, "open", nChannel)
    end
end

local function close(nChannel)
    for n = 1, #tModems do
        local sModem = tModems[n]
        peripheral.call(sModem, "close", nChannel)
    end
end

-- Open channels
print("0 messages repeated.")
open(rednet.CHANNEL_REPEAT)

-- Main loop (terminate to break)
local ok, error = pcall(function()
    local tReceivedMessages = {}
    local tReceivedMessageTimeouts = {}
    local nTransmittedMessages = 0

    while true do
        local sEvent, sModem, nChannel, nReplyChannel, tMessage = os.pullEvent()
        if sEvent == "modem_message" then
            -- Got a modem message, rebroadcast it if it's a rednet thing
            if nChannel == rednet.CHANNEL_REPEAT then
                if type(tMessage) == "table" and tMessage.nMessageID and tMessage.nRecipient and type(tMessage.nRecipient) == "number" then
                    if not tReceivedMessages[tMessage.nMessageID] then
                        -- Ensure we only repeat a message once
                        tReceivedMessages[tMessage.nMessageID] = true
                        tReceivedMessageTimeouts[os.startTimer(30)] = tMessage.nMessageID

                        local recipient_channel = tMessage.nRecipient
                        if tMessage.nRecipient ~= rednet.CHANNEL_BROADCAST then
                            recipient_channel = recipient_channel % rednet.MAX_ID_CHANNELS
                        end

                        -- Send on all other open modems, to the target and to other repeaters
                        for n = 1, #tModems do
                            local sOtherModem = tModems[n]
                            peripheral.call(sOtherModem, "transmit", rednet.CHANNEL_REPEAT, nReplyChannel, tMessage)
                            peripheral.call(sOtherModem, "transmit", recipient_channel, nReplyChannel, tMessage)
                        end

                        -- Log the event
                        nTransmittedMessages = nTransmittedMessages + 1
                        local _, y = term.getCursorPos()
                        term.setCursorPos(1, y - 1)
                        term.clearLine()
                        if nTransmittedMessages == 1 then
                            print(nTransmittedMessages .. " message repeated.")
                        else
                            print(nTransmittedMessages .. " messages repeated.")
                        end
                    end
                end
            end

        elseif sEvent == "timer" then
            -- Got a timer event, use it to clear the message history
            local nTimer = sModem
            local nMessageID = tReceivedMessageTimeouts[nTimer]
            if nMessageID then
                tReceivedMessageTimeouts[nTimer] = nil
                tReceivedMessages[nMessageID] = nil
            end

        end
    end
end)
if not ok then
    printError(error)
end

-- Close channels
close(rednet.CHANNEL_REPEAT)
