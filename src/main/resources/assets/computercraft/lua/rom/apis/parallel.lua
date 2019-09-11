local function create( ... )
    local tFns = table.pack(...)
    local tCos = {}
    for i = 1, tFns.n, 1 do
        local fn = tFns[i]
        if type( fn ) ~= "function" then
            error( "bad argument #" .. i .. " (expected function, got " .. type( fn ) .. ")", 4 )
        end

        tCos[i] = coroutine.create(fn)
    end

    return tCos
end

local function checkDead(self, r, n)
    if r and coroutine.status( r ) == "dead" then
        for i = 1, #self._coroutines do
            if r == self._coroutines[i] then
                table.remove(self._coroutines, i)
                self._removed[#self._removed+1] = i
                if _any then
                    return i
                elseif #self._removed == self._total then
                    return true
                end
            end
        end
    end
end

local group = {}

function runUntilLimit( self, _any )
    local buffer
    local tFilters = {}
    local eventData = { n = 0 }

    while true do
        buffer = {}
        for i = 1, #self._coroutines do
            buffer[#buffer+1] = self._coroutines[i]
        end
        for n=1,#buffer do
            local r = buffer[n]
            if r then
                if tFilters[r] == nil or tFilters[r] == eventData[1] or eventData[1] == "terminate" then
                    local ok, param = coroutine.resume( r, table.unpack( eventData, 1, eventData.n ) )
                    if not ok then
                        error( param, 0 )
                    else
                        tFilters[r] = param
                    end
                end
                local c = checkDead(self, buffer[n], n)
                if c then return c end
            end
        end
        for n = 1, #buffer do
            local c = checkDead(self, buffer[n], n)
            if c then return c end
        end
        if not self._run then
            return
        end
        eventData = table.pack( os.pullEventRaw() )
    end
end

function group:add(...)
    local set = create( ... )
    local ids = {}
    for i = 1, #set do
        self._coroutines[#self._coroutines+1] = set[i]
        self._total = self._total+1
        ids[i] = self._total
    end
    return table.unpack(ids)
end

function group:remove(id)
    local out
    for i = 1, #self._removed do
        if self._removed[i] <= id then
            id = id-1
        end
    end
    if self._coroutines[id] then
        table.remove(self._coroutines, id)
        self._removed[#self._removed+1] = id
        return true
    end
    return false
end

function group:stop()
    self._run = false
end

function group:waitForAll()
    runUntilLimit( self, false )
end

function group:waitForAny()
    return runUntilLimit( self, true )
end

function createGroup( ... )
    local inst = setmetatable(
        { 
            _run = true, 
            _coroutines = {},
            _removed = {},
            _total = 0
        }, 
        { 
            __index = group 
        }
    )
    inst._data = function()
        return inst._run, inst._total
    end
    inst:add(...)
    return inst
end

function waitForAny( ... )
    local routines = createGroup( ... )
    return routines:runUntilLimit( true )
end

function waitForAll( ... )
    local routines = createGroup( ... )
    routines:runUntilLimit( false )
end
