local function getupvalue(fn, name)
    for i = 1, debug.getinfo(fn, "u").nups do
        local up_name, value = debug.getupvalue(fn, i)
        if up_name == name then return value end
    end
    error("Cannot find upvalue with name " .. name, 2)
end

return { getupvalue = getupvalue }
