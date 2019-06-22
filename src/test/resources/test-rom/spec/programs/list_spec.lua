local capture = require "test_helpers".capture_program

describe("The list program", function()

    it("list files", function()
        expect(capture(stub, "list /rom"))
            :matches { ok = true, output = "8192\napis\nautorun\nhelp\nmodules\nprograms\n1\nmotd.txt\nstartup.lua\n", error = "" }
    end)

    it("try to list a not existing directory", function()
        expect(capture(stub, "list /rom/nothing"))
            :matches { ok = true, output = "", error = "Not a directory\n" }
    end)
end)
