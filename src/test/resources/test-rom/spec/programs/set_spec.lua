local capture = require "test_helpers".capture_program

describe("The set program", function()

    it("displays all settings", function()
        settings.clear()
        settings.set("Test", "Hello World!")
        settings.set("123", 456)
        
        expect(capture(stub, "set"))
            :matches { ok = true, output = '"123" is 456\n"Test" is "Hello World!"\n', error = "" }
    end)

    it("displays a single settings", function()
        settings.clear()
        settings.set("Test", "Hello World!")
        settings.set("123", 456)
        
        expect(capture(stub, "set Test"))
            :matches { ok = true, output = '"Test" is "Hello World!"\n', error = "" }
    end)

    it("set a setting", function()        
        expect(capture(stub, "set Test Hello"))
            :matches { ok = true, output = '"Test" set to "Hello"\n', error = "" }

        expect(settings.get("Test")):eq("Hello")
    end)
end)
