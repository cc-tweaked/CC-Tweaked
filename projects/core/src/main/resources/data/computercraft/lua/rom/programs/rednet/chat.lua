-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tArgs = { ... }

local function printUsage()
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usages:")
    print(programName .. " host <hostname> [password]")
    print(programName .. " join <hostname> <nickname> [password]")
end

local sOpenedModem = nil
local function openModem()
    for _, sModem in ipairs(peripheral.getNames()) do
        if peripheral.getType(sModem) == "modem" then
            if not rednet.isOpen(sModem) then
                rednet.open(sModem)
                sOpenedModem = sModem
            end
            return true
        end
    end
    print("No modems found.")
    return false
end

local function closeModem()
    if sOpenedModem ~= nil then
        rednet.close(sOpenedModem)
        sOpenedModem = nil
    end
end

local highlightColour, textColour
if term.isColour() then
    textColour = colours.white
    highlightColour = colours.yellow
else
    textColour = colours.white
    highlightColour = colours.white
end

local sCommand = tArgs[1]
if sCommand == "host" then
    local sHostname = tArgs[2]
    local sPassword = tArgs[3]
    if sHostname == nil then
        printUsage()
        return
    end

    if not openModem() then
        return
    end
    rednet.host("chat", sHostname)

    local tUsers = {}
    local nUsers = 0

    local sServerSalt = nil
    local sDerivedKey = nil
    if sPassword then
        print("Password protected chat. Deriving key...")
        sServerSalt = crypto.randomBytes(16)
        sDerivedKey = crypto.pbkdf2(sPassword, sServerSalt)
        print("0 users connected.")
    else
        print("0 users connected.")
    end

    local function sendMessage(tUser, sType, tData)
        if tUser.sKey then
            local sMessage = textutils.serialize(tData)
            local sEncrypted = crypto.encrypt(tUser.sKey, sMessage)
            rednet.send(tUser.nID, {
                sType = sType,
                nUserID = tUser.nUserID,
                sData = sEncrypted,
            }, "chat")
        else
            rednet.send(tUser.nID, {
                sType = sType,
                nUserID = tUser.nUserID,
                tData = tData,
            }, "chat")
        end
    end

    local function send(sText, nUserID)
        if nUserID then
            local tUser = tUsers[nUserID]
            if tUser then
                sendMessage(tUser, "text", { sText = sText })
            end
        else
            for _, tUser in pairs(tUsers) do
                sendMessage(tUser, "text", { sText = sText })
            end
        end
    end

    local tPingPongTimer = {}
    local function ping(nUserID)
        local tUser = tUsers[nUserID]
        if tUser then
            sendMessage(tUser, "ping", {})
            local timer = os.startTimer(15)
            tUser.bPingPonged = false
            tPingPongTimer[timer] = nUserID
        end
    end

    local function printUsers()
        local _, y = term.getCursorPos()
        term.setCursorPos(1, y - 1)
        term.clearLine()
        if nUsers == 1 then
            print(nUsers .. " user connected.")
        else
            print(nUsers .. " users connected.")
        end
    end

    local ok, error = pcall(parallel.waitForAny,
        function()
            while true do
                local _, timer = os.pullEvent("timer")
                local nUserID = tPingPongTimer[timer]
                if nUserID and tUsers[nUserID] then
                    local tUser = tUsers[nUserID]
                    if tUser then
                        if not tUser.bPingPonged then
                            send("* " .. tUser.sUsername .. " has timed out")
                            tUsers[nUserID] = nil
                            nUsers = nUsers - 1
                            printUsers()
                        else
                            ping(nUserID)
                        end
                    end
                end
            end
        end,
        function()
            while true do
                local tCommands = {
                    ["me"] = function(tUser, sContent)
                        if #sContent > 0 then
                            send("* " .. tUser.sUsername .. " " .. sContent)
                        else
                            send("* Usage: /me [words]", tUser.nUserID)
                        end
                    end,
                    ["nick"] = function(tUser, sContent)
                        if #sContent > 0 then
                            local sOldName = tUser.sUsername
                            tUser.sUsername = sContent
                            send("* " .. sOldName .. " is now known as " .. tUser.sUsername)
                        else
                            send("* Usage: /nick [nickname]", tUser.nUserID)
                        end
                    end,
                    ["users"] = function(tUser, sContent)
                        send("* Connected Users:", tUser.nUserID)
                        local sUsers = "*"
                        for _, u in pairs(tUsers) do
                            sUsers = sUsers .. " " .. u.sUsername
                        end
                        send(sUsers, tUser.nUserID)
                    end,
                    ["help"] = function(tUser, sContent)
                        send("* Available commands:", tUser.nUserID)
                        local sCommands = "*"
                        for cmd in pairs(tCommands) do
                            sCommands = sCommands .. " /" .. cmd
                        end
                        send(sCommands .. " /logout", tUser.nUserID)
                    end,
                }

                local nSenderID, tMessage = rednet.receive("chat")
                if type(tMessage) == "table" then
                    if tMessage.sType == "login" then
                        local nUserID = tMessage.nUserID
                        local sUsername = tMessage.sUsername

                        if nUserID and sUsername then
                            local bLoginOK = true
                            local sUserKey = nil

                            if sPassword ~= nil then
                                local sChallenge = crypto.randomBytes(16)
                                rednet.send(nSenderID, {
                                    sType = "challenge",
                                    nUserID = nUserID,
                                    sSalt = sServerSalt,
                                    sChallenge = sChallenge,
                                }, "chat")

                                local startTime = os.clock()
                                local sClientResponse = nil
                                while os.clock() - startTime < 10 do
                                    local respSender, respMessage = rednet.receive("chat", 0.1)
                                    if respSender == nSenderID and type(respMessage) == "table" and respMessage.nUserID == nUserID and respMessage.sType == "challenge_response" then
                                        sClientResponse = respMessage.sResponse
                                        break
                                    end
                                end

                                local sExpectedResponse = crypto.hmacSha256(sDerivedKey, sChallenge)

                                if sClientResponse == nil then
                                    rednet.send(nSenderID, {
                                        sType = "login_response",
                                        nUserID = nUserID,
                                        bSuccess = false,
                                        sReason = "Login timeout",
                                    }, "chat")
                                    bLoginOK = false
                                elseif sClientResponse ~= sExpectedResponse then
                                    rednet.send(nSenderID, {
                                        sType = "login_response",
                                        nUserID = nUserID,
                                        bSuccess = false,
                                        sReason = "Invalid password",
                                    }, "chat")
                                    bLoginOK = false
                                else
                                    sUserKey = sDerivedKey
                                end
                            end

                            if bLoginOK then
                                tUsers[nUserID] = {
                                    nID = nSenderID,
                                    nUserID = nUserID,
                                    sUsername = sUsername,
                                    sKey = sUserKey,
                                    bPingPonged = true,
                                }
                                nUsers = nUsers + 1
                                printUsers()
                                send("* " .. sUsername .. " has joined the chat")
                                ping(nUserID)
                                rednet.send(nSenderID, {
                                    sType = "login_response",
                                    nUserID = nUserID,
                                    bSuccess = true,
                                }, "chat")
                            end
                        end

                    else
                        local nUserID = tMessage.nUserID
                        local tUser = tUsers[nUserID]
                        if tUser and tUser.nID == nSenderID then
                            local tData = nil

                            if tMessage.sData and tUser.sKey then
                                local ok, result = pcall(crypto.decrypt, tUser.sKey, tMessage.sData)
                                if ok then
                                    tData = textutils.unserialize(result)
                                end
                            elseif tMessage.tData then
                                tData = tMessage.tData
                            end

                            if type(tData) == "table" then
                                if tMessage.sType == "logout" then
                                    send("* " .. tUser.sUsername .. " has left the chat")
                                    tUsers[nUserID] = nil
                                    nUsers = nUsers - 1
                                    printUsers()

                                elseif tMessage.sType == "chat" then
                                    local sMsg = tData.sText
                                    if sMsg then
                                        local sCmd = string.match(sMsg, "^/([a-z]+)")
                                        if sCmd then
                                            local fnCmd = tCommands[sCmd]
                                            if fnCmd then
                                                local sContent = string.sub(sMsg, #sCmd + 3)
                                                fnCmd(tUser, sContent)
                                            else
                                                send("* Unrecognised command: /" .. sCmd, tUser.nUserID)
                                            end
                                        else
                                            send("<" .. tUser.sUsername .. "> " .. sMsg)
                                        end
                                    end

                                elseif tMessage.sType == "pong" then
                                    tUser.bPingPonged = true
                                end
                            end
                        end
                    end
                end
            end
        end
    )

    if not ok then
        printError(error)
    end

    for _, tUser in pairs(tUsers) do
        sendMessage(tUser, "kick", {})
    end
    rednet.unhost("chat")
    closeModem()

elseif sCommand == "join" then
    local sHostname = tArgs[2]
    local sUsername = tArgs[3]
    local sPassword = tArgs[4]
    if sHostname == nil or sUsername == nil then
        printUsage()
        return
    end

    if not openModem() then
        return
    end
    write("Looking up " .. sHostname .. "... ")
    local nHostID = rednet.lookup("chat", sHostname)
    if nHostID == nil then
        print("Failed.")
        return
    else
        print("Success.")
    end

    local nUserID = math.random(1, 2147483647)
    rednet.send(nHostID, {
        sType = "login",
        nUserID = nUserID,
        sUsername = sUsername,
    }, "chat")

    local sDerivedKey = nil
    local loginSuccess = false
    local timeout = os.startTimer(15)

    while true do
        local event, p1, p2 = os.pullEvent()
        if event == "timer" and p1 == timeout then
            print("Login timeout: no response from host.")
            closeModem()
            return
        elseif event == "rednet_message" then
            local senderID, tMessage = p1, p2
            if senderID == nHostID and type(tMessage) == "table" and tMessage.nUserID == nUserID then
                if tMessage.sType == "challenge" then
                    local sSalt = tMessage.sSalt
                    local sChallenge = tMessage.sChallenge
                    if sSalt and sChallenge then
                        write("Authenticating... ")
                        sDerivedKey = crypto.pbkdf2(sPassword or "", sSalt)
                        local sResponse = crypto.hmacSha256(sDerivedKey, sChallenge)
                        rednet.send(nHostID, {
                            sType = "challenge_response",
                            nUserID = nUserID,
                            sResponse = sResponse,
                        }, "chat")
                    end
                elseif tMessage.sType == "login_response" then
                    if tMessage.bSuccess then
                        if sDerivedKey then
                            print("Authenticated.")
                        end
                        loginSuccess = true
                        break
                    else
                        print("Login failed: " .. (tMessage.sReason or "Unknown reason"))
                        closeModem()
                        return
                    end
                end
            end
        end
    end

    if not loginSuccess then
        closeModem()
        return
    end

    local bPingPonged = true
    local pingPongTimer = os.startTimer(0)

    local function sendMessage(sType, tData)
        if sDerivedKey then
            local sMessage = textutils.serialize(tData)
            local sEncrypted = crypto.encrypt(sDerivedKey, sMessage)
            rednet.send(nHostID, {
                sType = sType,
                nUserID = nUserID,
                sData = sEncrypted,
            }, "chat")
        else
            rednet.send(nHostID, {
                sType = sType,
                nUserID = nUserID,
                tData = tData,
            }, "chat")
        end
    end

    local function ping()
        sendMessage("ping", {})
        bPingPonged = false
        pingPongTimer = os.startTimer(15)
    end

    local w, h = term.getSize()
    local parentTerm = term.current()
    local titleWindow = window.create(parentTerm, 1, 1, w, 1, true)
    local historyWindow = window.create(parentTerm, 1, 2, w, h - 2, true)
    local promptWindow = window.create(parentTerm, 1, h, w, 1, true)
    historyWindow.setCursorPos(1, h - 2)

    term.clear()
    term.setTextColour(textColour)
    term.redirect(promptWindow)
    promptWindow.restoreCursor()

    local function drawTitle()
        local tw = titleWindow.getSize()
        local sTitle = sUsername .. " on " .. sHostname
        titleWindow.setTextColour(highlightColour)
        titleWindow.setCursorPos(math.floor(tw / 2 - #sTitle / 2), 1)
        titleWindow.clearLine()
        titleWindow.write(sTitle)
        promptWindow.restoreCursor()
    end

    local function printMessage(sMessage)
        term.redirect(historyWindow)
        print()
        if string.match(sMessage, "^%*") then
            term.setTextColour(highlightColour)
            write(sMessage)
            term.setTextColour(textColour)
        else
            local sUsernameBit = string.match(sMessage, "^<[^>]*>")
            if sUsernameBit then
                term.setTextColour(highlightColour)
                write(sUsernameBit)
                term.setTextColour(textColour)
                write(string.sub(sMessage, #sUsernameBit + 1))
            else
                write(sMessage)
            end
        end
        term.redirect(promptWindow)
        promptWindow.restoreCursor()
    end

    drawTitle()

    local ok, error = pcall(parallel.waitForAny,
        function()
            while true do
                local sEvent, timer = os.pullEvent()
                if sEvent == "timer" then
                    if timer == pingPongTimer then
                        if not bPingPonged then
                            printMessage("Server timeout.")
                            return
                        else
                            ping()
                        end
                    end
                elseif sEvent == "term_resize" then
                    local nw, nh = parentTerm.getSize()
                    titleWindow.reposition(1, 1, nw, 1)
                    historyWindow.reposition(1, 2, nw, nh - 2)
                    promptWindow.reposition(1, nh, nw, 1)
                end
            end
        end,
        function()
            while true do
                local nSenderID, tMessage = rednet.receive("chat")
                if nSenderID == nHostID and type(tMessage) == "table" and tMessage.nUserID == nUserID then
                    local tData = nil

                    if tMessage.sData and sDerivedKey then
                        local ok, result = pcall(crypto.decrypt, sDerivedKey, tMessage.sData)
                        if ok then
                            tData = textutils.unserialize(result)
                        end
                    elseif tMessage.tData then
                        tData = tMessage.tData
                    end

                    if type(tData) == "table" then
                        if tMessage.sType == "text" and tData.sText then
                            printMessage(tData.sText)
                        elseif tMessage.sType == "ping" then
                            sendMessage("pong", {})
                        elseif tMessage.sType == "pong" then
                            bPingPonged = true
                        elseif tMessage.sType == "kick" then
                            return
                        end
                    end
                end
            end
        end,
        function()
            local tSendHistory = {}
            while true do
                promptWindow.setCursorPos(1, 1)
                promptWindow.clearLine()
                promptWindow.setTextColor(highlightColour)
                promptWindow.write(": ")
                promptWindow.setTextColor(textColour)

                local sChat = read(nil, tSendHistory)
                if string.match(sChat, "^/logout") then
                    break
                else
                    sendMessage("chat", { sText = sChat })
                    table.insert(tSendHistory, sChat)
                end
            end
        end
    )

    term.redirect(parentTerm)

    local _, th = term.getSize()
    term.setCursorPos(1, th)
    term.clearLine()
    term.setCursorBlink(false)
    if not ok then
        printError(error)
    end

    sendMessage("logout", {})
    closeModem()

    print("Disconnected.")

else
    printUsage()
end
