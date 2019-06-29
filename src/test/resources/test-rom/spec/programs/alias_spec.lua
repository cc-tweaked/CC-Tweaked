local capture = require "test_helpers".capture_program

describe("The alias program", function()
    it("displays its usage when given too many arguments", function()
        expect(capture(stub, "alias a b c"))
            :matches { ok = true, output = "Usage: alias <alias> <program>\n", error = "" }
    end)

    it("lists aliases", function()
        local pagedTabulate = stub(textutils, "pagedTabulate", function(x) print(table.unpack(x)) end)
        stub(shell, "aliases", function() return { cp = "copy" } end)
        expect(capture(stub, "alias"))
            :matches { ok = true, output = "cp:copy\n", error = "" }
        expect(pagedTabulate):called_with_matching({ "cp:copy" })
    end)

    it("sets an alias", function()
        local setAlias = stub(shell, "setAlias")
        capture(stub, "alias test Hello")
        expect(setAlias):called_with("test", "Hello")
    end)

    it("clears an alias", function()
        local clearAlias = stub(shell, "clearAlias")
        capture(stub, "alias test")
        expect(clearAlias):called_with("test")
    end)
end)
