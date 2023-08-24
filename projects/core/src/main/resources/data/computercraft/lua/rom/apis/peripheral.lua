-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- Find and control peripherals attached to this computer.

Peripherals are blocks (or turtle and pocket computer upgrades) which can
be controlled by a computer. For instance, the [`speaker`] peripheral allows a
computer to play music and the [`monitor`] peripheral allows you to display text
in the world.

## Referencing peripherals

Computers can interact with adjacent peripherals. Each peripheral is given a
name based on which direction it is in. For instance, a disk drive below your
computer will be called `"bottom"` in your Lua code, one to the left called
`"left"` , and so on for all 6 directions (`"bottom"`, `"top"`, `"left"`,
`"right"`, `"front"`, `"back"`).

You can list the names of all peripherals with the `peripherals` program, or the
[`peripheral.getNames`] function.

It's also possible to use peripherals which are further away from your computer
through the use of [Wired Modems][`modem`]. Place one modem against your computer
(you may need to sneak and right click), run Networking Cable to your
peripheral, and then place another modem against that block. You can then right
click the modem to use (or *attach*) the peripheral. This will print a
peripheral name to chat, which can then be used just like a direction name to
access the peripheral. You can click on the message to copy the name to your
clipboard.

## Using peripherals

Once you have the name of a peripheral, you can call functions on it using the
[`peripheral.call`] function. This takes the name of our peripheral, the name of
the function we want to call, and then its arguments.

> [!INFO]
> Some bits of the peripheral API call peripheral functions *methods* instead
> (for example, the [`peripheral.getMethods`] function). Don't worry, they're the
> same thing!

Let's say we have a monitor above our computer (and so "top") and want to
[write some text to it][`monitor.write`]. We'd write the following:

```lua
peripheral.call("top", "write", "This is displayed on a monitor!")
```

Once you start calling making a couple of peripheral calls this can get very
repetitive, and so we can [wrap][`peripheral.wrap`] a peripheral. This builds a
table of all the peripheral's functions so you can use it like an API or module.

For instance, we could have written the above example as follows:

```lua
local my_monitor = peripheral.wrap("top")
my_monitor.write("This is displayed on a monitor!")
```

## Finding peripherals

Sometimes when you're writing a program you don't care what a peripheral is
called, you just need to know it's there. For instance, if you're writing a
music player, you just need a speaker - it doesn't matter if it's above or below
the computer.

Thankfully there's a quick way to do this: [`peripheral.find`]. This takes a
*peripheral type* and returns all the attached peripherals which are of this
type.

What is a peripheral type though? This is a string which describes what a
peripheral is, and so what functions are available on it. For instance, speakers
are just called `"speaker"`, and monitors `"monitor"`. Some peripherals might
have more than one type - a Minecraft chest is both a `"minecraft:chest"` and
`"inventory"`.

You can get all the types a peripheral has with [`peripheral.getType`], and check
a peripheral is a specific type with [`peripheral.hasType`].

To return to our original example, let's use [`peripheral.find`] to find an
attached speaker:

```lua
local speaker = peripheral.find("speaker")
speaker.playNote("harp")
```

@module peripheral
@see event!peripheral This event is fired whenever a new peripheral is attached.
@see event!peripheral_detach This event is fired whenever a peripheral is detached.
@since 1.3
@changed 1.51 Add support for wired modems.
@changed 1.99 Peripherals can have multiple types.
]]

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
            if native.hasType(side, "peripheral_hub") then
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
        if native.hasType(side, "peripheral_hub") and native.call(side, "isPresentRemote", name) then
            return true
        end
    end
    return false
end

--[[- Get the types of a named or wrapped peripheral.

@tparam string|table peripheral The name of the peripheral to find, or a
wrapped peripheral instance.
@treturn string... The peripheral's types, or `nil` if it is not present.
@changed 1.88.0 Accepts a wrapped peripheral as an argument.
@changed 1.99 Now returns multiple types.
@usage Get the type of a peripheral above this computer.

    peripheral.getType("top")
]]
function getType(peripheral)
    expect(1, peripheral, "string", "table")
    if type(peripheral) == "string" then -- Peripheral name passed
        if native.isPresent(peripheral) then
            return native.getType(peripheral)
        end
        for n = 1, #sides do
            local side = sides[n]
            if native.hasType(side, "peripheral_hub") and native.call(side, "isPresentRemote", peripheral) then
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

--[[- Check if a peripheral is of a particular type.

@tparam string|table peripheral The name of the peripheral or a wrapped peripheral instance.
@tparam string peripheral_type The type to check.

@treturn boolean|nil If a peripheral has a particular type, or `nil` if it is not present.
@since 1.99
]]
function hasType(peripheral, peripheral_type)
    expect(1, peripheral, "string", "table")
    expect(2, peripheral_type, "string")
    if type(peripheral) == "string" then -- Peripheral name passed
        if native.isPresent(peripheral) then
            return native.hasType(peripheral, peripheral_type)
        end
        for n = 1, #sides do
            local side = sides[n]
            if native.hasType(side, "peripheral_hub") and native.call(side, "isPresentRemote", peripheral) then
                return native.call(side, "hasTypeRemote", peripheral, peripheral_type)
            end
        end
        return nil
    else
        local mt = getmetatable(peripheral)
        if not mt or mt.__name ~= "peripheral" or type(mt.types) ~= "table" then
            error("bad argument #1 (table is not a peripheral)", 2)
        end
        return mt.types[peripheral_type] ~= nil
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
        if native.hasType(side, "peripheral_hub") and native.call(side, "isPresentRemote", name) then
            return native.call(side, "getMethodsRemote", name)
        end
    end
    return nil
end

--- Get the name of a peripheral wrapped with [`peripheral.wrap`].
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
        if native.hasType(side, "peripheral_hub") and native.call(side, "isPresentRemote", name) then
            return native.call(side, "callRemote", name, method, ...)
        end
    end
    return nil
end

--- Get a table containing all functions available on a peripheral. These can
-- then be called instead of using [`peripheral.call`] every time.
--
-- @tparam string name The name of the peripheral to wrap.
-- @treturn table|nil The table containing the peripheral's methods, or `nil` if
-- there is no peripheral present with the given name.
-- @usage Open the modem on the top of this computer.
--
--     local modem = peripheral.wrap("top")
--     modem.open(1)
function wrap(name)
    expect(1, name, "string")

    local methods = peripheral.getMethods(name)
    if not methods then
        return nil
    end

    -- We store our types array as a list (for getType) and a lookup table (for hasType).
    local types = { peripheral.getType(name) }
    for i = 1, #types do types[types[i]] = true end
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
[wrapped][`peripheral.wrap`] peripherals.

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

@usage This abuses the `filter` argument to call [`rednet.open`] on every modem.

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
