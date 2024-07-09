-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local pp = require "cc.pretty"

local tArgs = { ... }
if #tArgs == 0 then
    -- "set"
    for n, sName in ipairs(settings.getNames()) do
        pp.print(pp.text(sName, colors.cyan) .. " is " .. pp.pretty(settings.get(sName)))
    end
elseif #tArgs == 1 then
    -- "set foo"
    local sName = tArgs[1]
    local deets = settings.getDetails(sName)
    local msg = pp.text(sName, colors.cyan) .. " is " .. pp.pretty(deets.value)
    if deets.default ~= nil and deets.value ~= deets.default then
        msg = msg .. " (default is " .. pp.pretty(deets.default) .. ")"
    end
    pp.print(msg)
    if deets.description then print(deets.description) end
else
    -- "set foo bar"
    local sName = tArgs[1]
    local sValue = tArgs[2]
    local value
    if sValue == "true" then
        value = true
    elseif sValue == "false" then
        value = false
    elseif sValue == "nil" then
        value = nil
    elseif tonumber(sValue) then
        value = tonumber(sValue)
    else
        value = sValue
    end

    local option = settings.getDetails(sName)
    if value == nil then
        settings.unset(sName)
        pp.print(pp.text(sName, colors.cyan) .. " unset")
    elseif option.type and option.type ~= type(value) then
        printError(("%s is not a valid %s."):format(textutils.serialize(sValue), option.type))
    else
        settings.set(sName, value)
        pp.print(pp.text(sName, colors.cyan) .. " set to " .. pp.pretty(value))
    end

    if value ~= option.value then
        settings.save()
    end
end
