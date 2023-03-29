-- SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

describe("The peripheral library", function()
    local it_modem = peripheral.getType("top") == "modem" and it or pending
    local it_remote = peripheral.getType("bottom") == "peripheral_hub" and it or pending

    local multitype_peripheral = setmetatable({}, {
        __name = "peripheral",
        name = "top",
        type = "modem",
        types = { "modem", "inventory", modem = true, inventory = true },
    })

    describe("peripheral.isPresent", function()
        it("validates arguments", function()
            peripheral.isPresent("")
            expect.error(peripheral.isPresent, nil):eq("bad argument #1 (string expected, got nil)")
        end)

        it_modem("asserts the presence of local peripherals", function()
            expect(peripheral.isPresent("top")):eq(true)
            expect(peripheral.isPresent("left")):eq(false)
        end)

        it_remote("asserts the presence of remote peripherals", function()
            expect(peripheral.isPresent("remote_1")):eq(true)
            expect(peripheral.isPresent("remote_2")):eq(false)
        end)
    end)

    describe("peripheral.getName", function()
        it("validates arguments", function()
            expect.error(peripheral.getName, nil):eq("bad argument #1 (table expected, got nil)")
            expect.error(peripheral.getName, {}):eq("bad argument #1 (table is not a peripheral)")
        end)

        it_modem("can get the name of a wrapped peripheral", function()
            expect(peripheral.getName(peripheral.wrap("top"))):eq("top")
        end)

        it("can get the name of a fake peripheral", function()
            expect(peripheral.getName(multitype_peripheral)):eq("top")
        end)
    end)

    describe("peripheral.getType", function()
        it("validates arguments", function()
            peripheral.getType("")
            expect.error(peripheral.getType, nil):eq("bad argument #1 (string or table expected, got nil)")
            expect.error(peripheral.getType, {}):eq("bad argument #1 (table is not a peripheral)")
        end)

        it("returns nil when no peripheral is present", function()
            expect(peripheral.getType("left")):eq(nil)
            expect(peripheral.getType("remote_2")):eq(nil)
        end)

        it_modem("can get the type of a local peripheral", function()
            expect(peripheral.getType("top")):eq("modem")
        end)

        it_remote("can get the type of a remote peripheral", function()
            expect(peripheral.getType("remote_1")):eq("remote")
        end)

        it_modem("can get the type of a wrapped peripheral", function()
            expect(peripheral.getType(peripheral.wrap("top"))):eq("modem")
        end)

        it("can return multiple types", function()
            expect({ peripheral.getType(multitype_peripheral) }):same { "modem", "inventory" }
        end)
    end)

    describe("peripheral.hasType", function()
        it("validates arguments", function()
            peripheral.getType("")
            expect.error(peripheral.hasType, nil):eq("bad argument #1 (string or table expected, got nil)")
            expect.error(peripheral.hasType, {}, ""):eq("bad argument #1 (table is not a peripheral)")
            expect.error(peripheral.hasType, ""):eq("bad argument #2 (string expected, got nil)")
        end)

        it("returns nil when no peripherals are present", function()
            expect(peripheral.hasType("left", "modem")):eq(nil)
            expect(peripheral.hasType("remote_2", "remote")):eq(nil)
        end)

        it_modem("can check type of a peripheral by side", function()
            expect(peripheral.hasType("top", "modem")):eq(true)
            expect(peripheral.hasType("top", "not_a_modem")):eq(false)
        end)

        it_modem("can check the type of a wrapped peripheral (true)", function()
            expect(peripheral.hasType(peripheral.wrap("top"), "modem")):eq(true)
        end)

        it("can check the type of a wrapped peripheral (fake)", function()
            expect(peripheral.hasType(multitype_peripheral, "modem")):eq(true)
            expect(peripheral.hasType(multitype_peripheral, "inventory")):eq(true)
            expect(peripheral.hasType(multitype_peripheral, "something else")):eq(false)
        end)

        it_remote("can check type of a remote peripheral", function()
            expect(peripheral.hasType("remote_1", "remote")):eq(true)
        end)
    end)

    describe("peripheral.getMethods", function()
        it("validates arguments", function()
            peripheral.getMethods("")
            expect.error(peripheral.getMethods, nil):eq("bad argument #1 (string expected, got nil)")
        end)
    end)

    describe("peripheral.call", function()
        it("validates arguments", function()
            peripheral.call("", "")
            expect.error(peripheral.call, nil):eq("bad argument #1 (string expected, got nil)")
            expect.error(peripheral.call, "", nil):eq("bad argument #2 (string expected, got nil)")
        end)

        it_modem("has the correct error location", function()
            expect.error(function() peripheral.call("top", "isOpen", false) end)
                :str_match("^[^:]+:%d+: bad argument #1 %(number expected, got boolean%)$")
        end)
    end)

    describe("peripheral.wrap", function()
        it("validates arguments", function()
            peripheral.wrap("")
            expect.error(peripheral.wrap, nil):eq("bad argument #1 (string expected, got nil)")
        end)

        it_modem("wraps a local peripheral", function()
            local p = peripheral.wrap("top")
            expect(type(p)):eq("table")
            expect(type(next(p))):eq("string")
        end)

        it_remote("wraps a remote peripheral", function()
            local p = peripheral.wrap("remote_1")
            expect(type(p)):eq("table")
            expect(next(p)):eq("func")
        end)
    end)

    describe("peripheral.find", function()
        it("validates arguments", function()
            peripheral.find("")
            peripheral.find("", function()
            end)
            expect.error(peripheral.find, nil):eq("bad argument #1 (string expected, got nil)")
            expect.error(peripheral.find, "", false):eq("bad argument #2 (function expected, got boolean)")
        end)

        it_modem("finds a local peripheral", function()
            local p = peripheral.find("modem")
            expect(type(p)):eq("table")
            expect(peripheral.getName(p)):eq("top")
        end)

        it_modem("finds a local peripheral", function()
            local p = peripheral.find("remote")
            expect(type(p)):eq("table")
            expect(peripheral.getName(p)):eq("remote_1")
        end)
    end)
end)
