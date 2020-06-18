local translate = require("cc.translate").translate
local pp = require "cc.pretty"

local tArgs = { ... }
if #tArgs == 0 then
    -- "set"
    local _, y = term.getCursorPos()
    local tSettings = {}
    for n, sName in ipairs(settings.getNames()) do
        tSettings[n] = textutils.serialize(sName) .. " " .. translate("cc.set.is") .. " ".. textutils.serialize(settings.get(sName))
    end
    textutils.pagedPrint(table.concat(tSettings, "\n"), y - 3)

elseif #tArgs == 1 then
    -- "set foo"
    local sName = tArgs[1]
    local deets = settings.getDetails(sName)
    local msg = pp.text(sName, colors.cyan) .. " " .. translate("cc.set.is") .. " " .. pp.pretty(deets.value)
    if deets.default ~= nil and deets.value ~= deets.default then
        msg = msg .. " (" .. translate("cc.set.default") .. " " .. pp.pretty(deets.default) .. ")"
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
        print(translate("cc.set.unset"):format(textutils.serialize(sName)))
    elseif option.type and option.type ~= type(value) then
        printError(translate("cc.set.not_valid"):format(textutils.serialize(sValue), option.type))
    else
        settings.set(sName, value)
        print(translate("cc.set.set_to"):format(textutils.serialize(sName),textutils.serialize(value)))
    end

    if value ~= option.value then
        settings.save()
    end
end
