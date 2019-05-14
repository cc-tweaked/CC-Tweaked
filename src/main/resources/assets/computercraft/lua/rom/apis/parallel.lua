
local function create( ... )
    local tFns = table.pack(...)
    local tCos = {}
    for i = 1, tFns.n, 1 do
        local fn = tFns[i]
        expect(i, fn, "function")

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

function waitForAny( ... )
    local routines = create( ... )
    return runUntilLimit( routines, #routines - 1 )
end

function waitForAll( ... )
    local routines = create( ... )
    runUntilLimit( routines, 0 )
end
