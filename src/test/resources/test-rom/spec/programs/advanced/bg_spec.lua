local capture = require "test_helpers".capture_program

describe("The bg program", function()
    it("opens a tab in the background", function()
        local openTab = stub(shell, "openTab", function() return 12 end)
        local switchTab = stub(shell, "switchTab")
        capture(stub, "bg")
        expect(openTab):called_with("shell")
        expect(switchTab):called(0)
    end)
end)
