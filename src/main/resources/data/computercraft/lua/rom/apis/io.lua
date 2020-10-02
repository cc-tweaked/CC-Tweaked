--- Emulates Lua's standard [io library][io].
--
-- [io]: https://www.lua.org/manual/5.1/manual.html#5.7
--
-- @module io

local expect, type_of = dofile("rom/modules/main/cc/expect.lua").expect, _G.type

--- If we return nil then close the file, as we've reached the end.
-- We use this weird wrapper function as we wish to preserve the varargs
local function checkResult(handle, ...)
    if ... == nil and handle._autoclose and not handle._closed then handle:close() end
    return ...
end

--- A file handle which can be read or written to.
--
-- @type Handle
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
        --- Close this file handle, freeing any resources it uses.
        --
        -- @treturn[1] true If this handle was successfully closed.
        -- @treturn[2] nil If this file handle could not be closed.
        -- @treturn[2] string The reason it could not be closed.
        -- @throws If this handle was already closed.
        close = function(self)
            if type_of(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. type_of(self) .. ")", 2)
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

        --- Flush any buffered output, forcing it to be written to the file
        --
        -- @throws If the handle has been closed
        flush = function(self)
            if type_of(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. type_of(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if handle.flush then handle.flush() end
            return true
        end,

        lines = function(self, ...)
            if type_of(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. type_of(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if not handle.read then return nil, "file is not readable" end

            local args = table.pack(...)
            return function()
                if self._closed then error("file is already closed", 2) end
                return checkResult(self, self:read(table.unpack(args, 1, args.n)))
            end
        end,

        read = function(self, ...)
            if type_of(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. type_of(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if not handle.read and not handle.readLine then return nil, "Not opened for reading" end

            local n = select("#", ...)
            local output = {}
            for i = 1, n do
                local arg = select(i, ...)
                local res
                if type_of(arg) == "number" then
                    if handle.read then res = handle.read(arg) end
                elseif type_of(arg) == "string" then
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
                    error("bad argument #" .. i .. " (expected string, got " .. type_of(arg) .. ")", 2)
                end

                output[i] = res
                if not res then break end
            end

            -- Default to "l" if possible
            if n == 0 and handle.readLine then return handle.readLine() end
            return table.unpack(output, 1, n)
        end,

        seek = function(self, whence, offset)
            if type_of(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. type_of(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if not handle.seek then return nil, "file is not seekable" end

            -- It's a tail call, so error positions are preserved
            return handle.seek(whence, offset)
        end,

        setvbuf = function(self, mode, size) end,

        --- Write one or more values to the file
        --
        -- @tparam string|number ... The values to write.
        -- @treturn[1] Handle The current file, allowing chained calls.
        -- @treturn[2] nil If the file could not be written to.
        -- @treturn[2] string The error message which occurred while writing.
        write = function(self, ...)
            if type_of(self) ~= "table" or getmetatable(self) ~= handleMetatable then
                error("bad argument #1 (FILE expected, got " .. type_of(self) .. ")", 2)
            end
            if self._closed then error("attempt to use a closed file", 2) end

            local handle = self._handle
            if not handle.write then return nil, "file is not writable" end

            for i = 1, select("#", ...) do
                local arg = select(i, ...)
                expect(i, arg, "string", "number")
                handle.write(arg)
            end
            return self
        end,
    },
}

local defaultInput = setmetatable({
    _handle = { readLine = _G.read },
}, handleMetatable)

local defaultOutput = setmetatable({
    _handle = { write = _G.write },
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
    },
}, handleMetatable)

local currentInput = defaultInput
local currentOutput = defaultOutput

--- A file handle representing the "standard input". Reading from this
-- file will prompt the user for input.
stdin = defaultInput

--- A file handle representing the "standard output". Writing to this
-- file will display the written text to the screen.
stdout = defaultOutput

--- A file handle representing the "standard error" stream.
--
-- One may use this to display error messages, writing to it will display
-- them on the terminal.
stderr = defaultError

--- Closes the provided file handle.
--
-- @tparam[opt] Handle file The file handle to close, defaults to the
-- current output file.
--
-- @see Handle:close
-- @see io.output
function close(file)
    if file == nil then return currentOutput:close() end

    if type_of(file) ~= "table" or getmetatable(file) ~= handleMetatable then
        error("bad argument #1 (FILE expected, got " .. type_of(file) .. ")", 2)
    end
    return file:close()
end

--- Flushes the current output file.
--
-- @see Handle:flush
-- @see io.output
function flush()
    return currentOutput:flush()
end

--- Get or set the current input file.
--
-- @tparam[opt] Handle|string file The new input file, either as a file path or pre-existing handle.
-- @treturn Handle The current input file.
-- @throws If the provided filename cannot be opened for reading.
function input(file)
    if type_of(file) == "string" then
        local res, err = open(file, "rb")
        if not res then error(err, 2) end
        currentInput = res
    elseif type_of(file) == "table" and getmetatable(file) == handleMetatable then
        currentInput = file
    elseif file ~= nil then
        error("bad fileument #1 (FILE expected, got " .. type_of(file) .. ")", 2)
    end

    return currentInput
end

--- Opens the given file name in read mode and returns an iterator that,
-- each time it is called, returns a new line from the file.
--
-- This can be used in a for loop to iterate over all lines of a file:
--
-- ```lua
-- for line in io.lines(filename) do print(line) end
-- ```
--
-- Once the end of the file has been reached, @{nil} will be
-- returned. The file is automatically closed.
--
-- If no file name is given, the @{io.input|current input} will be used
-- instead. In this case, the handle is not used.
--
-- @tparam[opt] string filename The name of the file to extract lines from
-- @param ... The argument to pass to @{Handle:read} for each line.
-- @treturn function():string|nil The line iterator.
-- @throws If the file cannot be opened for reading
--
-- @see Handle:lines
-- @see io.input
function lines(filename, ...)
    expect(1, filename, "string", "nil")
    if filename then
        local ok, err = open(filename, "rb")
        if not ok then error(err, 2) end

        -- We set this magic flag to mark this file as being opened by io.lines and so should be
        -- closed automatically
        ok._autoclose = true
        return ok:lines(...)
    else
        return currentInput:lines(...)
    end
end

--- Open a file with the given mode, either returning a new file handle
-- or @{nil}, plus an error message.
--
-- The `mode` string can be any of the following:
--  - **"r"**: Read mode
--  - **"w"**: Write mode
--  - **"a"**: Append mode
--
-- The mode may also have a `b` at the end, which opens the file in "binary
-- mode". This allows you to read binary files, as well as seek within a file.
--
-- @tparam string filename The name of the file to open.
-- @tparam[opt] string mode The mode to open the file with. This defaults to `rb`.
-- @treturn[1] Handle The opened file.
-- @treturn[2] nil In case of an error.
-- @treturn[2] string The reason the file could not be opened.
function open(filename, mode)
    expect(1, filename, "string")
    expect(2, mode, "string", "nil")

    local sMode = mode and mode:gsub("%+", "") or "rb"
    local file, err = fs.open(filename, sMode)
    if not file then return nil, err end

    return setmetatable({ _handle = file }, handleMetatable)
end

--- Get or set the current output file.
--
-- @tparam[opt] Handle|string file The new output file, either as a file path or pre-existing handle.
-- @treturn Handle The current output file.
-- @throws If the provided filename cannot be opened for writing.
function output(file)
    if type_of(file) == "string" then
        local res, err = open(file, "wb")
        if not res then error(err, 2) end
        currentOutput = res
    elseif type_of(file) == "table" and getmetatable(file) == handleMetatable then
        currentOutput = file
    elseif file ~= nil then
        error("bad argument #1 (FILE expected, got " .. type_of(file) .. ")", 2)
    end

    return currentOutput
end

--- Read from the currently opened input file.
--
-- This is equivalent to `io.input():read(...)`. See @{Handle:read|the
-- documentation} there for full details.
--
-- @tparam string ... The formats to read, defaulting to a whole line.
-- @treturn (string|nil)... The data read, or @{nil} if nothing can be read.
function read(...)
    return currentInput:read(...)
end

--- Checks whether `handle` is a given file handle, and determine if it is open
-- or not.
--
-- @param obj The value to check
-- @treturn string|nil `"file"` if this is an open file, `"closed file"` if it
-- is a closed file handle, or `nil` if not a file handle.
function type(obj)
    if type_of(obj) == "table" and getmetatable(obj) == handleMetatable then
        if obj._closed then
            return "closed file"
        else
            return "file"
        end
    end
    return nil
end

--- Write to the currently opened output file.
--
-- This is equivalent to `io.output():write(...)`. See @{Handle:write|the
-- documentation} there for full details.
--
-- @tparam string ... The strings to write
function write(...)
    return currentOutput:write(...)
end
