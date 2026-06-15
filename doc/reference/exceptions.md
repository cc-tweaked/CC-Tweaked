---
module: [kind=reference] exceptions
---

<!--
SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# CraftOS's exception protocol
By default, Lua represents errors are plain strings. The file name and line number may be perpended to the error message
(e.g. `example.lua:12: error message`), but all other information (e.g. stack trace) is lost.

In order to preserve this information across APIs which support catching errors ([`pcall`], [`coroutine.resume`]),
CraftOS supports a richer form of errors that capture the context they were thrown in, referred to as
"exceptions". These exceptions can be created by coroutine managers (such as [`parallel`]) to preserve error information
across coroutine boundaries, allowing the shell and Lua REPL to display the precise source and location of the error.

An exception is defined as a table with:
 - A `message` field of type [`string`].
 - A `thread` field, of type [`thread`][`coroutine`].
 - A metatable, with a `__name = "exception"` field.

## Example
As an example, let's consider the simplest coroutine manager, that just spawns a new coroutine and forwards events to
it. When we error inside the coroutine, notice that shell does not display a rich error, as the stack trace information
is lost!

```lua {data-snippet=run_basic data-no-run=1}
local function run(fn, ...)
  local co = coroutine.create(fn)
  local ok, result = coroutine.resume(co, ...)
  while coroutine.status(co) ~= "dead" do
    local event = table.pack(os.pullEventRaw())
    if result == nil or filter == "terminated" or event[1] == filter then
      ok, result = table.pack(coroutine.resume(co, table.unpack(event, 1, event.n)))
    end
  end

  if not ok then error(result, 0) end
end

return run
```

```lua {data-mount=run_basic:run.lua}
local run = require "run"
run(function()
  error("ohno")
end)
```

We can fix this by updating our coroutine manager to throw an exception when an error occurs. The shell now prints a
rich error on exit.

```lua {data-snippet=run_exn data-no-run=1}
-- NEW: Define our exception metatable.
local exception_mt = {
  __name = "exception",
  __tostring = function(self) return self.message end
}

local function run(fn, ...)
  local co = coroutine.create(fn)
  local ok, result = coroutine.resume(co, ...)
  while coroutine.status(co) ~= "dead" do
    local event = table.pack(os.pullEventRaw())
    if result == nil or filter == "terminated" or event[1] == filter then
      ok, result = table.pack(coroutine.resume(co, table.unpack(event, 1, event.n)))
    end
  end

  if not ok then
    -- NEW: If we have a string error, wrap that into an exception instead
    if type(result) == "string" then
      error(setmetatable({ message = result, thread = co }, exception_mt))
    else
      error(result, 0)
    end
  end
end

return run
```

```lua {data-mount=run_exn:run.lua}
local run = require "run"
run(function()
  error("ohno")
end)
```


## [`parallel`] and exceptions
One issue you may find with the above pattern is exceptions are *not* thrown by [`parallel`] functions. For instance,
this program has the same issue as before, and does not print a rich error.

```lua {data-mount=run_exn:run.lua}
local run = require "run"
run(function()
  parallel.waitForAny(function()
    error("ohno")
  end)
end)
```

This is done by default to preserve the backwards compatibility of CraftOS. Some user code catches errors thrown from
within [`parallel`] functions, and inspects the errors, and trying to convert those errors into exceptions will break
that code. However, if you run a this code *without* our `run` function, you'll notice the rich error is displayed:

```lua
parallel.waitForAny(function()
  error("ohno")
end)
```

Internally, [`parallel`] functions attempt to determine whether an error message is captured by user code (with
[`pcall`]/[`xpcall`] or [`coroutine.resume`]). If the error is never observed, then it's safe to wrap it into an
exception!

What we're seeing here is that [`parallel`] doesn't know anything about our coroutine manager, and so assumes it's not
safe to throw an exception.

This can be fixed by making the first function in our child coroutine the magic `debug.getregistry().try_barrier`
function. This function:
 - Accepts a "context", the function to call, and the function's arguments as parameters, then immediately calls the
   function.
 - The "context" is a table with:
   - A `co` field, containing the parent coroutine.
   - An optional `can_wrap` field, indicating whether exceptions can be wrapped or not.

Support for this in our coroutine manager looks as follows:

```lua {data-snippet=run_barrier data-no-run=1}
-- NEW: Define our magic try_barrier function:
local try_barrier = debug.getregistry().cc_try_barrier
if not try_barrier then
    local function bounce(...) return ... end
    try_barrier = function(parent, f, ...) return bounce(f(...)) end
    debug.getregistry().cc_try_barrier = try_barrier
end

local exception_mt = {
  __name = "exception",
  __tostring = function(self) return self.message end
}

local function run(fn, ...)
  -- NEW: Start our coroutine using the try_barrier function instead. We use
  -- { can_wrap = true } to tell parallel that it can always wrap errors into
  -- exceptions.
  local co = coroutine.create(try_barrier)
  local ok, result = coroutine.resume(co, { co = co, can_wrap = true }, fn, ...)

  while coroutine.status(co) ~= "dead" do
    local event = table.pack(os.pullEventRaw())
    if result == nil or filter == "terminated" or event[1] == filter then
      ok, result = table.pack(coroutine.resume(co, table.unpack(event, 1, event.n)))
    end
  end

  if not ok then
    if type(result) == "string" then
      error(setmetatable({ message = result, thread = co }, exception_mt))
    else
      error(result, 0)
    end
  end
end

return run
```

```lua {data-mount=run_barrier:run.lua}
local run = require "run"
run(function()
  parallel.waitForAny(function()
    error("ohno")
  end)
end)
```
