local capture = require "test_helpers".capture_program

describe("The fg program", function()
    it("opens the shell in the foreground", function()
        local openTab = stub(shell, "openTab", function() return 12 end)
        local switchTab = stub(shell, "switchTab")
        capture(stub, "fg")
        expect(openTab):called_with("shell")
        expect(switchTab):called_with(12)
    end)
end)
