--- The @{cc.expect} library provides helper functions for verifying that
-- function arguments are well-formed and of the correct type.
--
-- @module cc.expect

local native_select, native_type = select, type

--- Expect an argument to have a specific type.
--
-- @tparam number index The 1-based argument index.
-- @param value The argument's value.
-- @tparam string ... The allowed types of the argument.
-- @throws If the value is not one of the allowed types.
local function expect(index, value, ...)
    local t = native_type(value)
    for i = 1, native_select("#", ...) do
        if t == native_select(i, ...) then return true end
    end

    local types = table.pack(...)
    for i = types.n, 1, -1 do
        if types[i] == "nil" then table.remove(types, i) end
    end

    local type_names
    if #types <= 1 then
        type_names = tostring(...)
    else
        type_names = table.concat(types, ", ", 1, #types - 1) .. " or " .. types[#types]
    end

    -- If we can determine the function name with a high level of confidence, try to include it.
    local name
    if native_type(debug) == "table" and native_type(debug.getinfo) == "function" then
        local ok, info = pcall(debug.getinfo, 3, "nS")
        if ok and info.name and #info.name ~= "" and info.what ~= "C" then name = info.name end
    end

    if name then
        error( ("bad argument #%d to '%s' (expected %s, got %s)"):format(index, name, type_names, t), 3 )
    else
        error( ("bad argument #%d (expected %s, got %s)"):format(index, type_names, t), 3 )
    end
end

return { expect = expect }
