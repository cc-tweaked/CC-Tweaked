local expect = (dofile("rom/modules/main/cc/expect.lua")).expect

UTFString = {}
local UTFString_mt = {
    __index = UTFString, __name = 'UTFString'
}

local StringWrapper = {}
local StringWrapper_mt = {
    __index = StringWrapper, __name = 'StringWrapper',
    __tostring = function() return self.str  end,
    __len = function() return #self.str  end
}

wrapStr = StringWrapper

local function rectify_string(s)
    -- check a string is a valid utf8 byte sequence
    -- if an invalid byte is found, consider it as raw codepoint and convert it to valid byte sequence
    -- for example, if '\xA7\x39' is mixed into s, such subsequence would be converted into '\xC2\xA7\x39'
    local result = ""
    local iter, s1, i = utf8.codes(s)
    local ok, codepoint
    while i do
        local oldI = i
        ok, i, codepoint = pcall(iter, s1, i)
        if not ok then
            i = oldI + 1
            result = result .. utf8.char(string.byte(s, utf8.offset(s, oldI + 1)))
        elseif codepoint then
            result = result .. utf8.char(codepoint)
        end
    end
    return result
end

function isUTFString(obj)
    return type(obj) == 'table' and getmetatable(obj) == UTFString_mt
end

function isStringWrapper(obj)
    return type(obj) == 'table' and getmetatable(obj) == StringWrapper_mt
end

local function unsafe_utfstring(s)
    -- unsafe version to create a UTFString instance. This function will not attempt to rectify invalid byte sequence.
    -- do not use this function unless s is guaranteed to be valid.
    return setmetatable({ bytestring = s }, UTFString_mt)
end

function UTFString:new(s)
    if isUTFString(s) then return unsafe_utfstring(s.bytestring) end
    return unsafe_utfstring(rectify_string(s))
end

setmetatable(UTFString, {__call = UTFString.new })

function StringWrapper:new(s)
    return setmetatable({ str = s }, StringWrapper_mt)
end

setmetatable(StringWrapper, {__call = StringWrapper.new })

function fromLatin(s)
    local str = ""
    for _, c in string.codes(s) do
        str = str .. utf8.char(c)
    end
    return unsafe_utfstring(str)
end

function UTFString:len()
    return utf8.len(self.bytestring)
end

UTFString_mt.__len = UTFString.len

local function common_concat(a,b)
    if type(a) == "string" then
        if isStringWrapper(b) then return StringWrapper(a .. b.str) end
        if isUTFString(b) then return UTFString(a .. b.bytestring) end
    elseif type(b) == "string" then
        if isStringWrapper(a) then return StringWrapper(a.str .. b) end
        if isUTFString(a) then return UTFString(a.bytestring .. b) end
    elseif isStringWrapper(a) then
        if isStringWrapper(b) then return StringWrapper(a.str .. b.str) end
        if isUTFString(b) then return UTFString(a.str .. b.bytestring) end
        error("attempt to concatenate StringWrapper and " .. type(b), 3)
    elseif isStringWrapper(b) then
        if isUTFString(a) then return UTFString(a.bytestring .. b.str) end
        error("attempt to concatenate " .. type(a) .. " and StringWrapper", 3)
    elseif isUTFString(a) then
        if isUTFString(b) then return unsafe_utfstring(a.bytestring .. b.bytestring) end
        error("attempt to concatenate UTFString and " .. type(b), 3)
    elseif a ~= nil and b ~= nil then
        error("attempt to concatenate " .. type(a) .. " and UTFString", 3)
        print(debug.traceback())
    end
    return nil
end

UTFString_mt.__concat = common_concat
StringWrapper_mt.__concat = common_concat()

local function common_eq(a,b)
    local function checkByCodepoint(a1, b1)
        if #a1 ~= #b1 then return false end
        for i = 1, #a1 do
            if a1:byte(i) ~= b1:byte(i) then return false end
        end
        return true
    end
    if isStringWrapper(a) then
        if isStringWrapper(b) then return a.str == b.str end
        if isUTFString(b) then return checkByCodepoint(a.str, b) end
    elseif isStringWrapper(b) then
        if isUTFString(a) then return checkByCodepoint(a, b.str) end
    elseif isUTFString(a) then
        if isUTFString(b) then return a.bytestring == b.bytestring end
    end
    return false
end

