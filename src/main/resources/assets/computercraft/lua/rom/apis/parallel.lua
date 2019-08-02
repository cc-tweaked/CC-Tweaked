local function create( level, ... )
    local tFns = table.pack(...)
    local tCos = {}
    for i = 1, tFns.n, 1 do
        local fn = tFns[i]
        if type( fn ) ~= "function" then
            error( "bad argument #" .. i .. " (expected function, got " .. type( fn ) .. ")", level+3 )
        end

        tCos[i] = coroutine.create(fn)
    end

    return tCos
end

local function runUntilLimit( _routines, _data, _limit )
    local run, count = true, #_routines

    local tFilters = {}
    local eventData = { n = 0 }

    local function checkDead(r, n)
        local living = count
        if r and coroutine.status( r ) == "dead" then
            _routines[n] = nil
        end
        for i = 1, count do
            if _routines[i] == nil then
                living = living-1
            end
        end
        if living <= _limit then
            return n
        end
    end

    while true do
        local buffer = {}
        for i = 1, count do
            buffer[#buffer+1] = _routines[i]
        end
        for n=1,count do
            local r = buffer[n]
            if r then
                if tFilters[r] == nil or tFilters[r] == eventData[1] or eventData[1] == "terminate" then
                    local ok, param = coroutine.resume( r, table.unpack( eventData, 1, eventData.n ) )
                    if not ok then
                        error( param, 0 )
                    else
                        tFilters[r] = param
                    end
                    if _data then
                        local last_count = count
                        run, count = _data()
                        if _limit > 0 then
                            _limit = _limit+(count-last_count)
                        end
                    end
                    local c = checkDead(r, n)
                    if c then return c end
                end
            end
        end
        for n = 1, count do
            local c = checkDead(_routines[n], n)
            if c then return c end
        end
        if not run then
            return
        end
        eventData = table.pack( os.pullEventRaw() )
    end
end

local group = {}

function group:add(...)
    local set = create(1, ...)
    local rfuncs = {}
    for i = 1, #set do
        self._coroutines[self._total+1] = set[i]
        self._total = self._total+1
        local del = self._total
        rfuncs[#rfuncs+1] = function()
            self._coroutines[del] = nil
        end
    end
    return table.unpack(rfuncs)
end

function group:stop()
    self._run = false
end

function group:waitForAll()
    runUntilLimit( self._coroutines, self._data, 0 )
end

function group:waitForAny()
    return runUntilLimit( self._coroutines, self._data, #self._coroutines-1 )
end



function createBundle( ... )
    local inst = setmetatable(
        { 
            _run = true, 
            _coroutines = {},
            _total = 0
        }, 
        { 
            __index = group 
        }
    )
    inst._data = function()
        return inst._run, inst._total
    end
    return inst, inst:add(...)
end

function waitForAny( ... )
    local routines = create(0, ...)
    return runUntilLimit( routines, nil, #routines - 1 )
end

function waitForAll( ... )
    local routines = create(0, ...)
    runUntilLimit( routines, nil, 0 )
end
