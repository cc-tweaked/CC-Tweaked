describe("The peripheral library", function()
    local it_modem = peripheral.getType("top") == "modem" and it or pending

    local multitype_peripheral = setmetatable({}, {
        __name = "peripheral",
        name = "top",
        type = "modem",
        types = { "modem", "inventory", modem = true, inventory = true },
    })

    describe("peripheral.isPresent", function()
        it("validates arguments", function()
            peripheral.isPresent("")
            expect.error(peripheral.isPresent, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("peripheral.getName", function()
        it("validates arguments", function()
            expect.error(peripheral.getName, nil):eq("bad argument #1 (expected table, got nil)")
            expect.error(peripheral.getName, {}):eq("bad argument #1 (table is not a peripheral)")
        end)

        it_modem("can get the name of a wrapped peripheral", function()
            expect(peripheral.getName(peripheral.wrap("top"))):eq("top")
        end)
    end)

    describe("peripheral.getType", function()
        it("validates arguments", function()
            peripheral.getType("")
            expect.error(peripheral.getType, nil):eq("bad argument #1 (expected string or table, got nil)")
            expect.error(peripheral.getType, {}):eq("bad argument #1 (table is not a peripheral)")
        end)

        it("returns nil when no peripheral is present", function()
            expect(peripheral.getType("bottom")):eq(nil)
        end)

        it_modem("can get the type of a peripheral by side", function()
            expect(peripheral.getType("top")):eq("modem")
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
            expect.error(peripheral.hasType, nil):eq("bad argument #1 (expected string or table, got nil)")
            expect.error(peripheral.hasType, {}, ""):eq("bad argument #1 (table is not a peripheral)")
            expect.error(peripheral.hasType, ""):eq("bad argument #2 (expected string, got nil)")
        end)

        it("returns nil when no peripherals are present", function()
            expect(peripheral.hasType("bottom", "modem")):eq(nil)
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
    end)

    describe("peripheral.getMethods", function()
        it("validates arguments", function()
            peripheral.getMethods("")
            expect.error(peripheral.getMethods, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("peripheral.call", function()
        it("validates arguments", function()
            peripheral.call("", "")
            expect.error(peripheral.call, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(peripheral.call, "", nil):eq("bad argument #2 (expected string, got nil)")
        end)

        it_modem("has the correct error location", function()
            expect.error(function() peripheral.call("top", "isOpen", false) end)
                :str_match("^[^:]+:%d+: bad argument #1 %(number expected, got boolean%)$")
        end)
    end)

    describe("peripheral.wrap", function()
        it("validates arguments", function()
            peripheral.wrap("")
            expect.error(peripheral.wrap, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("peripheral.find", function()
        it("validates arguments", function()
            peripheral.find("")
            peripheral.find("", function()
            end)
            expect.error(peripheral.find, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(peripheral.find, "", false):eq("bad argument #2 (expected function, got boolean)")
        end)
    end)
end)
