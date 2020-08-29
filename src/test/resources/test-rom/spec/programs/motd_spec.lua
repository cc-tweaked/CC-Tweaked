local capture = require "test_helpers".capture_program

describe("The motd program", function()
        
    local function setup_date(month, day)
        stub(_G, "os", { date = function() return { month = month, day = day } end })
    end

    it("displays MOTD", function()
        setup_date(0, 0)
        local file = fs.open("/motd_check.txt", "w")
        file.write("Hello World!")
        file.close()
        settings.set("motd.path", "/motd_check.txt")

        expect(capture(stub, "motd"))
            :matches { ok = true, output = "Hello World!\n", error = "" }
    end)
        
    it("displays date-specific MOTD (1/1)", function()
        setup_date(1, 1)
        expect(capture(stub, "motd"))
            :matches { ok = true, output = "Happy new year!\n", error = "" }
    end)
        
    it("displays date-specific MOTD (10/31)", function()
        setup_date(10, 31)
        expect(capture(stub, "motd"))
            :matches { ok = true, output = "OOoooOOOoooo! Spooky!\n", error = "" }
    end)
        
    it("displays date-specific MOTD (12/24)", function()
        setup_date(12, 24)
        expect(capture(stub, "motd"))
            :matches { ok = true, output = "Merry X-mas!\n", error = "" }
    end)
end)
