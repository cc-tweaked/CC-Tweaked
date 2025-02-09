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

local exception = dofile("rom/modules/main/cc/internal/tiny_require.lua")("cc.internal.exception")

local function create(...)
    local barrier_ctx = { co = coroutine.running() }

    local functions = table.pack(...)
    local threads = {}
    for i = 1, functions.n, 1 do
        local fn = functions[i]
        if type(fn) ~= "function" then
            error("bad argument #" .. i .. " (function expected, got " .. type(fn) .. ")", 3)
        end

        threads[i] = { co = coroutine.create(function() return exception.try_barrier(barrier_ctx, fn) end), filter = nil }
    end

    return threads
end

local function runUntilLimit(threads, limit)
    local count = #threads
    if count < 1 then return 0 end
    local living = count

    local event = { n = 0 }
    while true do
        for i = 1, count do
            local thread = threads[i]
            if thread and (thread.filter == nil or thread.filter == event[1] or event[1] == "terminate") then
                local ok, param = coroutine.resume(thread.co, table.unpack(event, 1, event.n))
                if ok then
                    thread.filter = param
                elseif type(param) == "string" and exception.can_wrap_errors() then
                    error(exception.make_exception(param, thread.co))
                else
                    error(param, 0)
                end

                if coroutine.status(thread.co) == "dead" then
                    threads[i] = false
                    living = living - 1
                    if living <= limit then
                        return i
                    end
                end
            end
        end

        event = table.pack(os.pullEventRaw())
    end
end

--[[- Switches between execution of the functions, until any of them
finishes. If any of the functions errors, the message is propagated upwards
from the [`parallel.waitForAny`] call.

@tparam function ... The functions this task will run
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
    local threads = create(...)
    return runUntilLimit(threads, #threads - 1)
end

--[[- Switches between execution of the functions, until all of them are
finished. If any of the functions errors, the message is propagated upwards
from the [`parallel.waitForAll`] call.

@tparam function ... The functions this task will run
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
]]
function waitForAll(...)
    local threads = create(...)
    return runUntilLimit(threads, 0)
end
