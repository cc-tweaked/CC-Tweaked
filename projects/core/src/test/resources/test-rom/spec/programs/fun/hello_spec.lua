local capture = require "test_helpers".capture_program

describe("The hello program", function()
    it("says hello", function()
        local slowPrint = stub(textutils, "slowPrint", function(...) return print(...) end)
        expect(capture(stub, "hello"))
            :matches { ok = true, output = "Hello World!\n", error = "" }
        expect(slowPrint):called(1)
    end)
end)
