-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--- @module turtle

if not turtle then
    error("Cannot load turtle API on computer", 2)
end

--- The builtin turtle API, without any generated helper functions.
--
-- @deprecated Historically this table behaved differently to the main turtle API, but this is no longer the case. You
-- should not need to use it.
native = turtle.native or turtle

local function waitForResponse(_id)
    local event, responseID, success
    while event ~= "turtle_response" or responseID ~= _id do
        event, responseID, success = os.pullEvent("turtle_response")
    end
    return success
end

local function wrap(_sCommand)
    return function(...)
        local id = native[_sCommand](...)
        if id == -1 then
            return false
        end
        return waitForResponse(id)
    end
end

-- Wrap standard commands
local turtle = {}
turtle["getItemCount"] = native.getItemCount
turtle["getItemSpace"] = native.getItemSpace
turtle["getFuelLevel"] = native.getFuelLevel
turtle["getSelectedSlot"] = native.getSelectedSlot
turtle["getFuelLimit"] = native.getFuelLimit

for k,v in pairs(native) do
    if type(k) == "string" and type(v) == "function" then
        if turtle[k] == nil then
            turtle[k] = wrap(k)
        end
    end
end

-- Wrap peripheral commands
if peripheral.getType("left") == "workbench" then
    turtle["craft"] = function(...)
        local id = peripheral.call("left", "craft", ...)
        return waitForResponse(id)
    end
elseif peripheral.getType("right") == "workbench" then
    turtle["craft"] = function(...)
        local id = peripheral.call("right", "craft", ...)
        return waitForResponse(id)
    end
end

-- Put commands into environment table
local env = _ENV
for k,v in pairs(turtle) do env[k] = v end