UTFString_mt.__eq = common_eq
StringWrapper_mt.__eq = common_eq

local function common_lt(a, b)
    local function checkByCodepoint(a1, b1)
        for i = 1, math.min(#a1, #b1) do
            if a1:byte(i) < b1:byte(i) then return true end
            if a1:byte(i) > b1:byte(i) then return false end
        end
        return #a1 < #b1
    end
    if isStringWrapper(a) then
        if isStringWrapper(b) then return a.str < b.str end
        if isUTFString(b) then return checkByCodepoint(a.str, b) end
    elseif isStringWrapper(b) then
        if isUTFString(a) then return checkByCodepoint(a, b.str) end
    elseif isUTFString(a) then
        if isUTFString(b) then return a.bytestring < b.bytestring end
    end
    return false
end

UTFString_mt.__lt = common_lt
StringWrapper_mt.__lt = common_lt

local function common_le(a,b)
    local function checkByCodepoint(a1, b1)
        for i = 1, math.min(#a1, #b1) do
            if a1:byte(i) < b1:byte(i) then return true end
            if a1:byte(i) > b1:byte(i) then return false end
        end
        return #a1 <= #b1
    end
    if isStringWrapper(a) then
        if isStringWrapper(b) then return a.str <= b.str end
        if isUTFString(b) then return checkByCodepoint(a.str, b) end
    elseif isStringWrapper(b) then
        if isUTFString(a) then return checkByCodepoint(a, b.str) end
    elseif isUTFString(a) then
        if isUTFString(b) then return a.bytestring <= b.bytestring end
    end
    return false

end

UTFString_mt.__le = common_le
StringWrapper_mt.__le = common_le()

function UTFString_mt:__tostring()
    -- return byte string, rather than erasing out anything outside 0x00-0xff
    -- if such process is wanted, use UTFString:to_latin
    return self.bytestring
end

function UTFString:tostring()
    return self.bytestring
end

local function _sub(s, i, j)
    expect(1, s, "string")
    expect(2, i, "number")
    expect(3, j, "number", "nil")
    if i > utf8.len(s) then return '' end
    if i == j then
        return utf8.char(utf8.codepoint(s, utf8.offset(s, i)))
    end
    local e
    if j == nil then e = -1
    else e = j end
    if e < 0 then e = math.max(0, utf8.len(s) - e + 1) end
    if e > utf8.len(s) then e = utf8.len(s) end
    if e ~= 0 then e = utf8.offset(s, e + 1) - 1 end
    return string.sub(s, utf8.offset(s, i), e)
end


function UTFString:sub(i, j)
    return UTFString(_sub(self.bytestring, i, j))
end

function UTFString:byte(i, j)
    -- return codepoint of u[i], u[i+1], ..., u[j].
    expect(1, i, 'number')
    expect(2, j, 'number', 'nil')
    return utf8.codepoint(self.bytestring, utf8.offset(self.bytestring, i), j and utf8.offset(self.bytestring, j))
end

function UTFString:toLatin()
    local str = ""
    for _, c in utf8.codes(self.bytestring) do
        if c > 255 then str = str .. "?"
        else str = str .. string.char(c) end
    end
    return str
end

function UTFString:format(...)
    local args = table.pack(...)
    for i = 1, args.n do
        if isUTFString(args[i]) then args[i] = args[i].bytestring
        elseif type(args[i]) == 'string' then args[i] = rectify_string(args[i]) end
    end
    return UTFString(string.format(self.bytestring, table.unpack(args, 1, args.n)))
end

--- REGION pattern functions

--- Check if the pattern is simple enough to redirect back to the standard string library.
--- Such pattern has to be:
--- * Not containing bytes larger than 0x7F
--- * Not containing negated char-set
--- * Not containing any character set except %z
--- * Not containing . or .?
--- * Not containing () the position-captures.
--- * Will not match the empty string
local function isSimplePattern(pattern)
    expect(1, pattern, "string")
    local safeFind = function( ... )
        local ok, ret = pcall(string.find, ...)
        return ok and ret
    end

    return not (
        pattern:find("[\128-\255]") or
        pattern:find("%[%^") or
        pattern:find("%%[acdlpsuwxACDLPSUWXZ]") or
        pattern:find("%.[^*+-]") or pattern:find("%.$") or
        pattern:find("()", 1, true) or
        pattern == '' or safeFind('', pattern)
    )
end

local charsetCache = {}
setmetatable(charsetCache, { __mode = "kv" })

local function find_(str, substr, init, noAncher)
    local charsets = require and require("cc.utflib.charsets") or dofile("rom/modules/main/cc/utflib/charsets.lua")
    local anchor = false
    local ncapt, captures
    local captparen = {}

    local function getcapt(n, errMsg, errLvl)
        if n > ncapt then
            error(errMsg, errLvl)
        elseif type(captures[n]) == 'table' then
            if captures[n][2] == '' then
                error(errMsg, errLvl)
            end
            return _sub(str, captures[n][1], captures[n][2]), captures[n][2] - captures[n][1] + 1
        else
            return captures[n], math.floor(math.log10(captures[n])) + 1
        end
    end

    local safeGetCp = function(s, p)
        local ok, cp = pcall(utf8.codepoint, s, p)
        return ok and cp or nil
    end

    local match, match_charset, parse_charset
    match = function(sp, pp)
        local pcp = safeGetCp(substr, pp)
        if pcp == 0x28 then -- '('
            ncapt = ncapt + 1
            captparen[ncapt] = pp
            local ret
            if safeGetCp(substr, pp + 1) == 0x29 then -- ')'
                captparen[ncapt] = sp
                ret = match(sp, pp + 2)
            else
                captures[ncapt] = {sp, ''}
                ret = match(sp, pp + 1)
            end
            if ret then
                return ret
            else
                ncapt = ncapt - 1
                return nil
            end
        elseif pcp == 0x29 then -- ')'
            for n = ncapt, 1, -1 do
                if type(captures[n]) == 'table' and captures[n][2] == '' then
                    captures[n][2] = sp - 1
                    local ret = match(sp, pp + 1)
                    if ret then
                        return ret
                    else
                        captures[n][2] = ''
                        return nil
                    end
                end
            end
            error('Unmatched close-paren at pattern character ' .. pp, 3)
        elseif pcp == 0x5b then -- '['
            return match_charset(sp, parse_charset(pp))
        elseif pcp == 0x5d then -- ']'
            error('Unmatched close-bracket at pattern character ' .. pp, 3)
        elseif pcp == 0x25 then -- '%'
            c = safeGetCp(substr, pp + 1)
            if charsets[c] then
                return match_charset(sp, pp + 2, charsets[c])
            elseif c == 0x62 then -- 'b'. balanced delimiter match
                local openingCp = safeGetCp(substr, pp + 2)
                local closingCp = safeGetCp(substr, pp + 3)
                if not openingCp or not closingCp then
                    error('malformed pattern (missing arguments to \'%b\')', 3)
                end
                if safeGetCp(str, sp) ~= openingCp then
                    return nil
                end
                sp = sp + 1
                local matchedOpening = 1
                while true do
                    c = safeGetCp(str, sp)
                    sp = sp + 1
                    if not c then
                        return nil
                    elseif c == closingCp then
                        if matchedOpening == 1 then
                            return match(sp, pp + 4)
                        end
                        matchedOpening = matchedOpening - 1
                    elseif c == openingCp then
                        matchedOpening = matchedOpening + 1
                    end
                end
            elseif c == 0x66 then -- 'f'. frontier pattern match
                if safeGetCp(substr, pp + 2) ~= 0x5b then
                    error('missing \'[\' after %f in pattern at pattern character ' .. pp, 3)
                end
                local pp, charset = parse_charset(pp + 2)
                local c1 = safeGetCp(str, sp - 1) or 0
                local c2 = safeGetCp(str, sp) or 0
                if not charset[c1] and charset[c2] then
                    return match(sp, pp)
                else
                    return nil
                end
            elseif c >= 0x30 and c <= 0x39 then -- '0' to '9'
                local m, l = getcapt(c - 0x30, 'invalid capture index %' .. c .. ' at pattern character ' .. pp, 3)
                local ep = math.min(utf8.len(str) + 1, sp + 1)
                if _sub(str, sp, ep - 1) == m then
                    return match(ep, pp + 2)
                else
                    return nil
                end
            elseif not c then -- % at the end of string
                error('malformed pattern (ends with \'%\')', 3)
            else
                return match_charset(sp, pp + 2, {[c] = 1})
            end
        elseif pcp == 0x2e then -- '.'
            if not charsetCache['.'] then
                local tbl = {}
                setmetatable(tbl, { __index = function(t, k) return k end})
                charsetCache['.'] = {1, tbl}
            end
            return match_charset(sp, pp + 1, charsetCache['.'][2] )
        elseif pcp == nil then
            return sp
        elseif pcp == 0x24 and utf8.len(substr) == pp then -- '$'
            return (sp == utf8.len(str) + 1) and sp or nil
        else
            return match_charset(sp, pp + 1, {[pcp] = 1})
        end
    end
    parse_charset = function(pp)
        local _, ep
        local epp = utf8.offset(substr, pp) + 1
        if string.sub(substr, epp, epp) == '^' then
            epp = epp + 1
        end
        if string.sub(substr, epp, epp) == ']' then
            epp = epp + 1
        end
        repeat
            _, ep = string.find(substr, ']', epp, true)
            if not ep then
                error('Missing close-bracket for character set beginning at pattern character ' .. pp, 3)
            end
            epp = ep + 1
        until string.byte(substr, ep - 1) ~= 0x25 or string.byte(substr, ep - 2) == 0x25
        local key = string.sub(substr, utf8.offset(substr, pp), ep)
        if charsetCache[key] then
            local p1, cs = table.unpack(charsetCache[key])
            return pp + p1, cs
        end

        local p0 = pp
        local cs = {}
        local csrefs = { cs }
        local invert = false
        pp = pp + 1
        if safeGetCp(substr, pp) == 0x5e then -- '^'
            invert = true
            pp = pp + 1
        end
        local first = true
        while true do
            local c = safeGetCp(substr, pp)
            if not first and c == 0x5d then -- closing ']'
                pp = pp + 1
                break
            elseif c == 0x25 then -- %
                c = safeGetCp(substr, pp + 1)
                if charsets[c] then
                    csrefs[#csrefs + 1] = charsets[c]
                else
                    cs[c] = 1
                end
                pp = pp + 2
            elseif safeGetCp(substr, pp + 1) == 0x2d and safeGetCp(substr, pp + 2) and safeGetCp(substr, pp + 2) ~= 0x5d then
                -- range '-'
                for i = c, safeGetCp(substr, pp + 2) do
                    cs[i] = 1
                end
                pp = pp + 3
            elseif not c then
                error('Missing close-bracket', 3)
            else
                cs[c] = 1
                pp = pp + 1
            end
            first = false
        end

        local ret
        if not csrefs[2] then
            if not invert then
                ret = cs
            else
                ret = {}
                setmetatable(ret, { __index = function(t, k) return k and not cs[k] end })
            end
        else
            ret = {}
            setmetatable(ret, {__index = function(t, k)
                if not k then
                    return nil
                end
                for i = 1, #csrefs do
                    if csrefs[i][k] then return not invert end
                end
                return invert
            end
            })
        end
        charsetCache[key] = { pp - p0, ret }
        return pp, ret
    end

    match_charset = function(sp, pp, charset)
        local q = safeGetCp(substr, pp)
        if q == 0x2a then -- '*' 0 or more
            pp = pp + 1
            local i = 0
            while charset[safeGetCp(str, sp + i)] do
                i = i + 1
            end
            while i >= 0 do
                local ret = match(sp + i, pp)
                if ret then return ret end
                i = i - 1
            end
            return nil
        elseif q == 0x2b then -- '+' 1 or more
            pp = pp + 1
            local i = 0
            while charset[safeGetCp(str, sp + i)] do
                i = i + 1
            end
            while i > 0 do
                local ret = match(sp + i, pp)
                if ret then return ret end
                i = i - 1
            end
            return nil
        elseif q == 0x2d then -- '-' non-greedy 0 or more
            pp = pp + 1
            while true do
                local ret = match(sp, pp)
                if ret then return ret end
                if not charset[safeGetCp(str, sp)] then return nil end
                sp = sp + 1
            end
        elseif q == 0x3f then -- '?' 0 or 1
            pp = pp + 1
            if charset[safeGetCp(sp)] then
                local ret = match(sp + 1, pp)
                if ret then return ret end
            end
            return match(sp, pp)
        else
            if charset[safeGetCp(str, sp)] then
                return match(sp + 1, pp)
            else
                return nil
            end
        end
    end

    init = init or 1
    if init < 0 then init = utf8.len(str) + init + 1 end
    init = math.max(1, math.min(init, utf8.len(str) + 1))
    local sp = init
    local pp = 1
    if not noAncher and safeGetCp(substr, 1) == 0x5e then
        anchor = true
        pp = 2
    end
    repeat
        ncapt, captures = 0, {}
        local ep = match(sp, pp)
        if ep then
            for i = 1, ncapt do
                captures[i] = getcapt(i, 'Unclosed capture beginning at pattern character ' .. captparen[i], 3)
            end
            return sp, ep - 1, table.unpack(captures)
        end
        sp = sp + 1
    until anchor or sp > utf8.len(str) + 1
    return nil
end

function UTFString:find(substr, init, plain)
    expect(1, substr, 'string', 'table')
    expect(2, init, 'number', 'nil')
    expect(3, plain, 'boolean', 'nil')
    if type(substr) == 'table' then
        if not isUTFString(substr) then
            error("bad argument #1 to 'substr' (string or UTFString expected, got table)", 3)
        end
        substr = substr.bytestring
    else
        substr = rectify_string(substr)
    end
    if plain or isSimplePattern(substr) then
        if init == nil then init = 1 end
        if init > self:len() + 1 then
            init = self:len() + 1
        end
        local ok, result
        if plain then
            ok = true
            result = table.pack(string.find(self.bytestring, substr, utf8.offset(self.bytestring, init), plain))
        else
            result = table.pack(pcall(string.find, self.bytestring, substr, utf8.offset(self.bytestring, init), plain))
            ok = table.remove(result, 1)
        end
        if ok then
            if result then
                result[1] = result[1] and (utf8.len(string.sub(self.bytestring, 1, result[1] - 1)) + 1)
                result[2] = result[2] and (utf8.len(string.sub(self.bytestring, 1, result[2] - 1)) + 1)
                return table.unpack(result, 1)
            end
        end
    end
    fresult = table.pack(find_(self.bytestring, substr, init))
    if fresult then
        for i = 1, #fresult do
            if type(fresult[i]) == "string" then
                fresult[i] = UTFString(fresult[i])
            end
        end
    end
    return table.unpack(fresult)
end

function UTFString:gmatch(pattern)
    expect(1, pattern, "string", "table")
    if type(pattern) == "table" then
        if not isUTFString(pattern) then error("bad argument #1 to 'pattern' (string or UTFString expected, got table)", 3) end
        pattern = pattern.bytestring
    else pattern = rectify_string(pattern) end
    if isSimplePattern(pattern) then
        local tresult = table.pack(pcall(string.gmatch, self.bytestring, pattern))
        if tresult[1] then
            for i = 2, #tresult do
                if type(tresult[i]) == "string" then
                    tresult[i] = UTFString(tresult[i])
                end
            end
            return table.unpack(tresult, 2)
        end
    end
    local init = 1
    return function()
        local m = { find_(self.bytestring, pattern, init, true) }
        if not m[1] then return nil end
        init = m[2] + 1
        if m[3] then
            for i = 3, #m do
                if type(m[i]) == "string" then
                    m[i] = UTFString(m[i])
                end
            end
            return table.unpack(m, 3)
        end
        return self:sub(m[1], m[2])
    end
end

function UTFString:gsub(pattern, repl, n)
    expect(1, pattern, "string", "table")
    expect(2, repl, "string", "table", "function")
    expect(3, n, "number", "nil")
    if type(pattern) == "table" then
        if not isUTFString(pattern) then error("bad argument #1 to 'pattern' (string or UTFString expected, got table)", 3) end
        pattern = pattern.bytestring
    else pattern = rectify_string(pattern)end
    if isUTFString(repl) then repl = repl.bytestring
    elseif type(repl) == "string" then repl = rectify_string(repl) end
    if isSimplePattern(pattern) then
        local tresult = table.pack(pcall(string.gsub, self.bytestring, pattern, repl, n))
        if tresult[1] then
            for i = 2, #tresult do
                if type(tresult[i]) == "string" then
                    tresult[i] = UTFString(tresult[i])
                end
            end
            return table.unpack(tresult, 2)
        end
    end
    if n == nil then
        n = 1e100
    end
    if n < 1 then
        return self, 0
    end
    if utf8.codepoint(pattern, 1) == 0x5e then -- '^'. the pattern only matches at the beginning
        -- only one replacement will be happened
        n = 1
    end
    local init = 1
    local replacedTotal = 0
    local ret = {}
    local zeroAdjustment = 0
    repeat
        local m = { find_(self.bytestring, pattern, init + zeroAdjustment)}
        if not m[1] then break end
        if init < m[1] then
            ret[#ret + 1] = self:sub(init, m[1] - 1)
        end
        local mm = self:sub(m[1], m[2])
        if #m < 3 then m[3] = mm end
        local val
        if type(repl) == 'function' then
            val = repl(table.unpack(m, 3))
        elseif type(repl) == 'table' then
            val = repl[m[3]]
        else
            if replacedTotal == 0 and #m < 11 then
                local ss = string.gsub(repl, "%%[%%0-" .. (#m - 2) .. ']', x)
                ss = string.match(ss, '%%[0-9]')
                if ss then
                    error('invalid capture index ' .. ss .. ' in replacement string', 2)
                end
            end
            local t = {
                ["%0"] = mm,
                ["%1"] = m[3],
                ["%2"] = m[4],
                ["%3"] = m[5],
                ["%4"] = m[6],
                ["%5"] = m[7],
                ["%6"] = m[8],
                ["%7"] = m[9],
                ["%8"] = m[10],
                ["%9"] = m[11],
                ["%%"] = "%"
            }
            val = string.gsub(repl, '%%[%%0-9]', t)
        end
        local valType = type(val)
        if valType ~= 'nil' and valType ~= 'string' and valType ~= 'number' then
            error('invalid replacement value (' .. valType .. ')', 2)
        end
        ret[#ret + 1] = val or mm
        init = m[2] + 1
        replacedTotal = replacedTotal + 1
        zeroAdjustment = m[2] < m[1] and 1 or 0
    until init > self:len() or replacedTotal >= n
    if init <= self:len() then
        ret[#ret + 1] = self:sub(init, self:len()).bytestring
    end
    return UTFString(table.concat(ret)), replacedTotal
end

function UTFString:match(pattern, init)
    expect(1, pattern, "string", "table")
    expect(2, init, "number", "nil")
    if type(pattern) == "table" then
        if not isUTFString(pattern) then error("bad argument #1 to 'pattern' (string or UTFString expected, got table)", 3) end
        pattern = pattern.bytestring
    else pattern = rectify_string(pattern) end
    if isSimplePattern(pattern) then
        local tresult = table.pack(pcall(string.match, self.bytestring, pattern, utf8.offset(self.bytestring, init or 1)))
        if tresult[1] then
            for i = 2, #tresult do
                if type(tresult[i]) == "string" then
                    tresult[i] = UTFString(tresult[i])
                end
            end
            return table.unpack(tresult, 2)
        end
    end
    local m = {find_(self.bytestring, pattern, init)}
    if not m[1] then
        return nil
    end
    if m[3] then
        for i = 3, #m do
            if type(m[i]) == "string" then
                m[i] = UTFString(m[i])
            end
        end
        return table.unpack(m, 3)
    end
    return self:sub(m[1], m[2])
end

-- ENDREGION pattern functions

function UTFString:rep(n, sep)
    expect(1, n, "number")
    expect(2, sep, "string", "table", "nil")
    if type(sep) == "table" then
        if not isUTFString(sep) then error("bad argument #2 to 'sep' (string or UTFString expected, got table)", 3) end
        sep = sep.bytestring
    elseif sep then sep = rectify_string(sep) end
    return UTFString(string.rep(self.bytestring, n, sep))
end

function UTFString:lower()
    local s = ""
    for _, c in utf8.codes(self.bytestring) do
        if c < 255 then
            s = s .. utf8.char(string.byte(string.lower(string.char(c))))
        else
            s = s .. utf8.char(c)
        end
    end
    return unsafe_utfstring(s)
end


function UTFString:upper()
    local s = ""
    for _, c in utf8.codes(self.bytestring) do
        if c < 255 then
            s = s .. utf8.char(string.byte(string.upper(string.char(c))))
        else
            s = s .. utf8.char(c)
        end
    end
    return unsafe_utfstring(s)
end

function UTFString:reverse()
    local codes = {n = 0}
    for _, c in utf8.codes(self.bytestring) do
        codes.n = codes.n + 1
        codes[codes.n] = c
    end
    local s = ""
    for i = codes.n, 1, -1 do s = s .. utf8.char(codes[i]) end
    return unsafe_utfstring(s)
end
