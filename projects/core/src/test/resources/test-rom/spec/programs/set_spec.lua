local capture = require "test_helpers".capture_program

describe("The set program", function()
    local function setup()
        local set = setmetatable({}, { __index = _G })
        loadfile("/rom/apis/settings.lua", set)()
        stub(_G, "settings", set)

        settings.set("test", "Hello World!")
        settings.define("test.defined", { default = 456, description = "A description", type = "number" })
    end

    it("displays all settings", function()
        setup()

        expect(capture(stub, "set"))
            :matches { ok = true, output = '"test" is "Hello World!"\n"test.defined" is 456\n', error = "" }
    end)

    it("displays a single setting", function()
        setup()

        expect(capture(stub, "set test"))
            :matches { ok = true, output = 'test is "Hello World!"\n', error = "" }
    end)

    it("displays a single setting with description", function()
        setup()

        expect(capture(stub, "set test"))
            :matches { ok = true, output = 'test is "Hello World!"\n', error = "" }
    end)

    it("displays a changed setting with description", function()
        setup()

        settings.set("test.defined", 123)
        expect(capture(stub, "set test.defined"))
            :matches { ok = true, output = 'test.defined is 123 (default is 456)\nA description\n', error = "" }
    end)

    it("set a setting", function()
        setup()

        expect(capture(stub, "set test Hello"))
            :matches { ok = true, output = '"test" set to "Hello"\n', error = "" }

        expect(settings.get("test")):eq("Hello")
    end)

    it("checks the type of a setting", function()
        setup()

        expect(capture(stub, "set test.defined Hello"))
            :matches { ok = true, output = "", error = '"Hello" is not a valid number.\n' }
        expect(capture(stub, "set test.defined 456"))
            :matches { ok = true, output = '"test.defined" set to 456\n', error = "" }
    end)
end)
