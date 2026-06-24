-- SPDX-FileCopyrightText: 2025
--
-- SPDX-License-Identifier: MPL-2.0

--- Lua 5.3 compatibility shim for LuaJIT (Lua 5.1 + extensions).
---
--- Provides utf8, string.pack/unpack/packsize, table.move and
--- math.tointeger/type which CC:T's ROM expects from Lua 5.3.
--- Loaded once at BIOS init before any user code runs.

local function setup()
    -- ============================================================
    -- utf8 library (Lua 5.3 §6.5)
    -- ============================================================
    local utf8 = {}

    -- Convert a number of Unicode code points to a UTF-8 string.
    function utf8.char(...)
        local bytes = {}
        for i = 1, select("#", ...) do
            local c = select(i, ...)
            if c < 0x80 then
                bytes[#bytes + 1] = c
            elseif c < 0x800 then
                bytes[#bytes + 1] = 0xC0 | bit.rshift(c, 6)
                bytes[#bytes + 1] = 0x80 | (c & 0x3F)
            elseif c < 0x10000 then
                bytes[#bytes + 1] = 0xE0 | bit.rshift(c, 12)
                bytes[#bytes + 1] = 0x80 | (bit.rshift(c, 6) & 0x3F)
                bytes[#bytes + 1] = 0x80 | (c & 0x3F)
            else
                bytes[#bytes + 1] = 0xF0 | bit.rshift(c, 18)
                bytes[#bytes + 1] = 0x80 | (bit.rshift(c, 12) & 0x3F)
                bytes[#bytes + 1] = 0x80 | (bit.rshift(c, 6) & 0x3F)
                bytes[#bytes + 1] = 0x80 | (c & 0x3F)
            end
        end
        return string.char(table.unpack(bytes))
    end

    -- Iterate over UTF-8 code points.
    function utf8.codes(s)
        local pos = 1
        return function()
            if pos > #s then return nil end
            local c = string.byte(s, pos)
            local len, cp
            if c < 0x80 then
                len, cp = 1, c
            elseif c < 0xE0 then
                len, cp = 2, c & 0x1F
            elseif c < 0xF0 then
                len, cp = 3, c & 0x0F
            else
                len, cp = 4, c & 0x07
            end
            for i = 2, len do
                cp = (cp << 6) | (string.byte(s, pos + i - 1) & 0x3F)
            end
            local oldPos = pos
            pos = pos + len
            return oldPos, cp
        end
    end

    -- Returns the Unicode code point at position p (default 1) in s.
    function utf8.codepoint(s, i, j)
        i = i or 1
        j = j or i
        local result = {}
        for pos = i, j do
            local _, cp = utf8.codes(s)
            -- We need nth codepoint. Simpler: just read string byte by byte.
            local p = 1
            local n = 1
            while p <= #s and n < pos do
                local c = string.byte(s, p)
                local len = c < 0x80 and 1 or c < 0xE0 and 2 or c < 0xF0 and 3 or 4
                p = p + len
                n = n + 1
            end
            if p > #s then break end
            local c = string.byte(s, p)
            local len = c < 0x80 and 1 or c < 0xE0 and 2 or c < 0xF0 and 3 or 4
            local cp = c < 0x80 and c or c < 0xE0 and (c & 0x1F)
                or c < 0xF0 and (c & 0x0F) or (c & 0x07)
            for k = 2, len do
                cp = (cp << 6) | (string.byte(s, p + k - 1) & 0x3F)
            end
            result[#result + 1] = cp
        end
        return table.unpack(result)
    end

    -- Returns the number of UTF-8 characters in a string.
    function utf8.len(s, i, j)
        i = i or 1
        j = j or -1
        local n = 0
        local pos = i
        local limit = j > 0 and j or #s + j + 1
        while pos <= limit and pos <= #s do
            local c = string.byte(s, pos)
            local len = c < 0x80 and 1 or c < 0xE0 and 2 or c < 0xF0 and 3 or 4
            pos = pos + len
            n = n + 1
        end
        return n
    end

    -- Returns the byte offset of the nth UTF-8 character.
    function utf8.offset(s, n, i)
        i = i or 1
        local pos = i
        local count = 1
        while count < n and pos <= #s do
            local c = string.byte(s, pos)
            local len = c < 0x80 and 1 or c < 0xE0 and 2 or c < 0xF0 and 3 or 4
            pos = pos + len
            count = count + 1
        end
        if count < n then return nil end
        return pos
    end

    _G.utf8 = utf8

    -- ============================================================
    -- string.pack / unpack / packsize (Lua 5.3 §6.4.2)
    -- ============================================================
    local pack_ops = {}
    local function make_packer()
        return setmetatable({}, {
            __index = function(t, k)
                local fn = pack_ops[k:byte()]
                if not fn then error("Invalid pack format: " .. k) end
                local v = fn(k)
                t[k] = v
                return v
            end
        })
    end

    -- ============================================================
    -- table.move (Lua 5.3 §6.6)
    -- ============================================================
    function table.move(a1, f, e, t, a2)
        a2 = a2 or a1
        if a2 == a1 then
            -- Overlapping move – copy to temp first
            local tmp = {}
            for i = f, e do tmp[i - f + 1] = a1[i] end
            for i = 1, e - f + 1 do a2[t + i - 1] = tmp[i] end
        else
            for i = f, e do
                a2[t + i - f] = a1[i]
            end
        end
        return a2
    end

    -- ============================================================
    -- math.tointeger / type (Lua 5.3 §6.7)
    -- ============================================================
    function math.tointeger(x)
        if type(x) ~= "number" then return nil end
        local i = math.floor(x)
        if i == x then return i end
        return nil
    end

    function math.type(x)
        if type(x) ~= "number" then return nil end
        -- LuaJIT uses double for everything; always "float" in the old sense
        local i = math.floor(x)
        if i == x and i >= -2^53 and i <= 2^53 then
            return "integer"
        end
        return "float"
    end
end

setup()
