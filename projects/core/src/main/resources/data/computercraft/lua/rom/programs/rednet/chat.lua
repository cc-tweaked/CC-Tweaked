-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tArgs = { ... }

local function printUsage()
    local programName = arg[0] or fs.getName(shell.getRunningProgram())
    print("Usages:")
    print(programName .. " host <hostname>")
    print(programName .. " join <hostname> <nickname>")
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

-- Colours
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
    -- "chat host"
    -- Get hostname
    local sHostname = tArgs[2]
    if sHostname == nil then
        printUsage()
        return
    end

    -- Host server
    if not openModem() then
        return
    end
    rednet.host("chat", sHostname)
    print("0 users connected.")

    local tUsers = {}
    local nUsers = 0
    local function send(sText, nUserID)
        if nUserID then
            local tUser = tUsers[nUserID]
            if tUser then
                rednet.send(tUser.nID, {
                    sType = "text",
                    nUserID = nUserID,
                    sText = sText,
                }, "chat")
            end
        else
            for nUserID, tUser in pairs(tUsers) do
                rednet.send(tUser.nID, {
                    sType = "text",
                    nUserID = nUserID,
                    sText = sText,
                }, "chat")
            end
        end
    end

    -- Setup ping pong
    local tPingPongTimer = {}
    local function ping(nUserID)
        local tUser = tUsers[nUserID]
        rednet.send(tUser.nID, {
            sType = "ping to client",
            nUserID = nUserID,
        }, "chat")

        local timer = os.startTimer(15)
        tUser.bPingPonged = false
        tPingPongTimer[timer] = nUserID
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

    -- Handle messages
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
                local tCommands
                tCommands = {
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
                        for _, tUser in pairs(tUsers) do
                            sUsers = sUsers .. " " .. tUser.sUsername
                        end
                        send(sUsers, tUser.nUserID)
                    end,
                    ["help"] = function(tUser, sContent)
                        send("* Available commands:", tUser.nUserID)
                        local sCommands = "*"
                        for sCommand in pairs(tCommands) do
                            sCommands = sCommands .. " /" .. sCommand
                        end
                        send(sCommands .. " /logout", tUser.nUserID)
                    end,
                }

                local nSenderID, tMessage = rednet.receive("chat")
                if type(tMessage) == "table" then
                    if tMessage.sType == "login" then
                        -- Login from new client
                        local nUserID = tMessage.nUserID
                        local sUsername = tMessage.sUsername
                        if nUserID and sUsername then
                            tUsers[nUserID] = {
                                nID = nSenderID,
                                nUserID = nUserID,
                                sUsername = sUsername,
                            }
                            nUsers = nUsers + 1
                            printUsers()
                            send("* " .. sUsername .. " has joined the chat")
                            ping(nUserID)
                        end

                    else
                        -- Something else from existing client
                        local nUserID = tMessage.nUserID
                        local tUser = tUsers[nUserID]
                        if tUser and tUser.nID == nSenderID then
                            if tMessage.sType == "logout" then
                                send("* " .. tUser.sUsername .. " has left the chat")
                                tUsers[nUserID] = nil
                                nUsers = nUsers - 1
                                printUsers()

                            elseif tMessage.sType == "chat" then
                                local sMessage = tMessage.sText
                                if sMessage then
                                    local sCommand = string.match(sMessage, "^/([a-z]+)")
                                    if sCommand then
                                        local fnCommand = tCommands[sCommand]
                                        if fnCommand then
                                            local sContent = string.sub(sMessage, #sCommand + 3)
                                            fnCommand(tUser, sContent)
                                        else
                                            send("* Unrecognised command: /" .. sCommand, tUser.nUserID)
                                        end
                                    else
                                        send("<" .. tUser.sUsername .. "> " .. tMessage.sText)
                                    end
                                end

                            elseif tMessage.sType == "ping to server" then
                                rednet.send(tUser.nID, {
                                    sType = "pong to client",
                                    nUserID = nUserID,
                                }, "chat")

                            elseif tMessage.sType == "pong to server" then
                                tUser.bPingPonged = true

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

    -- Unhost server
    for nUserID, tUser in pairs(tUsers) do
        rednet.send(tUser.nID, {
            sType = "kick",
            nUserID = nUserID,
        }, "chat")
    end
    rednet.unhost("chat")
    closeModem()

elseif sCommand == "join" then
    -- "chat join"
    -- Get hostname and username
    local sHostname = tArgs[2]
    local sUsername = tArgs[3]
    if sHostname == nil or sUsername == nil then
        printUsage()
        return
    end

    -- Connect
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

    -- Login
    local nUserID = math.random(1, 2147483647)
    rednet.send(nHostID, {
        sType = "login",
        nUserID = nUserID,
        sUsername = sUsername,
    }, "chat")

    -- Setup ping pong
    local bPingPonged = true
    local pingPongTimer = os.startTimer(0)

    local function ping()
        rednet.send(nHostID, {
            sType = "ping to server",
            nUserID = nUserID,
        }, "chat")
        bPingPonged = false
        pingPongTimer = os.startTimer(15)
    end

    -- Handle messages
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
        local w = titleWindow.getSize()
        local sTitle = sUsername .. " on " .. sHostname
        titleWindow.setTextColour(highlightColour)
        titleWindow.setCursorPos(math.floor(w / 2 - #sTitle / 2), 1)
        titleWindow.clearLine()
        titleWindow.write(sTitle)
        promptWindow.restoreCursor()
    end

    local function printMessage(sMessage)
        term.redirect(historyWindow)
        print()
        if string.match(sMessage, "^%*") then
            -- Information
            term.setTextColour(highlightColour)
            write(sMessage)
            term.setTextColour(textColour)
        else
            -- Chat
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
                    local w, h = parentTerm.getSize()
                    titleWindow.reposition(1, 1, w, 1)
                    historyWindow.reposition(1, 2, w, h - 2)
                    promptWindow.reposition(1, h, w, 1)

                end
            end
        end,
        function()
            while true do
                local nSenderID, tMessage = rednet.receive("chat")
                if nSenderID == nHostID and type(tMessage) == "table" and tMessage.nUserID == nUserID then
                    if tMessage.sType == "text" then
                        local sText = tMessage.sText
                        if sText then
                            printMessage(sText)
                        end

                    elseif tMessage.sType == "ping to client" then
                        rednet.send(nSenderID, {
                            sType = "pong to server",
                            nUserID = nUserID,
                        }, "chat")

                    elseif tMessage.sType == "pong to client" then
                        bPingPonged = true

                    elseif tMessage.sType == "kick" then
                        return

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
                    rednet.send(nHostID, {
                        sType = "chat",
                        nUserID = nUserID,
                        sText = sChat,
                    }, "chat")
                    table.insert(tSendHistory, sChat)
                end
            end
        end
    )

    -- Close the windows
    term.redirect(parentTerm)

    -- Print error notice
    local _, h = term.getSize()
    term.setCursorPos(1, h)
    term.clearLine()
    term.setCursorBlink(false)
    if not ok then
        printError(error)
    end

    -- Logout
    rednet.send(nHostID, {
        sType = "logout",
        nUserID = nUserID,
    }, "chat")
    closeModem()

    -- Print disconnection notice
    print("Disconnected.")

else
    -- "chat somethingelse"
    printUsage()

end
