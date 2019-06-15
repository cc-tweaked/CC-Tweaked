local capture = require "test_helpers".capture_program

describe("The clear program", function()
    
    it("run the program", function()
        term.setCursorPos(5,5)
        shell.run("clear")

        expect({ term.getCursorPos() }):same { 1, 1 }
    end)
end)
