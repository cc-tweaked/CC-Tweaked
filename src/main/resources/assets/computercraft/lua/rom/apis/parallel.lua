--- Provides helpers for running multiple tasks in tandem, using
-- @{coroutine|coroutines}.
--
-- @module parallel

local function create( ... )
    local tFns = table.pack(...)
    local tCos = {}
    for i = 1, tFns.n, 1 do
        local fn = tFns[i]
        if type( fn ) ~= "function" then
            error( "bad argument #" .. i .. " (expected function, got " .. type( fn ) .. ")", 3 )
        end

        tCos[i] = coroutine.create(fn)
    end

    return tCos
end

local function runUntilLimit( _routines, _limit )
    local count = #_routines
    local living = count

    local tFilters = {}
    local eventData = { n = 0 }
    while true do
        for n=1,count do
            local r = _routines[n]
            if r then
                if tFilters[r] == nil or tFilters[r] == eventData[1] or eventData[1] == "terminate" then
                    local ok, param = coroutine.resume( r, table.unpack( eventData, 1, eventData.n ) )
                    if not ok then
                        error( param, 0 )
                    else
                        tFilters[r] = param
                    end
                    if coroutine.status( r ) == "dead" then
                        _routines[n] = nil
                        living = living - 1
                        if living <= _limit then
                            return n
                        end
                    end
                end
            end
        end
        for n=1,count do
            local r = _routines[n]
            if r and coroutine.status( r ) == "dead" then
                _routines[n] = nil
                living = living - 1
                if living <= _limit then
                    return n
                end
            end
        end
        eventData = table.pack( os.pullEventRaw() )
    end
end

--- Switches between execution of the functions, until any of them
--- finishes. If any of the functions errors, the message is propagated
--- upwards from the `parallel.waitForAny` call.
--
-- @tparam function ... The functions this task will run
-- @treturn nil
function waitForAny( ... )
    local routines = create( ... )
    return runUntilLimit( routines, #routines - 1 )
end

--- Switches between execution of the functions, until all of them are
--- finished. If any of the functions errors, the message is propagated
--- upwards from the `parallel.waitForAll` call.
--
-- @tparam function ... The functions this task will run
-- @treturn nil
function waitForAll( ... )
    local routines = create( ... )
    return runUntilLimit( routines, 0 )
end
