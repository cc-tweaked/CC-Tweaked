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

local group = {}
group.__index = group

function runUntilLimit( self, any )
    if self._count <= 0 then error("Cannot run group without coroutines", 3) end
    if self._run then error("Group is already running", 3) end
    self._run = true

    local tFilters = setmetatable({}, { __mode = "k" })
    local eventData = { n = 0 }

    while true do
        local coroutines, n = self._coroutines, self._count

        for i = 1, n do
            local r = coroutines[i]
            if r then
                if tFilters[r] == nil or tFilters[r] == eventData[1] or eventData[1] == "terminate" then
                    local ok, param = coroutine.resume( r, table.unpack( eventData, 1, eventData.n ) )
                    if not ok then
                        self:remove(r) self._run = false -- Dubious, but w/e.
                        error( param, 0 )
                    else
                        tFilters[r] = param
                    end
                end

                if coroutine.status(r) == "dead" then
                    self:remove(r)
                    if any then self._run = false return end
                end
            end
        end

        if not self._run or self._count <= 0 then self._run = false return end

        eventData = table.pack( os.pullEventRaw() )

        if not self._run then self._run = false return end
    end
end

function group:add(...)
    local set = create( ... )
    local objs = { }
    for i = 1, #set do
        self._coroutines[#self._coroutines+1] = set[i]
        self._count = self._count + 1
        objs[i] = set[i] -- TODO: Put this in a wrapper??
    end
    return table.unpack(objs)
end

function group:remove(co)
    local routines, index = self._coroutines, nil
    for i = 1, self._count do
        if routines[i] == co then
            index = i
            break
        end
    end

    if not index then return false end

    local new_routines = {}
    for i = 1, index - 1 do new_routines[i] = routines[i] end
    for i = index + 1, index do new_routines[i - 1] = routines[i] end

    self._coroutines = new_routines
    self._count = self._count - 1
    return true
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
    local inst = setmetatable({
        _run = false,
        _coroutines = {},
        _count = 0
    }, group)
    inst:add(...)
    return inst
end

function waitForAny( ... )
    return createGroup( ... ):waitForAny()
end

function waitForAll( ... )
    return createGroup( ... ):waitForAll()
end
