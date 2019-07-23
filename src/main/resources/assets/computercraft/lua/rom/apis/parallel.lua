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

local external = {
    __index = {
        add = function(bundle, ...)
            local set = create(1, ...)
            local rfuncs = {}
            for i = 1, #set do
                bundle[#bundle+1] = set[i]
                rfuncs[#rfuncs+1] = function()
                    local del = #bundle
                    return table.remove(bundle, del) and true
                end
            end
            return table.unpack(rfuncs)
        end,
        stop = function(bundle)
            bundle.data.run = false
        end
    }
}


local function setup( bundle, ... )
    local routines = {}
    local data
    if type(bundle) == "table" then
        routines = bundle
        data = bundle.data 
    else
        routines = create(1, bundle, ...)
    end
    return routines, data
end

local function runUntilLimit( _routines, _bundleData, _limit )
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
                        if living <= _limit or (_bundleData and not _bundleData.run) then
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
                if living <= _limit or (_bundleData and not _bundleData.run) then
                    return n
                end
            end
        end
        eventData = table.pack( os.pullEventRaw() )
    end
end

function createBundle( ... )
    local bundle = {}
    bundle.data = {
        run = true
    }
    bundle = setmetatable(bundle, external)
    return bundle, bundle:add(...)
end

function waitForAny( ... )
    local routines, data = setup( ... )
    return runUntilLimit( routines, data, #routines - 1 )
end

function waitForAll( ... )
    local routines, data = setup( ... )
    runUntilLimit( routines, data, 0 )
end