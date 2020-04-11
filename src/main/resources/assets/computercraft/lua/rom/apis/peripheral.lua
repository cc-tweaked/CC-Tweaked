--- The Peripheral API is for interacting with peripherals connected to the
-- computer, such as the Disk Drive, the Advanced Monitor and Monitor.
--
-- Each peripheral block has a name, either referring to the side the peripheral
-- can be found on, or a name on an adjacent wired network.
--
-- If the peripheral is next to the computer, its side is either `front`,
-- `back`, `left`, `right`, `top` or `bottom`. If the peripheral is attached by
-- a cable, its side will follow the format `type_id`, for example `printer_0`.
--
-- Peripheral functions are called *methods*, a term borrowed from Java.
--
-- @module peripheral

local expect = dofile("rom/modules/main/cc/expect.lua").expect

local native = peripheral
local sides = rs.getSides()

--- Provides a list of all peripherals available.
--
-- If a device is located directly next to the system, then its name will be
-- listed as the side it is attached to. If a device is attached via a Wired
-- Modem, then it'll be reported according to its name on the wired network.
--
-- @treturn table A list of the names of all attached peripherals.
function getNames()
    local results = {}
    for n = 1, #sides do
        local side = sides[n]
        if native.isPresent(side) then
            table.insert(results, side)
            if native.getType(side) == "modem" and not native.call(side, "isWireless") then
                local remote = native.call(side, "getNamesRemote")
                for _, name in ipairs(remote) do
                    table.insert(results, name)
                end
            end
        end
    end
    return results
end

--- Determines if a peripheral is present with the given name.
--
-- @tparam string name The side or network name that you want to check.
-- @treturn boolean If a peripheral is present with the given name.
-- @usage peripheral.isPresent("top")
-- @usage peripheral.isPresent("monitor_0")
function isPresent(name)
    expect(1, name, "string")
    if native.isPresent(name) then
        return true
    end

    for n = 1, #sides do
        local name = sides[n]
        if native.getType(name) == "modem" and not native.call(name, "isWireless") and
            native.call(name, "isPresentRemote", name)
        then
            return true
        end
    end
    return false
end

--- Get the type of the peripheral with the given name.
--
-- @tparam string name The name of the peripheral to find.
-- @treturn string|nil The peripheral's type, or `nil` if it is not present.
function getType(name)
    expect(1, name, "string")
    if native.isPresent(name) then
        return native.getType(name)
    end
    for n = 1, #sides do
        local side = sides[n]
        if native.getType(side) == "modem" and not native.call(side, "isWireless") and
            native.call(side, "isPresentRemote", name)
        then
            return native.call(side, "getTypeRemote", name)
        end
    end
    return nil
end

--- Get all available methods for the peripheral with the given name.
--
-- @tparam string name The name of the peripheral to find.
-- @treturn table|nil A list of methods provided by this peripheral, or `nil` if
-- it is not present.
function getMethods(name)
    expect(1, name, "string")
    if native.isPresent(name) then
        return native.getMethods(name)
    end
    for n = 1, #sides do
        local side = sides[n]
        if native.getType(side) == "modem" and not native.call(side, "isWireless") and
            native.call(side, "isPresentRemote", name)
        then
            return native.call(side, "getMethodsRemote", name)
        end
    end
    return nil
end

--- Call a method on a peripheral with a given name
--
-- @tparam string name The name of the peripheral to invoke the method on.
-- @tparam string method The name of the method
-- @param ... Additional arguments to pass to the method
-- @return The return values of the peripheral method.
--
-- @usage peripheral.call("top", "open", 1)
function call(name, method, ...)
    expect(1, name, "string")
    expect(2, method, "string")
    if native.isPresent(name) then
        return native.call(name, method, ...)
    end

    for n = 1, #sides do
        local side = sides[n]
        if native.getType(side) == "modem" and not native.call(side, "isWireless") and
            native.call(side, "isPresentRemote", name)
        then
            return native.call(side, "callRemote", name, method, ...)
        end
    end
    return nil
end

--- Get a table containing functions pointing to the peripheral's methods, which
-- can then be called as if using @{peripheral.call}.
--
-- @tparam string name The name of the peripheral to wrap.
-- @treturn table|nil The table containing the peripheral's methods, or `nil` if
-- there is no peripheral present with the given name.
-- @usage peripheral.wrap("top").open(1)
function wrap(name)
    expect(1, name, "string")

    local methods = peripheral.getMethods(name)
    if not methods then
        return nil
    end

    local result = {}
    for _, method in ipairs(methods) do
        result[method] = function(...)
            return peripheral.call(name, method, ...)
        end
    end
    return result
end

--- Find all peripherals of a specific type, and return the
-- @{peripheral.wrap|wrapped} peripherals.
--
-- @tparam string ty The type of peripheral to look for.
-- @tparam[opt] function(name:string, wrapped:table):boolean filter A
-- filter function, which takes the peripheral's name and wrapped table
-- and returns if it should be included in the result.
-- @treturn table... 0 or more wrapped peripherals matching the given filters.
-- @usage local monitors = { peripheral.find("monitor") }
-- @usage peripheral.find("modem", rednet.open)
function find(ty, filter)
    expect(1, ty, "string")
    expect(2, filter, "function", "nil")

    local results = {}
    for _, name in ipairs(peripheral.getNames()) do
        if peripheral.getType(name) == ty then
            local wrapped = peripheral.wrap(name)
            if filter == nil or filter(name, wrapped) then
                table.insert(results, wrapped)
            end
        end
    end
    return table.unpack(results)
end
