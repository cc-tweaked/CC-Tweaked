local function log(msg)
    print(msg)
    if assertion then assertion.log(msg) end
end

local function run(name, n, f, ...)
    sleep(0)
    local s = os.epoch("utc")
    for _ = 1, n do f(...) end
    local e = os.epoch("utc") - s
    log(("%10s %.2fs %.fop/s"):format(name, e*1e-3, n/e))
end

local function run5(...) for _ = 1, 5 do run(...) end end

local native = term.native()
local x, y = native.getCursorPos()

log("Starting the benchmark")
run5("redstone.getAnalogInput", 1e7, redstone.getAnalogInput, "top")
run5("term.getCursorPos", 2e7, native.getCursorPos)
run5("term.setCursorPos", 2e7, native.setCursorPos, x, y)

if assertion then assertion.assert(true) end
