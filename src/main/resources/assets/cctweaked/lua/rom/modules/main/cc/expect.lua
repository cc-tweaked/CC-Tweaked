-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- The [`cc.expect`] library provides helper functions for verifying that
function arguments are well-formed and of the correct type.

@module cc.expect
@since 1.84.0
@changed 1.96.0 The module can now be called directly as a function, which wraps around `expect.expect`.
@usage Define a basic function and check it has the correct arguments.

    local expect = require "cc.expect"
    local expect, field = expect.expect, expect.field

    local function add_person(name, info)
        expect(1, name, "string")
        expect(2, info, "table", "nil")

        if info then
            print("Got age=", field(info, "age", "number"))
            print("Got gender=", field(info, "gender", "string", "nil"))
        end
    end

    add_person("Anastazja") -- `info' is optional
    add_person("Kion", { age = 23 }) -- `gender' is optional
    add_person("Caoimhin", { age = 23, gender = true }) -- error!
]]

local native_select, native_type = select, type

local function get_type_names(...)
    local types = table.pack(...)
    for i = types.n, 1, -1 do
        if types[i] == "nil" then table.remove(types, i) end
    end

    if #types <= 1 then
        return tostring(...)
    else
        return table.concat(types, ", ", 1, #types - 1) .. " or " .. types[#types]
    end
end


local function get_display_type(value, t)
    -- Lua is somewhat inconsistent in whether it obeys __name just for values which
    -- have a per-instance metatable (so tables/userdata) or for everything. We follow
    -- Cobalt and only read the metatable for tables/userdata.
    if t ~= "table" and t ~= "userdata" then return t end

    local metatable = debug.getmetatable(value)
    if not metatable then return t end

    local name = rawget(metatable, "__name")
    if type(name) == "string" then return name else return t end
end

--- Expect an argument to have a specific type.
--
-- @tparam number index The 1-based argument index.
-- @param value The argument's value.
-- @tparam string ... The allowed types of the argument.
-- @return The given `value`.
-- @throws If the value is not one of the allowed types.
local function expect(index, value, ...)
    local t = native_type(value)
    for i = 1, native_select("#", ...) do
        if t == native_select(i, ...) then return value end
    end

    -- If we can determine the function name with a high level of confidence, try to include it.
    local name
    local ok, info = pcall(debug.getinfo, 3, "nS")
    if ok and info.name and info.name ~= "" and info.what ~= "C" then name = info.name end

    t = get_display_type(value, t)

    local type_names = get_type_names(...)
    if name then
        error(("bad argument #%d to '%s' (%s expected, got %s)"):format(index, name, type_names, t), 3)
    else
        error(("bad argument #%d (%s expected, got %s)"):format(index, type_names, t), 3)
    end
end

--- Expect an field to have a specific type.
--
-- @tparam table tbl The table to index.
-- @tparam string index The field name to check.
-- @tparam string ... The allowed types of the argument.
-- @return The contents of the given field.
-- @throws If the field is not one of the allowed types.
local function field(tbl, index, ...)
    expect(1, tbl, "table")
    expect(2, index, "string")

    local value = tbl[index]
    local t = native_type(value)
    for i = 1, native_select("#", ...) do
        if t == native_select(i, ...) then return value end
    end

    t = get_display_type(value, t)

    if value == nil then
        error(("field '%s' missing from table"):format(index), 3)
    else
        error(("bad field '%s' (%s expected, got %s)"):format(index, get_type_names(...), t), 3)
    end
end

local function is_nan(num)
  return num ~= num
end

--- Expect a number to be within a specific range.
--
-- @tparam number num The value to check.
-- @tparam[opt=-math.huge] number min The minimum value.
-- @tparam[opt=math.huge] number max The maximum value.
-- @return The given `value`.
-- @throws If the value is outside of the allowed range.
-- @since 1.96.0
local function range(num, min, max)
  expect(1, num, "number")
  min = expect(2, min, "number", "nil") or -math.huge
  max = expect(3, max, "number", "nil") or math.huge
  if min > max then
      error("min must be less than or equal to max)", 2)
  end

  if is_nan(num) or num < min or num > max then
      error(("number outside of range (expected %s to be within %s and %s)"):format(num, min, max), 3)
  end

  return num
end

return setmetatable({
    expect = expect,
    field = field,
    range = range,
}, { __call = function(_, ...) return expect(...) end })
