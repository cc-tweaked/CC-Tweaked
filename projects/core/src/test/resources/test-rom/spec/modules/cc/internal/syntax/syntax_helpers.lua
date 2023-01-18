local function make_context(input)
    local lines = { 1 }
    local function line(pos) lines[#lines + 1] = pos end

    local function get_pos(pos)
        for i = #lines, 1, -1 do
            local start = lines[i]
            if pos >= start then return i, pos - start + 1, start end
        end

        error("Position is <= 0", 2)
    end

    return { line = line, get_pos = get_pos }
end

return { make_context = make_context }
