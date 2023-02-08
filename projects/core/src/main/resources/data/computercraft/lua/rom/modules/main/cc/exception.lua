--[[- Capture stack traces from errors.

When a Lua program crashes, either due to a bug or an explicit call to @{error},
the only information returned to the user is a string representation of the
error message. This can make it difficult to accurately capture information
about the original error, such as the stack trace of the error.

This module provides a more structured way of working with errors, allowing the
original context of an error to be inspected.

## A motivating example
Let's start with a basic program which does nothing but crash. When you run
this, ComputerCraft's shell is able to show you exactly where the error
occurred.

```lua
error("I hit a problem")
```

However, imagine that we want to run some code and then do some cleanup
afterwards, such as closing a file or clearing the terminal. To do this, we
might use @{pcall} to catch an error, and then report it to the user afterwards.

```lua
local ok, err = pcall(function()
    error("I hit a problem")
end)

if not ok then
    print("Program crashed!")
    error(err)
end
```

If we run this, the error is now reported in the wrong place - any information
about the original error has been lost! To avoid this, we can replace our call
to @{pcall} with one to @{try}. This converts our original error into an
@{Exception}, meaning the context of the original error is not lost.

```lua
local exception = require "cc.exception"
local ok, err = exception.try(function() -- Use try instead of pcall
    error("I hit a problem")
end)

if not ok then
    print("Program crashed!")
    error(err)
end
```

## Non-string errors
While Lua's built-in errors are represented as strings, other programs or
libraries can throw non-string errors (indeed, that's exactly what
@{cc.exception} does).

These errors are _not_ wrapped into an @{Exception} by the functions in this
library (i.e. @{try} and @{wrap_error}) and instead are returned
untransformed. You should always use @{is_exception} to check if an error is a
valid exception before inspecting it.

@since 1.103.0
]]

local expect = require and require "cc.expect".expect or function() end

--[[- The basic exception object.

This is a table with the following properties:
 - `message`: The error message string.
 - `thread`: The @{coroutine} which threw this error.
 - `root_cause`: The root @{Exception}.

@type Exception
]]
local mt = {
    __name = "exception",
    __tostring = function(self) return self.message end,
    __index = {},
}

--[[- Check whether this object is a valid exception.

@param exn The object which may be an exception.
@treturn boolean Whether this is an exception.
]]
local function is_exception(exn)
    if type(exn) ~= "table" then return false end

    local mt = getmetatable(exn)
    return mt and mt.__name == mt.__name
        and type(exn.message) == "string" and type(exn.thread) == "thread"
end

--[[- Optionally wrap an error into an exception.

@param message The error message. Typically a string or @{Exception}, but may
  be any object.
@tparam coroutine traceback The traceback message. Either the erroring
  coroutine, or a traceback string provided by @{debug.traceback}.
]]
local function wrap_error(message, thread)
    expect(2, thread, "thread")

    if type(message) == "string" and message ~= "" then
        local exn = setmetatable({ message = message, thread = thread }, mt)
        exn.root_cause = exn
        return exn
    elseif is_exception(message) then
        return setmetatable({
            message = message.message, thread = thread,
            root_cause = message.root_cause, cause = message,
        }, mt)
    else
        return message
    end
end

--[[- Attempt to call the provided function `func` with the provided arguments.

Like @{pcall}, this returns whether the function ran successfully and, if not,
its accompanying error. Unlike @{pcall}, the error will be converted into an
exception if possible.

@tparam function func The function to call.
@param ... Arguments to this function.

@treturn[1] true If the function ran successfully.
@return[1] ... The return values of the function.

@treturn[2] false If the function failed.
@return[2] The error object.
]]
local function try(func, ...)
    expect(1, func, "function")

    local co = coroutine.create(func)
    local result = table.pack(coroutine.resume(co, ...))

    while coroutine.status(co) ~= "dead" do
        local event = table.pack(os.pullEventRaw(result[2]))
        if result[2] == nil or event[1] == result[2] or event[1] == "terminate" then
            result = table.pack(coroutine.resume(co, table.unpack(event, 1, event.n)))
        end
    end

    if not result[1] then return false, wrap_error(result[2], co) end
    return table.unpack(result, 1, result.n)
end

return {
    is_exception = is_exception,
    wrap_error = wrap_error,
    try = try,
}
