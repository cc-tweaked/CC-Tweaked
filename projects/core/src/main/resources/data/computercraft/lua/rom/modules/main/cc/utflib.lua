local expect = (require and require("cc.expect") or dofile("rom/modules/main/cc/expect.lua")).expect

local UTFString = {}
local UTFString_mt = {
    __index = UTFString, __name = 'UTFString'
}

local StringWrapper = {}
local StringWrapper_mt = {
    __index = StringWrapper, __name = 'StringWrapper',
    __tostring = function() return self.str  end,
    __len = function() return #self.str  end
}

local function rectify_string(s)
    -- check a string is a valid utf8 byte sequence
    -- if an invalid byte is found, consider it as raw codepoint and convert it to valid byte sequence
    -- for example, if '\xA7\x39' is mixed into s, such subsequence would be converted into '\xC2\xA7\x39'
    local result = ""
    --print("rectify: " .. type(s))
    --print(debug.traceback())
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

local function isUTFString(obj)
    return type(obj) == 'table' and getmetatable(obj) == UTFString_mt
end

local function isStringWrapper(obj)
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

local function from_latin(s)
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

function UTFString:byte(i, j)
    -- return codepoint of u[i], u[i+1], ..., u[j].
    expect(1, i, 'number')
    expect(2, j, 'number', 'nil')
    return utf8.codepoint(self.bytestring, utf8.offset(self.bytestring, i), j and utf8.offset(self.bytestring, j))
end

function UTFString:to_latin()
    local str = ""
    for _, c in utf8.codes(self.bytestring) do
        if c > 255 then str = str .. "?"
        else str = str .. string.char(c) end
    end
    return str
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
    if not plain then
        substr = string.gsub(string.gsub(pattern, "([^%%]?)%.", '%1' .. utf8.charpattern), "([^%%])%.", '%1' .. utf8.charpattern)
    end
    local start, end_ = string.find(self.bytestring, substr, init, plain)
    if not start then return nil end
    return utf8.len(string.sub(self.bytestring, 1, start - 1)) + 1, utf8.len(string.sub(self.bytestring, 1, end_ - 1)) + 1
end


function UTFString:format(...)
    local args = table.pack(...)
    for i = 1, args.n do
        if isUTFString(args[i]) then args[i] = args[i].bytestring
        elseif type(args[i]) == 'string' then args[i] = rectify_string(args[i]) end
    end
    return UTFString(string.format(self.bytestring, table.unpack(args, 1, args.n)))
end

function UTFString:gmatch(pattern)
    expect(1, pattern, "string", "table")
    if type(pattern) == "table" then
        if not isUTFString(pattern) then error("bad argument #1 to 'pattern' (string or UTFString expected, got table)", 3) end
        pattern = pattern.bytestring
    else pattern = rectify_string(pattern) end
    pattern = string.gsub(string.gsub(pattern, "([^%%]?)%.", '%1' .. utf8.charpattern), "([^%%])%.", '%1' .. utf8.charpattern)
    local iter = string.gmatch(self.bytestring, pattern)
    return function()
        local matches = table.pack(iter())
        for i = 1, matches.n do
            local tt = type(matches[i])
            if tt == "string" then matches[i] = UTFString(matches[i])
            elseif tt == "number" then matches[i] = utf8.len(string.sub(self.bytestring, 1, matches[i])) + 1 end
        end
        return table.unpack(matches, 1, matches.n)
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
    pattern = string.gsub(string.gsub(pattern, "([^%%]?)%.", '%1' .. utf8.charpattern), "([^%%])%.", '%1' .. utf8.charpattern)
    local s, total = string.gsub(self.bytestring, pattern, repl, n)
    return UTFString(s), total
end

function UTFString:match(pattern, init)
    expect(1, pattern, "string", "table")
    expect(2, init, "number", "nil")
    if type(pattern) == "table" then
        if not isUTFString(pattern) then error("bad argument #1 to 'pattern' (string or UTFString expected, got table)", 3) end
        pattern = pattern.bytestring
    else pattern = rectify_string(pattern) end
    pattern = string.gsub(string.gsub(pattern, "([^%%]?)%.", '%1' .. utf8.charpattern), "([^%%])%.", '%1' .. utf8.charpattern)
    local matches = table.pack(string.match(self.bytestring, pattern, init and utf8.offset(self.bytestring, init)))
    for i = 1, matches.n do
        local tt = type(matches[i])
        if tt == "string" then matches[i] = UTFString(matches[i])
        elseif tt == "number" then matches[i] = utf8.len(string.sub(self.bytestring, 1, matches[i])) + 1 end
    end
    return table.unpack(matches, 1, matches.n)
end

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

function UTFString:sub(i, j)
    expect(1, i, "number")
    expect(2, j, "number", "nil")
    if i == j then
        return UTFString(utf8.char(utf8.codepoint(self.bytestring, utf8.offset(self.bytestring, i))))
    end
    local e
    if j == nil then e = -1
    else e = j end
    if e < 0 then e = math.max(0, self:len() - e + 1) end
    if e > self:len() then e = self:len() end
    if e ~= 0 then e = utf8.offset(self.bytestring, e + 1) - 1 end
    return UTFString(string.sub(self.bytestring, utf8.offset(self.bytestring, i), e))
end

return {
    UTFString = UTFString,
    from_latin = from_latin,
    isUTFString = isUTFString,
    isStringWrapper = isStringWrapper,
    wrap_str = StringWrapper
}
