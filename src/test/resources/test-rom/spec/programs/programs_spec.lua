local capture = require "test_helpers".capture_program

describe("The programs program", function()
    local function touch(file)
        io.open(file, "w"):close()
    end

    it("list programs", function()
        touch("/test-files/programs/test.lua")
        
        shell.setPath("/test-files/programs")

        expect(capture(stub, "/rom/programs/programs.lua"))
            :matches { ok = true, output = "test\n", error = "" }
    end)
end)
