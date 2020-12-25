--[[- The @{cc.expect} library provides helper functions for verifying that
function arguments are well-formed and of the correct type.

@module cc.expect
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
    if native_type(debug) == "table" and native_type(debug.getinfo) == "function" then
        local ok, info = pcall(debug.getinfo, 3, "nS")
        if ok and info.name and info.name ~= "" and info.what ~= "C" then name = info.name end
    end

    local type_names = get_type_names(...)
    if name then
        error(("bad argument #%d to '%s' (expected %s, got %s)"):format(index, name, type_names, t), 3)
    else
        error(("bad argument #%d (expected %s, got %s)"):format(index, type_names, t), 3)
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

    if value == nil then
        error(("field '%s' missing from table"):format(index), 3)
    else
        error(("bad field '%s' (expected %s, got %s)"):format(index, get_type_names(...), t), 3)
    end
end

return {
    expect = expect,
    field = field,
}
