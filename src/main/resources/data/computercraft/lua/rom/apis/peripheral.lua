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
-- @since 1.3
-- @changed 1.51 Add support for wired modems.

local expect = dofile("rom/modules/main/cc/expect.lua").expect

local native = peripheral
local sides = rs.getSides()

--- Provides a list of all peripherals available.
--
-- If a device is located directly next to the system, then its name will be
-- listed as the side it is attached to. If a device is attached via a Wired
-- Modem, then it'll be reported according to its name on the wired network.
--
-- @treturn { string... } A list of the names of all attached peripherals.
-- @since 1.51
function getNames()
    local results = {}
    for n = 1, #sides do
        local side = sides[n]
        if native.isPresent(side) then
            table.insert(results, side)
            if native.hasType(side, "modem") and not native.call(side, "isWireless") then
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
        local side = sides[n]
        if native.hasType(side, "modem") and not native.call(side, "isWireless") and
            native.call(side, "isPresentRemote", name)
        then
            return true
        end
    end
    return false
end

--- Get the type of a wrapped peripheral, or a peripheral with the given name.
--
-- @tparam string|table peripheral The name of the peripheral to find, or a
-- wrapped peripheral instance.
-- @treturn string|nil The peripheral's type, or `nil` if it is not present.
-- @changed 1.88.0 Accepts a wrapped peripheral as an argument.
-- @changed 1.99 Peripherals can have multiple types - this function returns multiple values.
function getType(peripheral)
    expect(1, peripheral, "string", "table")
    if type(peripheral) == "string" then -- Peripheral name passed
        if native.isPresent(peripheral) then
            return native.getType(peripheral)
        end
        for n = 1, #sides do
            local side = sides[n]
            if native.hasType(side, "modem") and not native.call(side, "isWireless") and
                native.call(side, "isPresentRemote", peripheral)
            then
                return native.call(side, "getTypeRemote", peripheral)
            end
        end
        return nil
    else
        local mt = getmetatable(peripheral)
        if not mt or mt.__name ~= "peripheral" or type(mt.types) ~= "table" then
            error("bad argument #1 (table is not a peripheral)", 2)
        end
        return table.unpack(mt.types)
    end
end

--- Check if a peripheral is of a particular type.
--
-- @tparam string|table peripheral The name of the peripheral to find, or a
-- wrapped peripheral instance.
-- @tparam string peripheral_type The type to check.
--
-- @treturn boolean|nil If a peripheral has a particular type, or `nil` if it is not present.
-- @since 1.99
function hasType(peripheral, peripheral_type)
    expect(1, peripheral, "string", "table")
    expect(2, peripheral_type, "string")
    if type(peripheral) == "string" then -- Peripheral name passed
        if native.isPresent(peripheral) then
            return native.hasType(peripheral, peripheral_type)
        end
        for n = 1, #sides do
            local side = sides[n]
            if native.hasType(side, "modem") and not native.call(side, "isWireless") and
                native.call(side, "isPresentRemote", peripheral)
            then
                return native.call(side, "hasTypeRemote", peripheral, peripheral_type)
            end
        end
        return nil
    else
        local mt = getmetatable(peripheral)
        if not mt or mt.__name ~= "peripheral" or type(mt.types) ~= "table" then
            error("bad argument #1 (table is not a peripheral)", 2)
        end
        return mt.types[type] ~= nil
    end
end

--- Get all available methods for the peripheral with the given name.
--
-- @tparam string name The name of the peripheral to find.
-- @treturn { string... }|nil A list of methods provided by this peripheral, or `nil` if
-- it is not present.
function getMethods(name)
    expect(1, name, "string")
    if native.isPresent(name) then
        return native.getMethods(name)
    end
    for n = 1, #sides do
        local side = sides[n]
        if native.hasType(side, "modem") and not native.call(side, "isWireless") and
            native.call(side, "isPresentRemote", name)
        then
            return native.call(side, "getMethodsRemote", name)
        end
    end
    return nil
end

--- Get the name of a peripheral wrapped with @{peripheral.wrap}.
--
-- @tparam table peripheral The peripheral to get the name of.
-- @treturn string The name of the given peripheral.
-- @since 1.88.0
function getName(peripheral)
    expect(1, peripheral, "table")
    local mt = getmetatable(peripheral)
    if not mt or mt.__name ~= "peripheral" or type(mt.name) ~= "string" then
        error("bad argument #1 (table is not a peripheral)", 2)
    end
    return mt.name
end

--- Call a method on the peripheral with the given name.
--
-- @tparam string name The name of the peripheral to invoke the method on.
-- @tparam string method The name of the method
-- @param ... Additional arguments to pass to the method
-- @return The return values of the peripheral method.
--
-- @usage Open the modem on the top of this computer.
--
--     peripheral.call("top", "open", 1)
function call(name, method, ...)
    expect(1, name, "string")
    expect(2, method, "string")
    if native.isPresent(name) then
        return native.call(name, method, ...)
    end

    for n = 1, #sides do
        local side = sides[n]
        if native.hasType(side, "modem") and not native.call(side, "isWireless") and
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
-- @usage Open the modem on the top of this computer.
--
--     peripheral.wrap("top").open(1)
function wrap(name)
    expect(1, name, "string")

    local methods = peripheral.getMethods(name)
    if not methods then
        return nil
    end

    local types = { peripheral.getType(name) }
    local result = setmetatable({}, {
        __name = "peripheral",
        name = name,
        type = types[1],
        types = types,
    })
    for _, method in ipairs(methods) do
        result[method] = function(...)
            return peripheral.call(name, method, ...)
        end
    end
    return result
end

--[[- Find all peripherals of a specific type, and return the
@{peripheral.wrap|wrapped} peripherals.

@tparam string ty The type of peripheral to look for.
@tparam[opt] function(name:string, wrapped:table):boolean filter A
filter function, which takes the peripheral's name and wrapped table
and returns if it should be included in the result.
@treturn table... 0 or more wrapped peripherals matching the given filters.
@usage Find all monitors and store them in a table, writing "Hello" on each one.

    local monitors = { peripheral.find("monitor") }
    for _, monitor in pairs(monitors) do
      monitor.write("Hello")
    end

@usage Find all wireless modems connected to this computer.

    local modems = { peripheral.find("modem", function(name, modem)
        return modem.isWireless() -- Check this modem is wireless.
    end) }

@usage This abuses the `filter` argument to call @{rednet.open} on every modem.

    peripheral.find("modem", rednet.open)
@since 1.6
]]
function find(ty, filter)
    expect(1, ty, "string")
    expect(2, filter, "function", "nil")

    local results = {}
    for _, name in ipairs(peripheral.getNames()) do
        if peripheral.hasType(name, ty) then
            local wrapped = peripheral.wrap(name)
            if filter == nil or filter(name, wrapped) then
                table.insert(results, wrapped)
            end
        end
    end
    return table.unpack(results)
end
