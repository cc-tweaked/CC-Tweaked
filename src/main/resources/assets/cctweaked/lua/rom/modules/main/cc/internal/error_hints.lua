-- SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- Internal tools for diagnosing errors and suggesting fixes.

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

@local
]]

local debug, type, rawget = debug, type, rawget
local sub, lower, find, min, abs = string.sub, string.lower, string.find, math.min, math.abs

--[[- Compute the Optimal String Distance between two strings.

@tparam string str_a The first string.
@tparam string str_b The second string.
@treturn number|nil The distance between two strings, or nil if they are two far
apart.
]]
local function osa_distance(str_a, str_b, threshold)
    local len_a, len_b = #str_a, #str_b

    -- If the two strings are too different in length, then bail now.
    if abs(len_a - len_b) > threshold then return end

    -- Zero-initialise our distance table.
    local d = {}
    for i = 1, (len_a + 1) * (len_b + 1) do d[i] = 0 end

    -- Then fill the first row and column
    local function idx(a, b) return a * (len_a + 1) + b + 1 end
    for i = 0, len_a do d[idx(i, 0)] = i end
    for j = 0, len_b do d[idx(0, j)] = j end

    -- Then compute our distance
    for i = 1, len_a do
        local char_a = sub(str_a, i, i)
        for j = 1, len_b do
            local char_b = sub(str_b, j, j)

            local sub_cost
            if char_a == char_b then
                sub_cost = 0
            elseif lower(char_a) == lower(char_b) then
                sub_cost = 0.5
            else
                sub_cost = 1
            end

            local new_cost = min(
                d[idx(i - 1, j)] + 1, -- Deletion
                d[idx(i, j - 1)] + 1, -- Insertion,
                d[idx(i - 1, j - 1)] + sub_cost -- Substitution
            )

            -- Transposition
            if i > 1 and j > 1 and char_a == sub(str_b, j - 1, j - 1) and char_b == sub(str_a, i - 1, i - 1) then
                local trans_cost = d[idx(i - 2, j - 2)] + 1
                if trans_cost < new_cost then new_cost = trans_cost end
            end

            d[idx(i, j)] = new_cost
        end
    end

    local result = d[idx(len_a, len_b)]
    if result <= threshold then return result else return nil end
end

--- Check whether this suggestion is useful.
local function useful_suggestion(str)
    local len = #str
    return len > 0 and len < 32 and find(str, "^[%a_]%w*$")
end

local function get_suggestions(is_global, value, key, thread, frame_offset)
    if not useful_suggestion(key) then return end

    -- Pick a maximum number of edits. We're more lenient on longer strings, but
    -- still only allow two mistakes.
    local threshold = #key >= 5 and 2 or 1

    -- Find all items in the table, and see if they seem similar.
    local suggestions = {}
    local function process_suggestion(k)
        if type(k) ~= "string" or not useful_suggestion(k) then return end

        local distance = osa_distance(k, key, threshold)
        if distance then
            if distance < threshold then
                -- If this is better than any existing match, then prefer it.
                suggestions = { k }
                threshold = distance
            else
                -- Otherwise distance==threshold, and so just add it.
                suggestions[#suggestions + 1] = k
            end
        end
    end

    while type(value) == "table" do
        for k in next, value do process_suggestion(k) end

        local mt = debug.getmetatable(value)
        if mt == nil then break end
        value = rawget(mt, "__index")
    end

    -- If we're attempting to lookup a global, then also suggest any locals and
    -- upvalues. Our upvalues will be incomplete, but maybe a little useful?
    if is_global then
        for i = 1, 200 do
            local name = debug.getlocal(thread, frame_offset, i)
            if not name then break end
            process_suggestion(name)
        end

        local func = debug.getinfo(thread, frame_offset, "f").func
        for i = 1, 255 do
            local name = debug.getupvalue(func, i)
            if not name then break end
            process_suggestion(name)
        end
    end

    table.sort(suggestions)

    return suggestions
end

--[[- Get a tip to display at the end of an error.

@tparam string err The error message.
@tparam coroutine thread The current thread.
@tparam number frame_offset The offset into the thread where the current frame exists
@return An optional message to append to the error.
]]
local function get_tip(err, thread, frame_offset)
    local nil_op = err:match("^attempt to (%l+) .* %(a nil value%)")
    if not nil_op then return end

    local has_error_info, error_info = pcall(require, "cc.internal.error_info")
    if not has_error_info then return end
    local op, is_global, table, key = error_info.info_for_nil(thread, frame_offset)
    if op == nil or op ~= nil_op then return end

    local suggestions = get_suggestions(is_global, table, key, thread, frame_offset)
    if not suggestions or next(suggestions) == nil then return end

    local pretty = require "cc.pretty"
    local msg = "Did you mean: "

    local n_suggestions = min(3, #suggestions)
    for i = 1, n_suggestions do
        if i > 1 then
            if i == n_suggestions then msg = msg .. " or " else msg = msg .. ", " end
        end
        msg = msg .. pretty.text(suggestions[i], colours.lightGrey)
    end
    return msg .. "?"
end

return { get_tip = get_tip }
