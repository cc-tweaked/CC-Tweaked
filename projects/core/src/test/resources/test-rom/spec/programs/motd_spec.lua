local capture = require "test_helpers".capture_program

describe("The motd program", function()
    local function setup_date(day, month)
        stub(os, "date", function() return { day = day, month = month } end)
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

    it("displays date-specific MOTD (1 Jan)", function()
        setup_date(1, 1)
        expect(capture(stub, "motd"))
            :matches { ok = true, output = "Happy new year!\n", error = "" }
    end)

    it("displays date-specific MOTD (28 Apr)", function()
        setup_date(28, 4)
        expect(capture(stub, "motd"))
            :matches { ok = true, output = "Ed Balls\n", error = "" }
    end)

    it("displays date-specific MOTD (31 Oct)", function()
        setup_date(31, 10)
        expect(capture(stub, "motd"))
            :matches { ok = true, output = "OOoooOOOoooo! Spooky!\n", error = "" }
    end)

    it("displays date-specific MOTD (24 Dec)", function()
        setup_date(24, 12)
        expect(capture(stub, "motd"))
            :matches { ok = true, output = "Merry X-mas!\n", error = "" }
    end)
end)
