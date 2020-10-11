--[[-
Global functions defined by `bios.lua`. This does not include standard Lua
functions.

@module _G
]]

--[[- Pauses execution for the specified number of seconds.

As it waits for a fixed amount of world ticks, `time` will automatically be
rounded up to the nearest multiple of 0.05 seconds. If you are using coroutines
or the @{parallel|parallel API}, it will only pause execution of the current
thread, not the whole program.

**Note** Because sleep internally uses timers, it is a function that yields.
This means that you can use it to prevent "Too long without yielding" errors,
however, as the minimum sleep time is 0.05 seconds, it will slow your program
down.

**Warning** Internally, this function queues and waits for a timer event (using
@{os.startTimer}), however it does not listen for any other events. This means
that any event that occurs while sleeping will be entirely discarded. If you
need to receive events while sleeping, consider using @{os.startTimer|timers},
or the @{parallel|parallel API}.

@tparam number time The number of seconds to sleep for, rounded up to the
nearest multiple of 0.05.

@see os.startTimer
]]
function sleep(time) end

function write(text) end
function print(...) end
function printError(...) end

function read(replaceChar, history, completeFn, default) end
