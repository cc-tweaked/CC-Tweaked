local capture = require "test_helpers".capture_program

describe("The programs program", function()
    it("list programs", function()
        local programs = stub(shell, "programs", function() return { "some", "programs" } end)
        local pagedTabulate = stub(textutils, "pagedTabulate", function(x) print(table.unpack(x)) end)

        expect(capture(stub, "/rom/programs/programs.lua"))
            :matches { ok = true, output = "some programs\n", error = "" }

        expect(programs):called_with(false)
        expect(pagedTabulate):called_with_matching({ "some", "programs" })
    end)
end)
