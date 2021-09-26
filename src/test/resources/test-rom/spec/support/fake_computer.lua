local function keys(tbl)
    local keys = {}
    for k in pairs(tbl) do keys[#keys + 1] = k end
    return keys
end

local safe_globals = {
    "assert", "bit32", "coroutine", "debug", "error", "fs", "getmetatable", "io", "ipairs", "math", "next", "pairs",
    "pcall", "print", "printError", "rawequal", "rawget", "rawlen", "rawset", "select", "setmetatable", "string",
    "table", "term", "textutils", "tonumber", "tostring", "type", "utf8", "xpcall",
}

--- Create a fake computer.
local function make_computer(id, fn)
    local env = setmetatable({}, _G)

    local peripherals = {}
    local pending_timers, next_timer, clock = {}, 0, 0
    local events = { { n = 1, env } }
    local function queue_event(...) events[#events + 1] = table.pack(...) end

    for _, k in pairs(safe_globals) do env[k] = _G[k] end
    env.peripheral = {
        getNames = function() return keys(peripherals) end,
        isPresent = function(name) return peripherals[name] ~= nil end,
        getType = function(name) return peripherals[name] and getmetatable(peripherals[name]).type end,
        getMethods = function(name) return peripherals[name] and keys(peripherals[name]) end,
        call = function(name, method, ...)
            local p = peripherals[name]
            if p then return p[method](...) end
            return nil
        end,
        wrap = function(name) return peripherals[name] end,
    }
    env.os = {
        getComputerID = function() return id end,
        queueEvent = queue_event,
        pullEventRaw = coroutine.yield,
        pullEvent = function(filter)
            local event_data = table.pack(coroutine.yield(filter))
            if event_data[1] == "terminate" then error("Terminated", 0) end
            return table.unpack(event_data, 1, event_data.n)
        end,
        startTimer = function(delay)
            local t = next_timer
            pending_timers[t], next_timer = clock + delay, next_timer + 1
            return t
        end,
        clock = function() return clock end,
        sleep = function(time)
            local timer = env.os.startTimer(time or 0)
            repeat local _, id = env.os.pullEvent("timer") until id == timer
        end,
    }
    env.sleep = env.os.sleep
    env.dofile = function(path)
        local fn, err = loadfile(path, nil, env)
        if fn then return fn() else error(err, 2) end
    end

    local co = coroutine.create(fn)
    local filter = nil
    local function step()
        while true do
            if #events == 0 or coroutine.status(co) == "dead" then return false end

            local ev = table.remove(events, 1)
            if filter == nil or ev[1] == filter or ev[1] == "terminated" then
                local ok, result = coroutine.resume(co, table.unpack(ev, 1, ev.n))
                if not ok then
                    if type(result) == "table" and result.trace == nil then result.trace = debug.traceback(co) end
                    error(result, 0)
                end
                filter = result
                return true
            end
        end
    end

    local function advance(dt)
        clock = clock + dt
        for id, clk in pairs(pending_timers) do
            if clk <= clock then
                queue_event("timer", id)
                pending_timers[id] = nil
            end
        end
    end

    return { env = env, peripherals = peripherals, queue_event = queue_event, step = step, co = co, advance = advance }
end

local function parse_channel(c)
    if c < 0 or c > 65535 then error("Expected number in range 0-65535", 3) end
    return c
end

--- Add a modem to a computer on a particular side
local function add_modem(owner, side)
    local open, adjacent = {}, {}
    local peripheral = setmetatable({
        open = function(channel) open[parse_channel(channel)] = true end,
        close = function(channel) open[parse_channel(channel)] = false end,
        closeAll = function(channel) open = {} end,
        isOpen = function(channel) return open[parse_channel(channel)] == true end,
        transmit = function(channel, reply_channel, payload)
            channel, reply_channel = parse_channel(channel), parse_channel(reply_channel)

            for _, adjacent in pairs(adjacent) do
                if adjacent.open[channel] then
                    adjacent.owner.queue_event("modem_message", adjacent.side, channel, reply_channel, payload, 123)
                end
            end
        end,
    }, { type = "modem" })
    owner.peripherals[side] = peripheral
    return { adjacent = adjacent, side = side, owner = owner, open = open }
end

local function add_modem_edge(modem1, modem2)
    table.insert(modem1.adjacent, modem2)
    table.insert(modem2.adjacent, modem1)
end

--- Load an API into the computer's environment.
local function add_api(computer, path)
    local name = fs.getName(path)
    if name:sub(-4) == ".lua" then name = name:sub(1, -5) end

    local child_env = {}
    setmetatable(child_env, { __index = computer.env })
    assert(loadfile(path, nil, child_env))()

    local api = {}
    for k, v in pairs(child_env) do api[k] = v end

    computer.env[name] = api
end

--- Step all computers forward by one event.
local function step_all(computers)
    local any = false
    for _, computer in pairs(computers) do
        if computer.step() then any = true end
    end
    return any
end

--- Run all computers until their event queue is empty.
local function run_all(computers, require_done)
    while step_all(computers) do end

    if require_done ~= false then
        if type(require_done) == "table" then
            for _, v in ipairs(require_done) do require_done[v] = true end
        end

        for _, computer in pairs(computers) do
            if coroutine.status(computer.co) ~= "dead" and (type(require_done) ~= "table" or require_done[computer]) then
                error(debug.traceback(computer.co, ("Computer #%d did not shutdown"):format(computer.env.os.getComputerID())), 0)
            end
        end
    end
end

--- Advance all computers by a given time.
local function advance_all(computers, dt)
    for _, computer in pairs(computers) do computer.advance(dt) end
end

return {
    make_computer = make_computer,
    add_modem = add_modem,
    add_modem_edge = add_modem_edge,
    add_api = add_api,

    step_all = step_all,
    run_all = run_all,
    advance_all = advance_all,
}
