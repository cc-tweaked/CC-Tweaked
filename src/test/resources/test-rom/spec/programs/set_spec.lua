local capture = require "test_helpers".capture_program

describe("The set program", function()
    local function setup()
        local set = setmetatable({}, { __index = _G })
        loadfile("/rom/apis/settings.lua", set)()
        stub(_G, "settings", set)

        settings.set("test", "Hello World!")
        settings.define("another", { default = 456 })
    end

    it("displays all settings", function()
        setup()

        expect(capture(stub, "set"))
            :matches { ok = true, output = '"another" is 456\n"test" is "Hello World!"\n', error = "" }
    end)

    it("displays a single setting", function()
        setup()

        expect(capture(stub, "set test"))
            :matches { ok = true, output = 'test is "Hello World!"\n', error = "" }
    end)

    it("displays a single setting with description", function()
        setup()

        expect(capture(stub, "set another"))
            :matches { ok = true, output = 'another is 456\n', error = "" }
    end)

    it("displays a changed setting with description", function()
        setup()

        settings.set("another", 123)
        expect(capture(stub, "set another"))
            :matches { ok = true, output = 'another is 123 (default is 456)\n', error = "" }
    end)

    it("set a setting", function()
        expect(capture(stub, "set test Hello"))
            :matches { ok = true, output = '"test" set to "Hello"\n', error = "" }

        expect(settings.get("test")):eq("Hello")
    end)
end)
