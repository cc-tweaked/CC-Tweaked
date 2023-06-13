-- Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
--
-- SPDX-License-Identifier: LicenseRef-CCPL

local tArgs = { ... }
if #tArgs == 0 or #tArgs > 3 then
  print("Usages:")
  print("redset <side> <true/false>")
  print("redset <side> <number>")
  print("redset <side> <color> <true/false>")
  return
end

local sSide = tArgs[1]
if #tArgs == 2 then
    local value = tArgs[2]
    if tonumber(value) ~= nil then
        redstone.setBundledOutput(sSide, tonumber(value))
    elseif value == "true" or value == "false" then
        redstone.setOutput(sSide, value == "true")
    else
        print("Value must be a number or true/false")
        return
    end

else
    local sColour = tArgs[2]
    local nColour = colors[sColour] or colours[sColour]
    if type(nColour) ~= "number" then
        print("No such color")
        return
    end

    local sValue = tArgs[3]
    if sValue == "true" then
        rs.setBundledOutput(sSide, colors.combine(rs.getBundledOutput(sSide), nColour))
    elseif sValue == "false" then
        rs.setBundledOutput(sSide, colors.subtract(rs.getBundledOutput(sSide), nColour))
    else
        print("Value must be true/false")
        return
    end
end
