-- Definition for the IO API
local typeOf = _G.type

--- If we return nil then close the file, as we've reached the end.
-- We use this weird wrapper function as we wish to preserve the varargs
local function checkResult(handle, ...)
    if ... == nil and handle._autoclose and not handle._closed then handle:close() end
    return ...
end

local handleMetatable
handleMetatable = {
    __name = "FILE*",
    __tostring = function(self)
        if self._closed then
            return "file (closed)"
        else
            local hash = tostring(self._handle):match("table: (%x+)")
            return "file (" .. hash .. ")"
        end
    end,
    __index = {
        close = function(self)
            if typeOf(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. typeOf(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if handle.close then
                self._closed = true
                handle.close()
                return true
            else
                return nil, "attempt to close standard stream"
            end
        end,
        flush = function(self)
            if typeOf(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. typeOf(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if handle.flush then handle.flush() end
        end,
        lines = function(self, ...)
            if typeOf(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. typeOf(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if not handle.read then return nil, "file is not readable" end

            local args = table.pack(...)
            return function() return checkResult(self, self:read(table.unpack(args, 1, args.n))) end
        end,
        read = function(self, ...)
            if typeOf(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. typeOf(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if not handle.read then return nil, "Not opened for reading" end

            local n = select('#', ...)
            local output = {}
            for i = 1, n do
                local arg = select(i, ...)
                local res
                if typeOf(arg) == "number" then
                    if handle.read then res = handle.read(arg) end
                elseif typeOf(arg) == "string" then
                    local format = arg:gsub("^%*", ""):sub(1, 1)

                    if format == "l" then
                        if handle.readLine then res = handle.readLine() end
                    elseif format == "L" and handle.readLine then
                        if handle.readLine then res = handle.readLine(true) end
                    elseif format == "a" then
                        if handle.readAll then res = handle.readAll() or "" end
                    elseif format == "n" then
                        res = nil -- Skip this format as we can't really handle it
                    else
                        error("bad argument #" .. i .. " (invalid format)", 2)
                    end
                else
                    error("bad argument #" .. i .. " (expected string, got " .. typeOf(arg) .. ")", 2)
                end

                output[i] = res
                if not res then break end
            end

            -- Default to "l" if possible
            if n == 0 and handle.readLine then return handle.readLine() end
            return table.unpack(output, 1, n)
        end,
        seek = function(self, whence, offset)
            if typeOf(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. typeOf(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if not handle.seek then return nil, "file is not seekable" end

            -- It's a tail call, so error positions are preserved
            return handle.seek(whence, offset)
        end,
        setvbuf = function(self, mode, size) end,
        write = function(self, ...)
            if typeOf(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. typeOf(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if not handle.write then return nil, "file is not writable" end

            local n = select("#", ...)
            for i = 1, n do handle.write(select(i, ...)) end
            return self
        end,
    },
}

local defaultInput = setmetatable({
    _handle = { readLine = _G.read }
}, handleMetatable)

local defaultOutput = setmetatable({
    _handle = { write = _G.write }
}, handleMetatable)

local defaultError = setmetatable({
    _handle = {
        write = function(...)
            local oldColour
            if term.isColour() then
                oldColour = term.getTextColour()
                term.setTextColour(colors.red)
            end
            _G.write(...)
            if term.isColour() then term.setTextColour(oldColour) end
        end,
    }
}, handleMetatable)

local currentInput = defaultInput
local currentOutput = defaultOutput

stdin = defaultInput
stdout = defaultOutput
stderr = defaultError

function close(_file)
    if _file == nil then return currentOutput:close() end

    if typeOf(_file) ~= "table" or getmetatable(_file) ~= handleMetatable then
        error("bad argument #1 (FILE expected, got " .. typeOf(_file) .. ")", 2)
    end
    return _file:close()
end

function flush()
    return currentOutput:flush()
end

function input(_arg)
    if typeOf(_arg) == "string" then
        local res, err = open(_arg, "rb")
        if not res then error(err, 2) end
        currentInput = res
    elseif typeOf(_arg) == "table" and getmetatable(_arg) == handleMetatable then
        currentInput = _arg
    elseif _arg ~= nil then
        error("bad argument #1 (FILE expected, got " .. typeOf(_arg) .. ")", 2)
    end

    return currentInput
end

function lines(_sFileName)
    if _sFileName ~= nil and typeOf(_sFileName) ~= "string" then
        error("bad argument #1 (expected string, got " .. typeOf(_sFileName) .. ")", 2)
    end
    if _sFileName then
        local ok, err = open(_sFileName, "rb")
        if not ok then error(err, 2) end

        -- We set this magic flag to mark this file as being opened by io.lines and so should be 
        -- closed automatically
        ok._autoclose = true
        return ok:lines()
    else
        return currentInput:lines()
    end
end

function open(_sPath, _sMode)
    if typeOf(_sPath) ~= "string" then
        error("bad argument #1 (expected string, got " .. typeOf(_sPath) .. ")", 2)
    end
    if _sMode ~= nil and typeOf(_sMode) ~= "string" then
        error("bad argument #2 (expected string, got " .. typeOf(_sMode) .. ")", 2)
    end

    local sMode = _sMode and _sMode:gsub("%+", "") or "rb"
    local file, err = fs.open(_sPath, sMode)
    if not file then return nil, err end

    return setmetatable({ _handle = file }, handleMetatable)
end

function output(_arg)
    if typeOf(_arg) == "string" then
        local res, err = open(_arg, "w")
        if not res then error(err, 2) end
        currentOutput = res
    elseif typeOf(_arg) == "table" and getmetatable(_arg) == handleMetatable then
        currentOutput = _arg
    elseif _arg ~= nil then
        error("bad argument #1 (FILE expected, got " .. typeOf(_arg) .. ")", 2)
    end

    return currentOutput
end

function read(...)
    return currentInput:read(...)
end

function type(handle)
    if typeOf(handle) == "table" and getmetatable(handle) == handleMetatable then
        if handle._closed then
            return "closed file"
        else
            return "file"
        end
    end
    return nil
end

function write(...)
    return currentOutput:write(...)
end
