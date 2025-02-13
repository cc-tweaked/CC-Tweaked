-- SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- Internal tools for working with errors.

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

@local
]]

local expect = require "cc.expect".expect
local type, debug, coroutine = type, debug, coroutine

local function find_frame(thread, file, line)
    -- Scan the first 16 frames for something interesting.
    for offset = 0, 15 do
        local frame = debug.getinfo(thread, offset, "Sl")
        if not frame then break end

        if frame.short_src == file and frame.what ~= "C" and frame.currentline == line then
            return offset, frame
        end
    end
end

--[[- Check whether this error is an exception.

Currently we don't provide a stable API for throwing (and propagating) rich
errors, like those supported by this module. In lieu of that, we describe the
exception protocol, which may be used by user-written coroutine managers to
throw exceptions which are pretty-printed by the shell:

An exception is any table with:
 - The `"exception"` type
 - A string `message` field,
 - And a coroutine `thread` fields.

To throw such an exception, the inner loop of your coroutine manager may look
something like this:

```lua
local ok, result = coroutine.resume(co, table.unpack(event, 1, event.n))
if not ok then
    -- Rethrow non-string errors directly
    if type(result) ~= "string" then error(result, 0) end
    -- Otherwise, wrap it into an exception.
    error(setmetatable({ message = result, thread = co }, {
        __name = "exception",
        __tostring = function(self) return self.message end,
    }))
end
```

@param exn Some error object
@treturn boolean Whether this error is an exception.
]]
local function is_exception(exn)
    if type(exn) ~= "table" then return false end

    local mt = getmetatable(exn)
    return mt and mt.__name == "exception" and type(rawget(exn, "message")) == "string" and type(rawget(exn, "thread")) == "thread"
end

local exn_mt = {
    __name = "exception",
    __tostring = function(self) return self.message end,
}

--[[- Create a new exception from a message and thread.

@tparam string message The exception message.
@tparam coroutine thread The coroutine the error occurred on.
@return The constructed exception.
]]
local function make_exception(message, thread)
    return setmetatable({ message = message, thread = thread }, exn_mt)
end

--[[- A marker function for [`try`] and the wider exception machinery.

This function is typically the first function on the call stack. It acts as both
a signifier that this function is exception aware, and allows us to store
additional information for the exception machinery on the call stack.

@see can_wrap_errors
]]
local try_barrier = debug.getregistry().cc_try_barrier
if not try_barrier then
    -- We define an extra "bounce" function to prevent f(...) being treated as a
    -- tail call, and so ensure the barrier remains on the stack.
    local function bounce(...) return ... end

    --- @tparam { co = coroutine, can_wrap ?= boolean } parent The parent coroutine.
    -- @tparam function f The function to call.
    -- @param ... The arguments to this function.
    try_barrier = function(parent, f, ...) return bounce(f(...)) end

    debug.getregistry().cc_try_barrier = try_barrier
end

-- Functions that act as a barrier for exceptions.
local pcall_functions = { [pcall] = true, [xpcall] = true, [load] = true }

--[[- Check to see whether we can wrap errors into an exception.

This scans the current thread (up to a limit), and any parent threads, to
determine if there is a pcall anywhere on the callstack. If not, then we know
the error message is not observed by user code, and so may be wrapped into an
exception.

@tparam[opt] coroutine The thread to check. Defaults to the current thread.
@treturn boolean Whether we can wrap errors into exceptions.
]]
local function can_wrap_errors(thread)
    if not thread then thread = coroutine.running() end

    for offset = 0, 31 do
        local frame = debug.getinfo(thread, offset, "f")
        if not frame then return false end

        local func = frame.func
        if func == try_barrier then
            -- If we've a try barrier, then extract the parent coroutine and
            -- check if it can wrap errors.
            local _, parent = debug.getlocal(thread, offset, 1)
            if type(parent) ~= "table" or type(parent.co) ~= "thread" then return false end

            local result = parent.can_wrap
            if result == nil then
                result = can_wrap_errors(parent.co)
                parent.can_wrap = result
            end

            return result
        elseif pcall_functions[func] then
            -- If we're a pcall, then abort.
            return false
        end
    end

    return false
end

--[[- Attempt to call the provided function `func` with the provided arguments.

@tparam function func The function to call.
@param ... Arguments to this function.

@treturn[1] true If the function ran successfully.
@return[1] ... The return values of the function.

@treturn[2] false If the function failed.
@return[2] The error message
@treturn[2] coroutine The thread where the error occurred.
]]
local function try(func, ...)
    expect(1, func, "function")

    local co = coroutine.create(try_barrier)
    local result = table.pack(coroutine.resume(co, { co = co, can_wrap = true }, func, ...))

    while coroutine.status(co) ~= "dead" do
        local event = table.pack(os.pullEventRaw(result[2]))
        if result[2] == nil or event[1] == result[2] or event[1] == "terminate" then
            result = table.pack(coroutine.resume(co, table.unpack(event, 1, event.n)))
        end
    end

    if result[1] then
        return table.unpack(result, 1, result.n)
    elseif is_exception(result[2]) then
        local exn = result[2]
        return false, rawget(exn, "message"), rawget(exn, "thread")
    else
        return false, result[2], co
    end
end

--[[- Report additional context about an error.

@param err The error to report.
@tparam coroutine thread The coroutine where the error occurred.
@tparam[opt] { [string] = string } source_map Map of chunk names to their contents.
]]
local function report(err, thread, source_map)
    expect(2, thread, "thread")
    expect(3, source_map, "table", "nil")

    if type(err) ~= "string" then return end

    local file, line, err = err:match("^([^:]+):(%d+): (.*)")
    if not file then return end
    line = tonumber(line)

    local frame_offset, frame = find_frame(thread, file, line)
    if not frame or not frame.currentcolumn then return end

    local column = frame.currentcolumn
    local line_contents
    if source_map and source_map[frame.source] then
        -- File exists in the source map.
        local pos, contents = 1, source_map[frame.source]
        -- Try to remap our position. The interface for this only makes sense
        -- for single line sources, but that's sufficient for where we need it
        -- (the REPL).
        if type(contents) == "table" then
            column = column - contents.offset
            contents = contents.contents
        end

        for _ = 1, line - 1 do
            local next_pos = contents:find("\n", pos)
            if not next_pos then return end
            pos = next_pos + 1
        end

        local end_pos = contents:find("\n", pos)
        line_contents = contents:sub(pos, end_pos and end_pos - 1 or #contents)

    elseif frame.source:sub(1, 2) == "@/" then
        -- Read the file from disk.
        local handle = fs.open(frame.source:sub(3), "r")
        if not handle then return end
        for _ = 1, line - 1 do handle.readLine() end

        line_contents = handle.readLine()
    end

    -- Could not determine the line. Bail.
    if not line_contents or #line_contents == "" then return end

    require("cc.internal.error_printer")({
        get_pos = function() return line, column end,
        get_line = function() return line_contents end,
    }, {
        { tag = "annotate", start_pos = column, end_pos = column, msg = "" },
        require "cc.internal.error_hints".get_tip(err, thread, frame_offset),
    })
end


return {
    make_exception = make_exception,

    try_barrier = try_barrier,
    can_wrap_errors = can_wrap_errors,

    try = try,
    report = report,
}
