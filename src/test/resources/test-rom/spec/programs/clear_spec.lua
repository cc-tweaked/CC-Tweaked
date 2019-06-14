local capture = require "test_helpers".capture_program

describe("The clear program", function()
    
    it("run the program", function()
        local function checkCursor()
            local x,y = term.getCursorPos()
            if x == 1 and y == 1 then
                return true
            end 
        end

        term.setCursorPos(5,5)
        shell.run("clear")

        expect(checkCursor()):eq(true)
    end)
end)
