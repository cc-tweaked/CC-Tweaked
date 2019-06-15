local capture = require "test_helpers".capture_program

describe("The label program", function()

    it("displays its usage when given no argument", function()     
        expect(capture(stub, "label"))
            :matches { ok = true, output = "Usages:\nlabel get\nlabel get <drive>\nlabel set <text>\nlabel set <drive> <text>\nlabel clear\nlabel clear <drive>\n", error = "" }
    end)

    it("computer has no label", function() 
        os.setComputerLabel(nil)
    
        expect(capture(stub, "label get"))
            :matches { ok = true, output = "No Computer label\n", error = "" }
    end)

    it("computer has a label", function() 
        os.setComputerLabel("Test")
    
        expect(capture(stub, "label get"))
            :matches { ok = true, output = "Computer label is \"Test\"\n", error = "" }
    end)

    it("set computer label", function() 
        os.setComputerLabel(nil)
        
        shell.run("label set Test")

        expect(os.getComputerLabel()):eq("Test")
    end)

    it("clear computer label", function() 
        os.setComputerLabel("Test")
        
        shell.run("label clear")

        expect(os.getComputerLabel()):eq(nil)
    end)
end)
