local capture = require "test_helpers".capture_program

describe("The alias program", function()

    it("displays its usage when given to many arguments", function()
        
        expect(capture(stub, "alias a b c"))
            :matches { ok = true, output = "Usage: alias <alias> <program>\n", error = "" }
    end)

    it("list alias", function()
        
        expect(capture(stub, "alias"))
            :matches { ok = true, output = "background:bg\nclr:clear\ncp:copy\ndir:list\nforeground:fg\nls:list\nmv:move\nrm:delete\nrs:redstone\nsh:shell\n", error = "" }
    end)

    it("set alias", function()
        shell.run("alias test Hello")

        local tAlias = shell.aliases()

        expect(tAlias.test):eq("Hello")
    end)

    it("clear alias", function()
        shell.setAlias("test","hello")
        shell.run("alias test")

        local tAlias = shell.aliases()

        expect(tAlias.test):eq(nil)
    end)
end)
