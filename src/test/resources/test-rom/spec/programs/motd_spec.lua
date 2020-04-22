local capture = require "test_helpers".capture_program

describe("The motd program", function()

    it("displays MODT", function()
        local file = fs.open("/modt_check.txt", "w")
        file.write("Hello World!")
        file.close()
        settings.set("motd.path", "/modt_check.txt")

        expect(capture(stub, "motd"))
            :matches { ok = true, output = "Hello World!\n", error = "" }
    end)
end)
