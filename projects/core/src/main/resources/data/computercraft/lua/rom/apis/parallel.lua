-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- A simple way to run several functions at once.

Functions are not actually executed simultaneously, but rather this API will
automatically switch between them whenever they yield (e.g. whenever they call
[`coroutine.yield`], or functions that call that - such as [`os.pullEvent`] - or
functions that call that, etc - basically, anything that causes the function
to "pause").

Each function executed in "parallel" gets its own copy of the event queue,
and so "event consuming" functions (again, mostly anything that causes the
script to pause - eg [`os.sleep`], [`rednet.receive`], most of the [`turtle`] API,
etc) can safely be used in one without affecting the event queue accessed by
the other.


> [!WARNING]
> When using this API, be careful to pass the functions you want to run in
> parallel, and _not_ the result of calling those functions.
>
> For instance, the following is correct:
>
> ```lua
> local function do_sleep() sleep(1) end
> parallel.waitForAny(do_sleep, rednet.receive)
> ```
>
> but the following is **NOT**:
>
> ```lua
> local function do_sleep() sleep(1) end
> parallel.waitForAny(do_sleep(), rednet.receive)
> ```

@module parallel
@since 1.2
]]

local require = dofile("rom/modules/main/cc/internal/tiny_require.lua")
local expect = require("cc.expect").expect
local exception = require("cc.internal.exception")

--[[- Switches between execution of the functions, until any of them
finishes. If any of the functions errors, the message is propagated upwards
from the [`parallel.waitForAny`] call.

@tparam function ... The functions to run in parallel.
@usage Print a message every second until the `q` key is pressed.

    local function tick()
        while true do
            os.sleep(1)
            print("Tick")
        end
    end
    local function wait_for_q()
        repeat
            local _, key = os.pullEvent("key")
        until key == keys.q
        print("Q was pressed!")
    end

    parallel.waitForAny(tick, wait_for_q)
    print("Everything done!")
]]
function waitForAny(...)
    local barrier_ctx = { co = coroutine.running() }

    local functions = table.pack(...)
    local threads = {}
    for i = 1, functions.n do
        local fn = functions[i]
        expect(i, fn, "function")
        threads[i] = {
            co = coroutine.create(function() return exception.try_barrier(barrier_ctx, fn) end),
            filter = nil,
        }
    end

    local count = functions.n
    if count < 1 then return 0 end

    local event = { n = 0 }
    while true do
        for i = 1, count do
            local thread = threads[i]
            if thread.filter == nil or thread.filter == event[1] or event[1] == "terminate" then
                local ok, param = coroutine.resume(thread.co, table.unpack(event, 1, event.n))
                if not ok then
                    error(exception.wrap_error(param, thread.co), 0)
                end

                -- Abort if this coroutine has finished
                if coroutine.status(thread.co) == "dead" then return i end

                thread.filter = param
            end
        end

        event = table.pack(os.pullEventRaw())
    end
end

--[[-
Runs several functions in parallel, until all of them are finished.

If any of the functions errors, the other functions are not resumed, and the
error is propagated upwards.

> [!WARNING]
>
> While any number of coroutines can be run in parallel, running too many things
> in parallel can sometimes cause issues:
>
>  - Computers only buffer 256 events at a time. Trying to run several hundred
>    functions in parallel (particularly when calling peripheral methods) can
>    cause the event queue to fill up, resulting in events being dropped, and
>    programs getting stuck.
>  - Computers only run 16 HTTP requests at a time. Trying to run more than that
>    in parallel will have no effect.

### Spawning new parallel functions
In some cases, you may want to start running additional functions in parallel
from an existing [`parallel.waitForAll`] call. Every function passed to
[`waitForAll`] can accept a `spawn` argument, which can be called to spawn new
parallel functions.

```lua
parallel.waitForAll(function(spawn)
    spawn(function() sleep(1); print("Finished 1") end)
    spawn(function() sleep(2); print("Finished 2") end)
end)
```

@tparam function(spawn: function(fn: function, any...)) ... The functions to run
in parallel.

@usage Start off two timers and wait for them both to run.

    local function a()
        os.sleep(1)
        print("A is done")
    end
    local function b()
        os.sleep(3)
        print("B is done")
    end

    parallel.waitForAll(a, b)
    print("Everything done!")

@usage Generate a list of functions to run in parallel.

    local funcs = {}
    for i = 1, 5 do
        table.insert(funcs, function()
            sleep(math.random())
            print("Finished " .. i)
        end)
    end

    parallel.waitForAll(table.unpack(funcs))
    print("Everything done!")

@usage Run new functions in parallel from within `waitForAll`.

    parallel.waitForAll(function(spawn)
        for i = 1, 5 do
            spawn(function()
                sleep(math.random())
                print("Finished " .. i)
            end)
        end
    end)
    print("Everything done!")

@changed 1.120.0 Added ability to spawn new parallel functions.
]]
function waitForAll(...)
    local barrier_ctx = { co = coroutine.running() }

    local can_spawn, threads, count = false, {}, 0

    local function spawn(fn, ...)
        expect(1, fn, "function")

        threads[count + 1] = {
            co = coroutine.create(function(...) return exception.try_barrier(barrier_ctx, fn, ...) end),
            filter = nil,
            resume_with = table.pack(...),
        }
        count = count + 1
    end

    local function safe_spawn(fn, ...)
        if not can_spawn then error("Cannot spawn new coroutines outside of waitForAll", 2) end
        return spawn(fn, ...)
    end

    local functions = table.pack(...)
    for i = 1, functions.n, 1 do
        local fn = functions[i]
        expect(i, fn, "function")
        spawn(fn, safe_spawn)
    end

    local event = { n = 0 }
    while true do
        local i = 1
        while i <= count do
            local thread = threads[i]

            -- If this is a new coroutine, start it with the "resume_with" data,
            -- otherwise resume it with the event (if it matches).
            local resume_with
            if thread.resume_with then
                resume_with = thread.resume_with
                thread.resume_with = nil
            elseif thread.filter == nil or thread.filter == event[1] or event[1] == "terminate" then
                resume_with = event
            end

            if resume_with then
                can_spawn = true
                local ok, param = coroutine.resume(thread.co, table.unpack(resume_with, 1, resume_with.n))
                can_spawn = false

                if not ok then
                    error(exception.wrap_error(param, thread.co), 0)
                end

                if coroutine.status(thread.co) == "dead" then
                    -- If this thread has died, remove it and repeat this
                    -- iteration.
                    table.remove(threads, i)
                    i, count = i - 1, count - 1
                end

                thread.filter = param
            end

            i = i + 1
        end

        if count == 0 then return end

        event = table.pack(os.pullEventRaw())
    end
end
