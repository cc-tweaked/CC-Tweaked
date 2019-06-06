local capture = require "test_helpers".capture_program

describe("The wget program", function()

    it("downloads one file", function()
        shell.run("wget https://example.com")

        expect(fs.exists("/example.com")):eq(true)
    end)

    it("downloads one file with given filename", function()
        shell.run("wget https://example.com testdownload")

        expect(fs.exists("/testdownload")):eq(true)
    end)

    it("run a program from the internet", function()
        expect(capture(stub, "wget run https://raw.githubusercontent.com/SquidDev-CC/CC-Tweaked/master/src/main/resources/assets/computercraft/lua/rom/programs/type.lua /rom"))
            :matches { ok = true, output = "Connecting to https://raw.githubusercontent.com/SquidDev-CC/CC-Tweaked/master/src/main/resources/assets/computercraft/lua/rom/programs/type.lua... Success.\ndirectory\n", error = "" }
    end)

    it("displays the usage of wget with no arguments", function()
        expect(capture(stub, "wget"))
            :matches { ok = true, output = "Usage:\nwget [run] <url> [filename]\n", error = "" }
    end)
end)
